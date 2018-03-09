package org.epic.core.model;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.ListenerList;
import org.epic.core.parser.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PartitionTypes;
import org.epic.perleditor.editors.PerlPartitioner;

/**
 * A parsed Perl source file. This class provides access to 
 * {@link org.epic.core.model.ISourceElement}s recognized in a Perl
 * source file.
 * 
 * @author jploski
 */
public class SourceFile
{
    private final ListenerList<ISourceFileListener> listeners = new ListenerList<ISourceFileListener>();
    private final ILog log;
    private final IDocument doc;
    private List<PODComment> pods;
    private List<Package> packages;
    
    /**
     * Creates a SourceFile which will be reflecting contents of the given
     * source document. As a second step of initialisation, {@link #parse}
     * has to be called.
     */
    public SourceFile(ILog log, IDocument doc)
    {
        assert log != null;
        assert doc != null;
        this.log = log;
        this.doc = doc;
        this.pods = Collections.emptyList();
        this.packages = Collections.emptyList();
    }
    
    /**
     * Adds a listener for changes of this SourceFile.
     * Has no effect if an identical listener is already registered.
     */
    public synchronized void addListener(ISourceFileListener listener)
    {
        listeners.add(listener);
    }

    /**
     * @return the source document based on which this SourceFile was created;
     *         note that depending on the time when this method is called,
     *         the document may be more up to date than the information
     *         provided by this SourceFile instance
     */
    public IDocument getDocument()
    {
        return doc;
    }
    
    /**
     * @return a list of {@link Package} instances representing package
     *         scopes within the source file
     */
    public List<Package> getPackages()
    {
        return Collections.unmodifiableList(packages);
    }
    
    /**
     * @return an iterator over {@link PODComment} instances representing
     *         POD comments found in the source, in their original order  
     */
    public Iterator<PODComment> getPODs()
    {
        return Collections.unmodifiableList(pods).iterator();
    }

    /**
     * @return an iterator over {@link Subroutine} instances representing
     *         subroutines found in the source, in their original order  
     */
    public Iterator<Subroutine> getSubs()
    {
        return new SubIterator();
    }
    
    /**
     * @return an iterator over {@link ModuleUse} instances representing
     *         'use module' statements found in the source, in their original order  
     */
    public Iterator<ModuleUse> getUses()
    {
        return new ModuleUseIterator();
    }
    
    public synchronized void parse()
    {
        this.pods = new ArrayList<PODComment>();
        this.packages = new ArrayList<Package>();
        
        PerlPartitioner partitioner = (PerlPartitioner)
            PartitionTypes.getPerlPartitioner(doc);
        if (partitioner == null) return;

        synchronized (partitioner.getTokensLock())
        {
            try
            {
                ParsingState state = new ParsingState(partitioner.getTokens());

                while (state.hasMoreTokens()) state.processToken();
                state.finish();
            }
            catch (BadLocationException e)
            {
                log.log(new Status(
                    Status.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Unexpected exception: " + e.getClass().getName() +
                    "; report it as a bug " +
                    "in plug-in " + PerlEditorPlugin.getPluginId(),
                    e));
            }
        }
        fireSourceFileChanged();
    }
    
    /**
     * Removes the given listener from this SourceFile.
     * Has no affect if an identical listener is not registered.
     */
    public synchronized void removeListener(ISourceFileListener listener)
    {
        listeners.remove(listener);
    }
    
    private void addPOD(PerlToken podStart, PerlToken podEnd)
        throws BadLocationException
    {
        pods.add(new PODComment(podStart, podEnd));
    }
    
    private void fireSourceFileChanged()
    {
        Object[] listeners = this.listeners.getListeners();
        for (int i = 0; i < listeners.length; i++)
            ((ISourceFileListener) listeners[i]).sourceFileChanged(this);
    }
    
    private class ParsingState
    {
        private final int tokenCount;
        private final List<PerlToken> tokens;
        private int tIndex;
        private PerlToken t;
        private int type;
        private int blockLevel;
        private boolean afterEnd;
        
        private Stack<Package> pkgStack;
        private Stack<Subroutine> subStack;
        private PerlToken podStart;
        private PerlToken packageKeyword;
        private PerlToken subKeyword;
        private PerlToken useKeyword;
        private PerlToken subName;
        private boolean inSubProto;
        private PerlToken baseKeyword;
        
        public ParsingState(List<PerlToken> tokens)
        {
            this.tIndex = 0;
            this.tokens = tokens;
            this.tokenCount = tokens.size();
            this.pkgStack = new Stack<Package>();
            this.subStack = new Stack<Subroutine>();
        }
        
        public void finish()
        {
            closePackage();
            closeSub();
        }
        
        public boolean hasMoreTokens()
        {
            return tIndex < tokenCount && !afterEnd;
        }
        
        public void processToken() throws BadLocationException
        {
            this.t = tokens.get(tIndex);
            this.type = t.getType();
            
            if (this.type == PerlTokenTypes.END)
            {
                afterEnd = true;
                return;
            }
            
            updateBlockLevel();
            updatePackageState();
            updateSubState();
            updateUseState();
            updatePODState();
            
            tIndex++;
        } 
        
        private void closePackage()
        {            
            if (pkgStack.isEmpty()) return;

            Package pkg = pkgStack.peek();
            if (blockLevel > pkg.getBlockLevel()) return;
            //System.err.println("closePackage " + pkg.getName() + " " + t);
            pkgStack.pop();
            pkg.setLastToken(tokens.get(tIndex-1));
        }
        
        private void closeSub()
        {
            if (subStack.isEmpty()) return;
            
            Subroutine sub = subStack.peek();
            if (blockLevel-1 > sub.getBlockLevel()) return;
            subStack.pop();
            if (t instanceof CurlyToken) // could be false on finish()
                sub.setCloseCurly((CurlyToken) t);
        }
        
        private Package getCurrentPackage()
        {
            if (pkgStack.isEmpty()) openPackage(new Package());
            return pkgStack.peek();
        }
        
        private void openPackage(Package pkg)
        {
            //System.err.println("openPackage " + pkg.getName() + " " + t);
            pkgStack.push(pkg);
            packages.add(pkg);
        }
        
        private void updateBlockLevel()
        {
            if (type == PerlTokenTypes.OPEN_CURLY) blockLevel++;
            else if (type == PerlTokenTypes.CLOSE_CURLY && blockLevel > 0)
            {
                closeSub();
                closePackage();
                blockLevel--;
            }
        }
        
        private void updateSubState() throws BadLocationException
        {
            if (type == PerlTokenTypes.KEYWORD_SUB)
            {
                subKeyword = t;
                subName = null;
                inSubProto = false;
            }
            if (subKeyword != null)
            {
                if (subName == null && type == PerlTokenTypes.WORD)
                {
                    subName = t;
                }
                else if (!inSubProto && type == PerlTokenTypes.SEMI)
                {
                    // Here we apparently have something like sub foo;
                    // But not something like sub foo($;$) { ... }
                    subKeyword = null;
                    subName = null;
                    inSubProto = false;
                }
                else if (type == PerlTokenTypes.OPEN_PAREN)
                {
                    inSubProto = true;
                }
                else if (type == PerlTokenTypes.CLOSE_PAREN)
                {
                    inSubProto = false;
                }
                else if (type == PerlTokenTypes.OPEN_CURLY)
                {
                    if (subName != null)
                    {
                        Subroutine sub = getCurrentPackage().addSub(
                            subKeyword, subName, (CurlyToken) t);
                        subStack.push(sub);
                    }
                    subKeyword = null;
                    subName = null;
                    inSubProto = false;
                }
            }
        }
        
        private void updatePackageState()
        {
            if (packageKeyword == null)
            {
                if (type == PerlTokenTypes.KEYWORD_PACKAGE)
                {
                    closePackage();
                    packageKeyword = t;
                }
            }
            else
            {
                if (type == PerlTokenTypes.WORD)
                {
                    openPackage(new Package(
                        packages.size(), blockLevel, packageKeyword, t));
                    packageKeyword = null;
                }
            }
        }
        
        private void updatePODState() throws BadLocationException
        {
            if (podStart == null)
            {
                if (type == PerlTokenTypes.OPEN_POD) podStart = t;
            }
            else
            {
                if (type == PerlTokenTypes.CLOSE_POD)
                {
                    addPOD(podStart, t);
                    podStart = null;
                }
            }
        }
        
        private void updateUseState() throws BadLocationException
        {
            if (useKeyword == null)
            {
                if (type == PerlTokenTypes.KEYWORD_USE) useKeyword = t;
            }
            else
            {
                String text = t.getText().trim();
                if (type == PerlTokenTypes.WORD || type == PerlTokenTypes.STRING_BODY)
                {
                    if ("base".equals(text) || "parent".equals(text))
                    {
                        baseKeyword = t;
                        return;
                    }
                    if (!"constant".equals(text) &&
                        !"vars".equals(text) &&
                        !"feature".equals(text) &&
                        !"qw".equals(text))
                    {
                        getCurrentPackage().addUse(useKeyword, t);
                        if (baseKeyword != null) getCurrentPackage().addParent(useKeyword, t);
                    }
                    if (baseKeyword == null) useKeyword = null;
                }

                // end of command..
                if (";".equals(text))
                {
                    useKeyword = null;
                    baseKeyword = null;
                }
            }
        }
    }
    
    private class SubIterator implements Iterator<Subroutine>
    {
        private Iterator<Package> pkgIterator;
        private Iterator<Subroutine> subIterator;
        
        public SubIterator()
        {
            pkgIterator = packages.iterator();
        }
        
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext()
        {
            while (subIterator == null || !subIterator.hasNext())
            {
                if (pkgIterator.hasNext())
                {
                    Package pkg = pkgIterator.next();
                    subIterator = pkg.getSubs().iterator();
                }
                else return false;
            }
            return true;
        }

        public Subroutine next()
        {
            return subIterator.next();
        }
    }
    
    private class ModuleUseIterator implements Iterator<ModuleUse>
    {
        private Iterator<Package> pkgIterator;
        private Iterator<ModuleUse> useIterator;
        
        public ModuleUseIterator()
        {
            pkgIterator = packages.iterator();
        }
        
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext()
        {
            while (useIterator == null || !useIterator.hasNext())
            {
                if (pkgIterator.hasNext())
                {
                    Package pkg = pkgIterator.next();
                    useIterator = pkg.getUses().iterator();
                }
                else return false;
            }
            return true;
        }

        public ModuleUse next()
        {
            return useIterator.next();
        }
    }
}
