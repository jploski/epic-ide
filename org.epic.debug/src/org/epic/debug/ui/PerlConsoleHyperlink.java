package org.epic.debug.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.ui.*;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.core.util.FileUtilities;
import org.epic.debug.PerlDebugPlugin;
import org.epic.perleditor.editors.PerlEditor;

/**
 * A hyperlink in a console associated with a Perl launch configuration. 
 */
public class PerlConsoleHyperlink implements IHyperlink
{
    private final String path;
    private final int lineNumber;
    
    public PerlConsoleHyperlink(String path, int line)
    {
        this.path = path;
        this.lineNumber = line-1;
    }
    
    public void linkEntered()
    {
    }

    public void linkExited()
    {
    }

    public void linkActivated()
    {
        try
        {  
            PerlEditor editor = (PerlEditor) IDE.openEditor(
                activeWorkbenchPage(), getEditorInput().getFile(), true, true);
            
            IDocument doc = editor.getViewer().getDocument();
            IRegion line = doc.getLineInformation(lineNumber);
            editor.selectAndReveal(line.getOffset(), line.getLength());
        }
        catch (BadLocationException e)
        {
            PerlDebugPlugin.log(e);
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
        }
    }
    
    private IWorkbenchPage activeWorkbenchPage()
    {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }
    
    private IFileEditorInput getEditorInput()
    {
        IPath path = new Path(this.path);
        IFile fileInWorkspace =
            ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
        
        return fileInWorkspace != null
            ? new FileEditorInput(fileInWorkspace)
            : FileUtilities.getFileEditorInput(path);
    }
}
