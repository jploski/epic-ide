package org.epic.perleditor.popupmenus;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.core.PerlCore;
import org.epic.core.PerlProject;
import org.epic.core.model.*;
import org.epic.core.model.Package;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.actions.PerlEditorAction;
import org.epic.perleditor.editors.*;

/**
 * Attempts to find and open declaration of a selected subroutine or,
 * if no selection exists, of the subroutine over whose invocation
 * the caret is located. This action is based on some heuristics
 * and is thus not reliable:
 * 
 * <ol>
 * <li>If the selected subroutine's name starts with a module prefix,
 *     only the referenced module is searched. This assumes the common case
 *     and does not take into account that one may actually define
 *     subroutines belonging to any package anywhere (with multiple
 *     packages in a single module file).
 * </li>
 * <li>Otherwise, search the active editor's source text.</li>
 * <li>Then search in modules referenced by 'use'.</li>
 * </ol>
 * 
 * The search heuristics used here will, of course, fail in many circumstances,
 * especially when applied to method invocations in OO Perl code. This has to
 * be considered a known (and extremely difficult to overcome) limitation. 
 * 
 * If the subroutine is found in an external file (i.e. not in the active
 * editor), an attempt is made to open this file in the editor. However,
 * this only works with module files that are located in the workspace.
 * For other files, a hint about the file's location will be displayed (for now).
 *  
 * @author LeO (original implementation)
 * @author jploski (complete rewrite)
 */
public class OpenDeclaration extends PerlEditorAction
{
    public OpenDeclaration()
    {
    }
    
    public OpenDeclaration(PerlEditor editor)
    {
        super(editor);
    }

    /**
     * Runs the action using the given selection within the editor.
     */
    public void run(ITextSelection selection)
    {
        runWithFullSubName(getSelectedSubName(selection));
    }
    
    /**
     * Runs the action based on the current selection in the editor.
     */
    public void run()
    {
        runWithFullSubName(getSelectedSubName(
            (ITextSelection) getEditor().getSelectionProvider().getSelection()));
    }
    
    /**
     * @param subName
     *        subroutine name, optionally prefixed by a module name
     * @throws CoreException 
     */
    private void runWithFullSubName(String subName)
    {
        try { _runWithFullSubName(subName); }
        catch (CoreException e)
        {
            getLog().log(e.getStatus());
        }
    }
    
    private void _runWithFullSubName(String subName) throws CoreException
    {   
        if (subName == null)
        {
            messageBox(
                "No subroutine name selected",
                "No valid SUB-name could be located within the selection scope.");
            return;
        }
        
        if (subName.indexOf("::") != -1)
        {
            int lastSepIndex = subName.lastIndexOf("::");
            String modulePrefix = subName.substring(0, lastSepIndex);
            subName = subName.substring(lastSepIndex + 2); 
            File moduleFile = findModuleFile(modulePrefix);
            
            if (moduleFile != null)
            {
                if (searchModuleFile(moduleFile, subName)) return;
            }
            else messageBox(
                "Module file not found",
                "Could not locate module file for prefix " + modulePrefix);
        }
        else
        {
            IRegion match = findSubDeclaration(
                getEditor().getSourceFile(), subName);
            
            if (match != null)
            {
                getEditor().selectAndReveal(match.getOffset(), match.getLength());
                return;
            }
            else
            {
                String[] usedModules = findUsedModules(getEditor().getSourceFile());
                for (int i = 0; i < usedModules.length; i++)
                {
                    File moduleFile = findModuleFile(usedModules[i]);
                    if (moduleFile != null)
                    {
                        if (searchModuleFile(moduleFile, subName)) return;
                    }
                }
                // TODO add require traversal?
            }
        }
        
        messageBox(
            "Declaration not found",
            "Could not locate declaration for \"" + subName + "\"");
    }

    private void messageBox(String title, String message)
    {
        Shell shell;
        shell = PerlEditorPlugin.getWorkbenchWindow().getShell();
        MessageDialog.openInformation(shell, title, message);
    }
    
    protected String getPerlActionId()
    {
        return PerlEditorActionIds.OPEN_SUB;
    }
    
    private File findModuleFile(String moduleName) throws CoreException
    {
        if (moduleName.length() == 0) return null;
        
        String fileSep = File.separatorChar == '\\' ? "\\\\" : File.separator;
        String modulePath = moduleName.replaceAll("::", fileSep) + ".pm";
        List dirs = getProject().getEffectiveIncPath();
        
        for (Iterator i = dirs.iterator(); i.hasNext();)
        {
            File f = new File((File) i.next(), modulePath);
            if (f.exists() && f.isFile()) return f;
        }
        return null;
    } 
    
    /**
     * @return the region where the sub name was found,
     *         or null if not found
     */
    private IRegion findSubDeclaration(SourceFile sourceFile, String subName)
        throws CoreException
    {
        for (Iterator i = sourceFile.getSubs(); i.hasNext();)
        {
            Subroutine sub = (Subroutine) i.next();
            if (sub.getName().equals(subName))
                return new Region(sub.getOffset(), sub.getLength());
        }
        return null;
    }
    
    /**
     * @return names of modules referenced by 'use' statements from
     *         the given source text
     */
    private String[] findUsedModules(SourceFile sourceFile) throws CoreException
    {
        List names = new ArrayList();
        for (Iterator j = sourceFile.getPackages().iterator(); j.hasNext();)
        {
            Package pkg = (Package) j.next();
            for (Iterator i = pkg.getUses().iterator(); i.hasNext();)
                names.add(((ISourceElement) i.next()).getName());
        }
        return (String[]) names.toArray(new String[names.size()]);
    }
    
    /**
     * @return project with the edited source file from which
     *         OpenDeclaration was invoked
     */
    private PerlProject getProject()
    {
        IEditorInput input = getEditor().getEditorInput();
        IResource resource = (IResource) ((IAdaptable) input)
            .getAdapter(IResource.class);
        
        return PerlCore.create(resource.getProject());
    }
    
    /**
     * @return edited source document of the editor from which
     *         OpenDeclaration was invoked
     */
    private IDocument getSourceDocument()
    {
        return getSourceDocument(getEditor());
    }
    
    /**
     * @return document for the given source file, partitioned by PerlPartitioner
     */
    private IDocument getSourceDocument(File file) throws IOException
    {
        StringWriter sw = new StringWriter();        
        BufferedReader r = null;
        
        try
        {
            r = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "ISO-8859-1")); // TODO use which encoding?
            
            char[] buf = new char[4096];
            int bread;
            while ((bread = r.read(buf)) > 0) sw.write(buf, 0, bread);
            Document doc = new Document(sw.toString());
            PerlPartitioner p = new PerlPartitioner(getLog());
            doc.setDocumentPartitioner(p);
            p.connect(doc);
            return doc;
        }
        finally
        {
            if (r != null) try { r.close(); } catch (IOException e) { }
        }
    }
    
    /**
     * @return edited source document in the given editor
     */
    private IDocument getSourceDocument(PerlEditor editor)
    {
        return editor.getDocumentProvider().getDocument(editor.getEditorInput());
    }

    /**
     * Returns the currently selected subroutine name, including the package
     * name prefix (if present).
     * <p>
     * If the supplied selection's length is 0, the offset is treated as the
     * caret position and the enclosing partition is returned as the subroutine
     * name.
     * 
     * @return selected subroutine name or null if none is selected
     */
    private String getSelectedSubName(ITextSelection selection)
    {
        // Note that we rely heavily on the correct partitioning delivered
        // by PerlPartitioner. When in doubt, fix PerlPartitioner instead of
        // adding workarounds here.

        IDocument doc = getSourceDocument();

        try
        {
            ITypedRegion partition = doc.getPartition(selection.getOffset());
            if (!partition.getType().equals(PartitionTypes.DEFAULT)) return null;
            else
            {
                String subName =
                    doc.get(partition.getOffset(), partition.getLength());
                return subName.indexOf('&') == 0 ? subName.substring(1) : subName;
            }
        }
        catch (BadLocationException e)
        {
            return null; // should never happen
        }
    }
    
    /**
     * Searches the given editor for a declaration of the given sub.
     * If found, the declaration is highlighted.
     *
     * @return true if the declaration was found; false otherwise
     */
    private boolean searchEditor(PerlEditor editor, String subName)
        throws CoreException
    {
        SourceFile sourceFile = editor.getSourceFile();
        sourceFile.parse();
        IRegion match = findSubDeclaration(sourceFile, subName);
        
        if (match != null)
        {
            editor.getSite().getPage().activate(editor);
            editor.selectAndReveal(match.getOffset(), match.getLength());
            return true;
        }
        else return false;
    }
    
    /**
     * Searches a module file for a declaration of the given sub.
     * The file is read from disk and might be external to the workspace.
     * 
     * @return true if the declaration was found; false otherwise
     */
    private boolean searchExternalFile(File moduleFile, String subName)
        throws CoreException
    {
        try
        {
            SourceFile sourceFile = new SourceFile(
                getLog(), getSourceDocument(moduleFile));
            sourceFile.parse();

            IRegion match = findSubDeclaration(sourceFile, subName);
            return match != null;
        }
        catch (IOException e)
        {
            getLog().log(new Status(
                IStatus.ERROR,
                PerlEditorPlugin.getPluginId(),
                IStatus.OK,
                "Could not read module file " + moduleFile.getAbsolutePath(),
                e));
            return false;
        }
    }
    
    /**
     * Searches the given module file for a declaration of the given sub.
     * The search first occurs in already open editors containing that file.
     * If the declaration is found, an attempt is made to open it in an editor,
     * if not possible, displays a message about the file's location.
     * 
     * @return true if the declaration was found; false otherwise
     */
    private boolean searchModuleFile(File moduleFile, String subName)
        throws CoreException
    {
        IPath path = Path.fromOSString(moduleFile.getAbsolutePath());
        IFile fileInWorkspace = getProject().getProject()
            .getWorkspace().getRoot().getFileForLocation(path);
        
        if (fileInWorkspace != null)
            return searchModuleFile(moduleFile, fileInWorkspace, subName);
        else
        {
            if (searchExternalFile(moduleFile, subName))
            {
                messageBox(
                    "Definition found in external module",
                    "A potential definition of " + subName +
                    " was found in file " + moduleFile.getAbsolutePath() +
                    " outside of the workspace. EPIC cannot open such external" +
                    " files in the editor yet.");
                return true;
            }
            else return false;
        }
    }
    
    /**
     * Just like {@link #searchModuleFile(File, String)}, but takes into account
     * that the module file to be searched is contained in the workspace.
     */
    private boolean searchModuleFile(
        File moduleFile,
        IFile fileInWorkspace,
        String subName) throws CoreException
    {
        IWorkbenchPage page = getEditor().getSite().getPage();
        IEditorPart editor = page.findEditor(new FileEditorInput(fileInWorkspace));
        
        if (editor instanceof PerlEditor)
        {
            return searchEditor((PerlEditor) editor, subName);
        }
        else
        {
            if (!searchExternalFile(moduleFile, subName)) return false;

            try
            {
                FileEditorInput input = new FileEditorInput(fileInWorkspace);
                PerlEditor newEditor = (PerlEditor)
                    getEditor().getSite().getPage().openEditor(
                        input,
                        getEditor().getSite().getId());
                
                return searchEditor(newEditor, subName);
            }
            catch (PartInitException e)
            {
                getLog().log(new Status(
                    IStatus.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Problems encountered while opening editor for " +
                    moduleFile.getAbsolutePath(),
                    e));
                return false;
            }
        }   
    }
}