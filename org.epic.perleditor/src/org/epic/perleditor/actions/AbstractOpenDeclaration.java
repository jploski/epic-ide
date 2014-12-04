package org.epic.perleditor.actions;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.epic.core.util.FileUtilities;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.*;
import org.epic.perleditor.editors.perl.SourceElement;
import org.epic.perleditor.editors.perl.SourceParser;

/**
 * Base class for implementations which attempt to find and open
 * declaration of a selected syntactic element or, if no selection
 * exists, of the element over whose invocation the caret is located.
 * This class implements the following search heuristics:
 * 
 * <ol>
 * <li>If a "target module" can be extracted from the selected element's
 *     name (according to {@link #getTargetModule}),
 *     only the referenced module is searched. This assumes the common case
 *     and does not take into account that one may actually define
 *     elements belonging to any package anywhere (with multiple
 *     packages in a single module file).
 * </li>
 * <li>Otherwise, search the active editor's source text.</li>
 * <li>Then search in modules referenced by 'use'.</li>
 * <li>Finally, search recursively in files and/or modules
 *     included by 'require' (only 'require's followed by
 *     barewords or quoted strings are considered).</li>
 * </ol>
 * 
 * The search heuristics used here will, of course, fail in many circumstances,
 * especially when applied to elements such as method invocations in OO Perl
 * code. This has to be considered a known (and extremely difficult to overcome) 
 * limitation. 
 * 
 * If the declaration is found in an external file (i.e. not in the active
 * editor), an attempt is made to open this file in the editor. However,
 * this currently only works with module files that are located in the workspace.
 * For other files, a result object indicating failure will be returned (for now).
 *  
 * @author LeO (original implementation)
 * @author jploski (complete rewrite)
 */
abstract class AbstractOpenDeclaration
{
    private static final String REQUIRE_REG_EXPR = "^[\\s]*require\\s+(\\S+)";
    private final OpenDeclarationAction action;

    //~ Constructors

    public AbstractOpenDeclaration(OpenDeclarationAction action)
    {
        this.action = action;
    }
    
    //~ Methods

    /**
     * Runs the action using the given selection within the editor.
     */
    public Result run(ITextSelection selection)
    {
        return runWithSearchString(getSearchString(selection));
    }
    
    /**
     * Runs the action based on the current selection in the editor.
     */
    public Result run()
    {
        return runWithSearchString(getSearchString(
            (ITextSelection) getEditor().getSelectionProvider().getSelection()));
    }
    
    /**
     * @return the region where the element's declaration was found,
     *         or null if not found
     */
    protected abstract IRegion findDeclaration(SourceFile sourceFile, String searchString)
        throws CoreException;
    
    /**
     * @return if {@link #getTargetModule} returns non-null,
     *         the local name of the searched for element within
     *         the target module (this method is not called otherwise)
     */
    protected abstract String getLocalSearchString(String searchString);
    
    /**
     * Returns the name of the element whose declaration should be located.
     * <p>
     * If the supplied selection's length is 0, the offset is treated as
     * the caret position and the enclosing partition is used to find
     * the element's name
     * 
     * @return selected element's name or null if none is selected
     */
    protected abstract String getSearchString(ITextSelection selection);
    
    /**
     * @return name of the target module in which to search for the
     *         declaration of the requested element if it can be deduced
     *         from <code>searchString</code>; null if the search should
     *         proceed through modules from the \@INC path and 'require's
     */
    protected abstract String getTargetModule(String searchString);

    protected void messageBox(String title, String message)
    {
        Shell shell;
        shell = PerlEditorPlugin.getWorkbenchWindow().getShell();
        MessageDialog.openInformation(shell, title, message);
    }
    
    /**
     * @param searchString
     *        name of the element whose declaration we are looking for
     * @throws CoreException 
     */
    private Result runWithSearchString(String searchString)
    {
        try { return _runWithSearchString(searchString); }
        catch (CoreException e)
        {
            getLog().log(e.getStatus());
            return new Result(Result.EXCEPTION, null, null, null);
        }
    }
    
    private Result _runWithSearchString(String searchString) throws CoreException
    {
        if (searchString == null) return Result.invalidSearch();

        String targetModule = getTargetModule(searchString);

        // if SUPER::some_sub is found, only search in parent packages.
        boolean onlySearchInParents = "SUPER".equals(targetModule);

        if ("main".equals(targetModule) || onlySearchInParents)
        {
            // Treat search for 'main::some_sub' exactly the same way
            // as a search for 'some_sub'. Also, treat search for 'SUPER::some_sub'
            // as a search for 'some_sub' in parent packages.
            searchString = getLocalSearchString(searchString);
            targetModule = null;
        }

        if (targetModule != null)
        {   
            String localSearchString = getLocalSearchString(searchString);
            File moduleFile = findModuleFile(targetModule);
            
            if (moduleFile != null)
            {
                Result res = searchModuleFile(moduleFile, localSearchString);
                if (res.isFound()) return res;
            }
            else return Result.moduleNotFound(targetModule);
        }
        else
        {
            IRegion match = null;
            if (!onlySearchInParents)
            {
                match = findDeclaration(getEditor().getSourceFile(), searchString);
            }

            if (match != null)
            {
                getEditor().selectAndReveal(match.getOffset(), match.getLength());
                return Result.found();
            }

            Result res = searchInAllParents(searchString, findParents(getEditor().getSourceFile()));
            if (res != null && res.isFound()) return res;

            String[] usedModules = findUsedModules(getEditor().getSourceFile());
            res = searchInUsedModules(searchString, usedModules);
            if (res != null && res.isFound()) return res;

            res = searchInRequires(
                searchString,
                getCurrentDir(),
                getEditor().getSourceFile().getDocument(),
                new HashSet<File>());
            if (res.isFound()) return res;
        }
        return Result.notFound(searchString);
    }

    private Result searchInAllParents(String searchString, String[] parents) throws CoreException
    {
        Result res = searchInUsedModules(searchString, parents);
        if (res != null && res.isFound()) return res;
        for (int i = 0; i < parents.length; i++)
        {
            String myParent = parents[i];
            File moduleFile = findModuleFile(myParent);
            if (moduleFile == null) continue;

            Result parentResult = searchModuleFile(moduleFile, searchString);
            if (parentResult != null && parentResult.isFound())
                return parentResult;
            SourceFile moduleSource = findSourceFile(moduleFile);
            if (moduleSource == null) continue;
            moduleSource.parse();
            return searchInAllParents(searchString, findParents(moduleSource));
        }
        return Result.notFound(searchString);
    }

    /**
     * iterates over the 'use' list and looks for searchString
     * @param searchString
     * @param usedModules
     * @return Result
     * @throws CoreException
     */
    private Result searchInUsedModules(String searchString, String[] usedModules) throws CoreException
    {
        for (int i = 0; i < usedModules.length; i++)
        {
            File moduleFile = findModuleFile(usedModules[i]);
            if (moduleFile != null)
            {
                Result res = searchModuleFile(moduleFile, searchString);
                if (res.isFound()) return res;
            }
        }
        return Result.notFound(searchString);
    }

    /**
     * translates moduleName into File handle
     * @param moduleName
     * @return moduleFile
     * @throws CoreException
     */
    private File findModuleFile(String moduleName) throws CoreException
    {
        if (moduleName.length() == 0) return null;
        
        String fileSep = File.separatorChar == '\\' ? "\\\\" : File.separator;
        String modulePath = moduleName.replaceAll("::", fileSep) + ".pm";
        List<File> dirs = getProject().getEffectiveIncPath();
        
        for (Iterator<File> i = dirs.iterator(); i.hasNext();)
        {
            File dir = i.next();
            if (".".equals(dir.getName())) dir = getCurrentDir();
            File f = new File(dir, modulePath);
            if (f.exists() && f.isFile()) return f;
        }
        return null;
    } 
    
    /**
     * @param fromDir directory for resolving relative paths in 'require's
     * @param source source document for fromFile
     * @return an array with files 'required' by the given source text;
     *         due to the regexp-based nature of the search only those
     *         'require's which use string literals or barewords are considered
     * @throws CoreException 
     */
    private File[] findRequiredFiles(File fromDir, IDocument source) throws CoreException
    {        
        String text = source.get();
        List<SourceElement> elems =
            SourceParser.getElements(text, REQUIRE_REG_EXPR, "", "", true);
        List<File> requiredFiles = new ArrayList<File>();
        
        for (Iterator<SourceElement> i = elems.iterator(); i.hasNext();)
        {
            SourceElement elem = i.next();
            String elemText = elem.getName();
            
            if (elemText.indexOf("\"") != -1 ||
                elemText.indexOf("'") != -1)
            {
                // require 'some/literal/path.pm';

                Matcher m = Pattern.compile("['\"]([^'\"]*?)['\"]").matcher(elemText);
                if (m.find())
                {
                    File requiredFile = new File(fromDir, m.group(1));
                    if (requiredFile.isFile()) requiredFiles.add(requiredFile);
                }
            }
            else
            {
                // require Some::Module;

                Matcher m = Pattern.compile("([A-Za-z0-9:]+)").matcher(elemText);
                if (m.find())
                {
                    File moduleFile = findModuleFile(m.group(1));
                    if (moduleFile != null) requiredFiles.add(moduleFile);
                }
            }                
        }
        return requiredFiles.toArray(new File[requiredFiles.size()]);
    }
    
    /**
     * @return names of modules referenced by 'use' statements from
     *         the given source text
     */
    private String[] findUsedModules(SourceFile sourceFile) throws CoreException
    {
        List<String> names = new ArrayList<String>();
        for (Iterator<?> j = sourceFile.getPackages().iterator(); j.hasNext();)
        {
            Package pkg = (Package) j.next();
            for (Iterator<ModuleUse> i = pkg.getUses().iterator(); i.hasNext();)
                names.add(i.next().getName());
        }
        return names.toArray(new String[names.size()]);
    }

    /**
     * @return (unparsed) SourceFile from an identified moduleFile
     */
    private SourceFile findSourceFile(File moduleFile) throws CoreException
    {
        IDocument doc = null;
        SourceFile src = null;
        try
        {
            doc = getSourceDocument(moduleFile);
            src = new SourceFile(null, doc);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return src;
    }

    /**
     * @return names of modules referenced by 'use base/parent' statements from
     *         the given source text
     */
    private String[] findParents(SourceFile sourceFile) throws CoreException
    {
        List<String> names = new ArrayList<String>();
        for (Iterator<Package> j = sourceFile.getPackages().iterator(); j.hasNext();)
        {
            Package pkg = j.next();
            for (Iterator<ModuleUse> i = pkg.getParents().iterator(); i.hasNext();) {
                String parentName = i.next().getName();
                names.add(parentName);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /**
     * @return the script's parent directory, if the action is executing
     *         on a .pl script (to simulate the @INC entry used when actually
     *         executing or compiling the script); '.' otherwise
     */
    private File getCurrentDir()
    {
        IEditorInput input = getEditor().getEditorInput();        
        if (!(input instanceof IFileEditorInput)) return new File(".");
        
        IPath scriptFilePath = ((IFileEditorInput) input).getFile().getLocation();        
        if (scriptFilePath == null) return new File(".");
        
        String ext = scriptFilePath.getFileExtension();        
        if (ext == null || !ext.toLowerCase().equals("pm")) // not a module = script
        {
            return scriptFilePath.toFile().getParentFile();
        }
        else return new File(".");
    }
    
    private PerlEditor getEditor()
    {
        return action.getEditor();
    }
    
    private ILog getLog()
    {
        return action.getLog();
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
    protected IDocument getSourceDocument()
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
            IDocument doc = new Document(sw.toString());
            new PerlPartitioner(getLog(), doc);
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
     * Searches the given editor for a declaration of the given element.
     * If found, the declaration is highlighted.
     *
     * @return an object indicating if the declaration was found
     */
    private Result searchEditor(PerlEditor editor, String searchString)
        throws CoreException
    {
        SourceFile sourceFile = editor.getSourceFile();
        sourceFile.parse();
        IRegion match = findDeclaration(sourceFile, searchString);
        
        if (match != null)
        {
            editor.getSite().getPage().activate(editor);
            editor.selectAndReveal(match.getOffset(), match.getLength());
            return Result.found();
        }
        else return Result.notFound(searchString);
    }
    
    /**
     * Searches a module file for a declaration of the given element.
     * The file is read from disk and might be external to the workspace.
     * 
     * @return an object indicating if the declaration was found
     */
    private Result searchExternalFile(File moduleFile, String searchString)
        throws CoreException
    {
        try
        {
            SourceFile sourceFile = new SourceFile(
                getLog(), getSourceDocument(moduleFile));
            sourceFile.parse();

            IRegion match = findDeclaration(sourceFile, searchString);
            return match != null ? Result.found() : Result.notFound(searchString);
        }
        catch (IOException e)
        {
            getLog().log(new Status(
                IStatus.ERROR,
                PerlEditorPlugin.getPluginId(),
                IStatus.OK,
                "Could not read module file " + moduleFile.getAbsolutePath(),
                e));
            return Result.exception();
        }
    }
    
    /**
     * @param fromDir directory for resolving relative paths in 'require's
     * @param source document in which to look for 'require' statements;
     *               the search continues recursively in these required files
     * @param visitedFiles
     *        a set of already visited files (used to prevent endless loops)
     * @return an object indicating whether the element's declaration was found
     *         in some file
     * @throws IOException 
     * @throws CoreException 
     */
    private Result searchInRequires(        
        String searchString,
        File fromDir,
        IDocument source,
        Set<File> visitedFiles) throws CoreException
    {
        File[] requiredFiles = findRequiredFiles(fromDir, source);

        for (int i = 0; i < requiredFiles.length; i++)
        {
            if (!visitedFiles.contains(requiredFiles[i]))
            {
                visitedFiles.add(requiredFiles[i]);
                Result res = searchModuleFile(requiredFiles[i], searchString); 
                
                if (res.isFound()) return res;
                else
                {
                    try
                    {
                        res = searchInRequires(
                            searchString,
                            requiredFiles[i].getParentFile(),
                            getSourceDocument(requiredFiles[i]),
                            visitedFiles);
                        
                        if (res.isFound()) return res;
                    }
                    catch (IOException e)
                    {
                        getLog().log(new Status(
                            IStatus.ERROR,
                            PerlEditorPlugin.getPluginId(),
                            IStatus.OK,
                            "Could not read module file " + requiredFiles[i].getAbsolutePath(),
                            e));
                        return Result.exception();
                    }
                }
            }
        }
        return Result.notFound(searchString);
    }
    
    /**
     * Searches the given module file for a declaration of the given element.
     * The search first occurs in already open editors containing that file.
     * If the declaration is found, an attempt is made to open it in an editor,
     * if not possible, displays a message about the file's location.
     * 
     * @return true if the declaration was found; false otherwise
     */
    private Result searchModuleFile(File moduleFile, String searchString)
        throws CoreException
    {
        IPath path = Path.fromOSString(moduleFile.getAbsolutePath());
        IFile fileInWorkspace = getProject().getProject()
            .getWorkspace().getRoot().getFileForLocation(path);
        
        if (fileInWorkspace != null)
            return searchModuleFile(moduleFile, fileInWorkspace, searchString);
        else
        {
            Result res = searchExternalFile(moduleFile, searchString);
            if (res.isFound())
            {
                IFileEditorInput input = FileUtilities.getFileEditorInput(path);
                PerlEditor newEditor = (PerlEditor)
                    getEditor().getSite().getPage().openEditor(
                        input,
                        getEditor().getSite().getId());
                
                return searchEditor(newEditor, searchString);
            }
            else return res;
        }
    }
    
    /**
     * Just like {@link #searchModuleFile(File, String)}, but takes into account
     * that the module file to be searched is contained in the workspace.
     */
    private Result searchModuleFile(
        File moduleFile,
        IFile fileInWorkspace,
        String searchString) throws CoreException
    {
        IWorkbenchPage page = getEditor().getSite().getPage();
        IEditorPart editor = page.findEditor(new FileEditorInput(fileInWorkspace));
        
        if (editor instanceof PerlEditor)
        {
            return searchEditor((PerlEditor) editor, searchString);
        }
        else
        {
            Result res = searchExternalFile(moduleFile, searchString); 
            if (!res.isFound()) return res;

            try
            {
                FileEditorInput input = new FileEditorInput(fileInWorkspace);
                PerlEditor newEditor = (PerlEditor)
                    getEditor().getSite().getPage().openEditor(
                        input,
                        getEditor().getSite().getId());
                
                return searchEditor(newEditor, searchString);
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
                return Result.exception();
            }
        }   
    }
    
    /**
     * Used to report the result of the open declaration action.
     */
    public static class Result
    {
        /**
         * Declaration of the requested element was found successfully.
         */
        public static final int FOUND = 0;
        
        /**
         * Declaration of the requested element could not be found
         * using the implemented search algorithm.
         */
        public static final int NOT_FOUND = 1;

        /**
         * The target module in which the search was to be performed
         * could not be located.
         */
        public static final int MODULE_NOT_FOUND = 2;
        
        /**
         * No search was performed because no string was selected.
         */
        public static final int INVALID_SEARCH = 3;
        
        /**
         * An exception occurred and was logged during the search.
         */
        public static final int EXCEPTION = 4;
        
        public final int statusCode;
        public final String searchString;
        public final String targetModule;
        public final File moduleFile;
        
        private Result(
            int statusCode,
            String searchString,
            String targetModule,
            File moduleFile)
        {
            this.statusCode = statusCode;
            this.searchString = searchString;
            this.targetModule = targetModule;
            this.moduleFile = moduleFile;
        }
        
        public boolean isFound()
        {
            return statusCode == FOUND;
        }
        
        public static Result exception()
        {
            return new Result(EXCEPTION, null, null, null);
        }
        
        public static Result found()
        {
            return new Result(FOUND, null, null, null);
        }
        
        public static Result invalidSearch()
        {
            return new Result(INVALID_SEARCH, null, null, null);
        }
        
        public static Result moduleNotFound(String targetModule)
        {
            return new Result(MODULE_NOT_FOUND, null, targetModule, null);
        }
        
        public static Result notFound(String searchString)
        {
            return new Result(NOT_FOUND, searchString, null, null);
        }
    }
}