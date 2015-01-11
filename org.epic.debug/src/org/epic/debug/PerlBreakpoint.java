package org.epic.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.Breakpoint;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.core.util.FileUtilities;

/**
 * @author ruehl
 */
public abstract class PerlBreakpoint extends Breakpoint
{
    public static final String INVALID_POS = "PerlDebug_INVALID_POS";

    public PerlBreakpoint()
    {
    }

    public IEditorInput getEditorInput()
    {
        IWorkbench bench = PerlDebugPlugin.getDefault().getWorkbench();
        if (bench != null)
        {
            IWorkbenchWindow window = bench.getActiveWorkbenchWindow();
            if (window != null)
            {
                IWorkbenchPage page = window.getActivePage();
                if (page != null)
                {
                    FileEditorInput input = new FileEditorInput(
                        (IFile) getMarker().getResource());
                    return input;
                }
            }
        }
    
        return FileUtilities.getFileEditorInput(getMarker().getResource()
            .getLocation());
    }

    public String getModelIdentifier()
    {
        return PerlDebugPlugin.getUniqueIdentifier();
    }

    public IPath getResourcePath()
    {
        return getMarker().getResource().getLocation();
    }

    public boolean isInvalidBreakpointPosition()
    {
        return getMarker().getAttribute(INVALID_POS, false);
    }
    
    public void pendingRemove() throws CoreException
    {
    }

    public void setInvalidBreakpointPosition(boolean value)
        throws CoreException
    {
        getMarker().setAttribute(INVALID_POS, value);
    }

    protected IDocument getDocument()
    {
        IDocument doc = null;
        IWorkbench bench = PerlDebugPlugin.getDefault().getWorkbench();
        if (bench != null)
        {
            IWorkbenchWindow window = bench.getActiveWorkbenchWindow();
            if (window != null)
            {
                IWorkbenchPage page = window.getActivePage();
                if (page != null)
                {
                    FileEditorInput input = new FileEditorInput(
                        (IFile) getMarker().getResource());
                    TextEditor editor = (TextEditor) page.findEditor(input);
                    doc = editor.getDocumentProvider().getDocument(input);
                }
            }
        }
        if (doc == null)
        {
            StringBuffer sourceCode = new StringBuffer();

            int BUF_SIZE = 1024;

            // Get the file content
            char[] buf = new char[BUF_SIZE];
            File inputFile = new File(getMarker().getResource().getLocation()
                .toString());
            BufferedReader in;
            try
            {
                in = new BufferedReader(new FileReader(inputFile));

                int read = 0;
                while ((read = in.read(buf)) > 0)
                {
                    sourceCode.append(buf, 0, read);
                }
                in.close();
            }
            catch (FileNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
            doc = new Document(sourceCode.toString());
        }

        return doc;
    }
}
