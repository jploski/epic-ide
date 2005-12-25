package org.epic.core.model;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.epic.core.parser.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlPartitioner;

/**
 * A parsed Perl source file. This class provides access to 
 * {@link org.epic.core.model.ISourceElement}s recognised in a Perl
 * source file.
 * 
 * @author jploski
 */
public class SourceFile
{
    private final ILog log;
    private final IDocument doc;
    private List pods;
    private List packages;
    
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
        this.pods = Collections.EMPTY_LIST;
        this.packages = Collections.EMPTY_LIST;
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
    public List getPackages()
    {
        return Collections.unmodifiableList(packages);
    }
    
    /**
     * @return an iterator over {@link PODComment} instances representing
     *         POD comments found in the source, in their original order  
     */
    public Iterator getPODs()
    {
        return Collections.unmodifiableList(pods).iterator();
    }

    /**
     * @return an iterator over {@link Subroutine} instances representing
     *         subroutines found in the source, in their original order  
     */
    public Iterator getSubs()
    {
        return new SubIterator();
    }
    
    public synchronized void parse()
    {
        this.pods = new ArrayList();
        this.packages = new ArrayList();
        
        PerlPartitioner partitioner = (PerlPartitioner) doc.getDocumentPartitioner();
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
    }
    
    private void addPOD(PerlToken podStart, PerlToken podEnd)
        throws BadLocationException
    {
        pods.add(new PODComment(podStart, podEnd));
    }
    
    private class ParsingState
    {
        private final int tokenCount;
        private final List tokens;
        private int tIndex;
        private PerlToken t;
        private int type;
        private int blockLevel;
        
        private Stack pkgStack;
        private Stack subStack;
        private PerlToken podStart;
        private PerlToken packageKeyword;
        private PerlToken subKeyword;
        private PerlToken useKeyword;
        private PerlToken subName;
        
        public ParsingState(List tokens)
        {
            this.tIndex = 0;
            this.tokens = tokens;
            this.tokenCount = tokens.size();
            this.pkgStack = new Stack();
            this.subStack = new Stack();
        }
        
        public void finish()
        {
            closePackage();
            closeSub();
        }
        
        public boolean hasMoreTokens()
        {
            return tIndex < tokenCount;
        }
        
        public void processToken() throws BadLocationException
        {
            this.t = (PerlToken) tokens.get(tIndex);
            this.type = t.getType();
            
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

            Package pkg = (Package) pkgStack.peek();
            if (blockLevel > pkg.getBlockLevel()) return;
            //System.err.println("closePackage " + pkg.getName() + " " + t);
            pkgStack.pop();
            pkg.setLastToken((PerlToken) tokens.get(tIndex-1));
        }
        
        private void closeSub()
        {
            if (subStack.isEmpty()) return;
            
            Subroutine sub = (Subroutine) subStack.peek();
            if (blockLevel-1 > sub.getBlockLevel()) return;
            subStack.pop();
            if (t instanceof CurlyToken) // could be false on finish()
                sub.setCloseCurly((CurlyToken) t);
        }
        
        private Package getCurrentPackage()
        {
            if (pkgStack.isEmpty()) openPackage(new Package());
            return (Package) pkgStack.peek();
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
            else if (type == PerlTokenTypes.CLOSE_CURLY)
            {
                closeSub();
                closePackage();
                blockLevel--;
            }
        }
        
        private void updateSubState() throws BadLocationException
        {
            if (subKeyword == null)
            {
                if (type == PerlTokenTypes.KEYWORD_SUB) subKeyword = t;
            }
            else
            {
                if (subName == null && type == PerlTokenTypes.WORD)
                {
                    subName = t;
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
                if (type == PerlTokenTypes.WORD)
                {
                    String text = t.getText();
                    if (!"constant".equals(text) &&
                        !"warnings".equals(text) &&
                        !"strict".equals(text) &&
                        !"vars".equals(text))
                    {
                        getCurrentPackage().addUse(useKeyword, t);
                    }
                    useKeyword = null;
                }
            }
        }
    }
    
    private class SubIterator implements Iterator
    {
        private Iterator pkgIterator;
        private Iterator subIterator;
        
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
                    Package pkg = (Package) pkgIterator.next();
                    subIterator = pkg.getSubs().iterator();
                }
                else return false;
            }
            return true;
        }

        public Object next()
        {
            return subIterator.next();
        }
    }
}
