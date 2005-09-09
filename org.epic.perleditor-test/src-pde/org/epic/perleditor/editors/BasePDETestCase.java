package org.epic.perleditor.editors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.*;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.perl.editor.test.BaseTestCase;

public class BasePDETestCase extends BaseTestCase
{
    /**
     * Closes the specified editor, discards unsaved changes.
     */
    protected void closeEditor(IEditorPart editor)
    {
        IWorkbenchPage page =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        
        page.closeEditor(editor, false);
    }
    
    /**
     * Opens an editor for the specified file.
     * 
     * @param path workspace-relative path to the file
     */
    protected PerlEditor openEditor(String path) throws PartInitException
    {
        IWorkbenchPage page =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        
        IEditorInput input = new FileEditorInput(
            ResourcesPlugin.getWorkspace().getRoot().getFile(
                new Path(path)));

        return (PerlEditor)
            page.openEditor(input, "org.epic.perleditor.editors.PerlEditor");
    }
}
