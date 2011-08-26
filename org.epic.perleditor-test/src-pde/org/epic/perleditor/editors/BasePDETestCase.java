package org.epic.perleditor.editors;

import java.io.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.core.util.FileUtilities;
import org.epic.perl.editor.test.BaseTestCase;

public class BasePDETestCase extends BaseTestCase
{
    private final String spinEventLoopLock = "spinEventLoopLock";
    private boolean spinEventLoopInterrupted;

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
     * Opens a PerlEditor on a file located outside of the workspace.
     */
    protected PerlEditor openEditor(final File file) throws CoreException
    {
        IPath path = new Path(file.getAbsolutePath());
        IFileEditorInput input = FileUtilities.getFileEditorInput(path);
        
        input.getFile().getParent().refreshLocal(
            IResource.DEPTH_INFINITE,
            new NullProgressMonitor());

        return (PerlEditor)
            activeWorkbenchPage().openEditor(
                input, PerlEditor.PERL_EDITOR_ID);
    }
    
    /**
     * Forces spinEventLoop to throw an InterruptedException.
     */
    protected void interruptSpinEventLoop()
    {
        synchronized (spinEventLoopLock)
        {
            spinEventLoopInterrupted = true;
            spinEventLoopLock.notifyAll();
        }
    }

    /**
     * Spins the event loop for the specified duration of time.
     */
    protected void spinEventLoop(long timeMillis)
        throws InterruptedException
    {
        Display display = Display.getDefault();
        while (display.readAndDispatch());
        long t1 = System.currentTimeMillis();
        boolean interrupted;
        synchronized (spinEventLoopLock)
        {
            while (t1 + timeMillis > System.currentTimeMillis() && !spinEventLoopInterrupted)
            {
                while (display.readAndDispatch());
                spinEventLoopLock.wait(100);
            }
            interrupted = spinEventLoopInterrupted;
            spinEventLoopInterrupted = false;
        }
        if (interrupted) throw new InterruptedException("spinEventLoop interrupted");
    }
    
    protected IWorkbenchPage activeWorkbenchPage()
    {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }
    
    private IEditorInput fileEditorInput(String path)
    {
        return new FileEditorInput(
            ResourcesPlugin.getWorkspace().getRoot().getFile(
                new Path(path)));
    }

    /**
     * Writes a string of text to the specified file (encoded in ISO-8859-1).
     */
    protected void writeToFile(File tmpFile, String string) throws IOException
    {
        PrintWriter out = null;
        
        try
        {
            out = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(tmpFile), "ISO-8859-1"));
            
            out.print(string);
        }
        finally
        {
            if (out != null) try { out.close(); } catch (Exception e) { }
        }
    }
}
