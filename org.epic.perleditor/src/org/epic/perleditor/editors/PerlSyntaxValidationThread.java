/*
 *  PerSyntaxValidationThread.java 
 *
 *  Contributors:
 *                   Igor Alexeiuk <aie at mailru.com>
 *  Modified:             
 *                   skoehler
 */

package org.epic.perleditor.editors;

import java.awt.Toolkit;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.editors.util.PerlValidator;
import org.epic.perleditor.editors.util.StringReaderThread;

public class PerlSyntaxValidationThread extends Thread implements IdleTimerListener {
	private static final String PERL_CMD_EXT = "-c";
	private static final String PERL_ERROR_INDICATOR = " at - line ";
	//private static final int READ_BUFFER_SIZE = 128;

	private static final String[] WARNING_STRINGS =
		{ "possible", "Useless", "may", "better written as" };
        
    private final Object lock1 = new Object();
//    private final Object lock2 = new Object();
    //get monitors in that order: lock1, lock2
    //  or else dead-locks occur

	private String code = "";
    private boolean force = false;
    private boolean modified = false;

	private TextEditor fTextEditor;
	private ISourceViewer fSourceViewer;

	private int previousHashCode = 0;
	private int waitForTermination = 1000; // millis
	private int maxErrorsShown = 10;

    private StringReaderThread srt = new StringReaderThread();

	public PerlSyntaxValidationThread(TextEditor textEditor, ISourceViewer viewer) {
		super();
		this.fTextEditor = textEditor;
		this.fSourceViewer = viewer;
	}

	public void setText(String text) {
        this.setText(text, false);
	}

    //TODO method should only be called if changes are done
	public void setText(String text, boolean forceUpdate) {
        synchronized (this.lock1)
        {
            if (forceUpdate || !text.equals(this.code))
            {
                this.code = text;
                this.modified = true;
//                if (forceUpdate)
//                {
//                    synchronized (this.lock2)
//                    {
//                        this.force = true;
//                        this.lock2.notifyAll();
//                    }
//                }
    
                this.lock1.notifyAll();
            }
        }
	}

	public void setErrorsShown(int number) {
		this.maxErrorsShown = number;
	}

	public int getErrorsShown() {
		return maxErrorsShown;
	}

	public void dispose() throws InterruptedException {
        this.srt.dispose();
        this.interrupt();
        this.join(this.waitForTermination);
	}

	public void run() {
        try
        {
    		while (!Thread.interrupted())
            {
                String text;
                synchronized (this.lock1)
                {
//                    while (!this.modified)
                	if(!this.modified) {
                        this.lock1.wait();
                	}

                    this.force = false;
                    this.modified = false;
                    text = this.code;
                }

    			try
                {
    				this.validateSyntax(text);
    			}
                catch (Exception e) {
    				e.printStackTrace();
    			}                

//                long i = 1000L*PerlEditorPlugin.getDefault().getPreferenceStore().getInt(PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_PREFERENCE);
//                synchronized (this.lock2)
//                {
//                    if (!this.force)
//                        this.lock2.wait(i);
//                }
    		}
        }
        catch (InterruptedException e)
        {
            //everything is fine, and this thread will terminate
  				if (e.getMessage() == null) {
  				  System.out.println("Thread interrupted due to normal workflow in " +this.getClass().getName());
  				} else {
  				  e.printStackTrace();
  				}
        }
	}

	private boolean validateSyntax(String text) {

		Process proc = null;

		try {
			IEditorInput input = fTextEditor.getEditorInput();
			IResource resource =
				(IResource) ((IAdaptable) input).getAdapter(IResource.class);
				
			PerlValidator.validate(resource, text);
		} catch (Exception e) {
			e.printStackTrace();
			if (proc != null) {
				killProcess(proc);
			}
			return false;
		} finally {
			try {
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return true;

	}

	private void killProcess(Process proc) {
		while (true) {
			try {
				proc.destroy();
				proc.exitValue();
			} catch (Exception ex) {

				continue;
			}
			break;
		}
	}

	// Replaces all instances of the String o with the String n in the
	// StringBuffer orig if all is true, or only the first instance if all is false.
	private static void replace(
		StringBuffer orig,
		String o,
		String n,
		boolean all) {
		if (orig == null || o == null || o.length() == 0 || n == null)
			throw new IllegalArgumentException("Null or zero-length String");

		int i = 0;

		while (i + o.length() <= orig.length()) {
			if (orig.substring(i, i + o.length()).equals(o)) {
				orig.replace(i, i + o.length(), n);
				if (!all)
					break;
				else
					i += n.length();
			} else
				i++;
		}
	}

	/**
	 * Returns the resource on which to create the marker,
	 * or <code>null</code> if there is no applicable resource. This
	 * queries the editor's input using <code>getAdapter(IResource.class)</code>.
	 *
	 * @return the resource to which to attach the newly created marker
	 */
	private IResource getResource() {
		IEditorInput input = fTextEditor.getEditorInput();
		return (IResource) ((IAdaptable) input).getAdapter(IResource.class);
	}

	/* (non-Javadoc)
	 * @see org.epic.perleditor.editors.IdleTimerListener#onEditorIdle(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public synchronized void onEditorIdle(ISourceViewer viewer) {
		this.setText(((SourceViewer) viewer).getTextWidget().getText());
	}
}
