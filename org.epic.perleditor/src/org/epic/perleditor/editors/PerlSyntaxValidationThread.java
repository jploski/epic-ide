package org.epic.perleditor.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.ISourceViewer;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.*;

/**
 * @author Igor Alexeiuk <aie at mailru.com>
 * @author skoehler
 * @author jploski
 */
public class PerlSyntaxValidationThread
    extends Thread
    implements IdleTimerListener
{      
    private final Object lock = new Object();
    private final int waitForTermination = 1000; // millis
    
    private IResource resource;
    private IDocument document;
    private String code;
    
/*    private final IDocumentListener documentListener = new IDocumentListener() {
        public void documentAboutToBeChanged(DocumentEvent event) { }
        public void documentChanged(DocumentEvent event)
        {
            synchronized (lock) { modified = true; }
        }
    };*/    

    private StringReaderThread srt = new StringReaderThread();

	public PerlSyntaxValidationThread()
    {
		super("PerlSyntaxValidationThread");       
	}
    
    /**
     * Terminates this thread, releases resources.
     */
    public void dispose() throws InterruptedException
    {
        srt.dispose();
        interrupt();
        join(waitForTermination);
    }
    
    public void onEditorIdle(ISourceViewer viewer)
    {
        validate();
    }
    
    public void revalidate()
    {
        validate();
    }
    
    public void setDocument(IResource resource, IDocument document)
    {
        synchronized (lock)
        {
            this.document = document;
            this.resource = resource;
            
            if (this.document != null) validate();
        }
    }

	public void run()
    {
        try { runImpl(); }
        catch (InterruptedException e) { /* we were requested to terminate */ }
    }
    
    private void runImpl() throws InterruptedException
    {
        int exceptions = 0;
		while (!Thread.interrupted())
        {
            String text;
            synchronized (lock)
            {
                while (code == null) lock.wait();
                text = code;
                code = null;
            }
            
            try { PerlValidator.instance().validate(resource, text); }
            catch (PerlValidatorException e)
            {
                if (++exceptions < 5) // avoid spamming the log spamming
                {
                    PerlEditorPlugin.getDefault().getLog().log(
                        new Status(Status.ERROR,
                            PerlEditorPlugin.getPluginId(),
                            10004, // TODO: use some sort of constant
                            "An unexpected exception occurred while validating " +
                            resource.getProjectRelativePath(),
                            e));
                }
            }
		}
	}

	private void validate()
    {
        synchronized (lock)
        {
            code = document.get();
            lock.notifyAll();
        }
	}
}
