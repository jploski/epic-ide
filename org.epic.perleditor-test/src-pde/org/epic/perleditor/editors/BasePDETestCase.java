package org.epic.perleditor.editors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
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
        activeWorkbenchPage().closeEditor(editor, false);
    }
    
    /**
     * Closes the specified view.
     */
    protected void closeView(IViewPart view)
    {
        activeWorkbenchPage().hideView(view);
    }
    
    /**
     * Find an open PerlEditor for the specified file.
     * 
     * @param path workspace-relative path to the file
     * @return the found PerlEditor or null if not found
     */
    protected PerlEditor findEditor(String path)
    {
        IEditorPart part = activeWorkbenchPage().findEditor(fileEditorInput(path));
        return (part instanceof PerlEditor) ? (PerlEditor) part : null;
    }
    
    /**
     * Find an open view with the specified ID.
     * 
     * @return the found view or null if not found
     */
    protected IViewPart findView(String viewId)
    {
        return activeWorkbenchPage().findView(viewId);
    }

    /**
     * Opens an editor for the specified file.
     * 
     * @param path workspace-relative path to the file
     */
    protected PerlEditor openEditor(String path) throws PartInitException
    {
        return (PerlEditor)
            activeWorkbenchPage().openEditor(
                fileEditorInput(path), PerlEditor.PERL_EDITOR_ID);
    }
    
    /**
     * Spins the event loop for the specified duration of time.
     */
    protected void spinEventLoop(long timeMillis)
        throws InterruptedException
    {
        Display display = Display.getDefault();
        long t1 = System.currentTimeMillis();
        while (t1 + timeMillis > System.currentTimeMillis())
        {
            while (display.readAndDispatch());
            Thread.sleep(100);
        }
    }
    
    private IWorkbenchPage activeWorkbenchPage()
    {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }
    
    private IEditorInput fileEditorInput(String path)
    {
        return new FileEditorInput(
            ResourcesPlugin.getWorkspace().getRoot().getFile(
                new Path(path)));
    }
}
