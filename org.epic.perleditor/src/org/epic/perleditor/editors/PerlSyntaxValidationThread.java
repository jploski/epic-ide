/*
 *  PerSyntaxValidationThread.java 
 *
 *  Contributors:
 *                   Igor Alexeiuk <aie at mailru.com>
 *  Modified:             
 *                   skoehler
 */

package org.epic.perleditor.editors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;
import org.epic.perleditor.editors.util.StringReaderThread;

public class PerlSyntaxValidationThread extends Thread {
	private static final String PERL_CMD_EXT = "-c";
	private static final String PERL_ERROR_INDICATOR = " at - line ";
	//private static final int READ_BUFFER_SIZE = 128;

	private static final String[] WARNING_STRINGS =
		{ "possible", "Useless", "may", "better written as" };
        
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
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
                if (forceUpdate)
                {
                    synchronized (this.lock2)
                    {
                        this.force = true;
                        this.lock2.notifyAll();
                    }
                }
    
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
                    while (!this.modified)
                        this.lock1.wait();

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

                long i = 1000L*PerlEditorPlugin.getDefault().getPreferenceStore().getInt(PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_PREFERENCE);
                synchronized (this.lock2)
                {
                    if (!this.force)
                        this.lock2.wait(i);
                }
    		}
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
	}

	private boolean validateSyntax(String text) {

		Process proc = null;

		try {
			IEditorInput input = fTextEditor.getEditorInput();
			IResource resource =
				(IResource) ((IAdaptable) input).getAdapter(IResource.class);


			// Construct command line parameters
			List cmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(
					fTextEditor);
			cmdList.add(PERL_CMD_EXT);
			
			if(PerlEditorPlugin.getDefault().getWarningsPreference()) {
				cmdList.add("-w");
			}

			String[] cmdParams =
				(String[]) cmdList.toArray(new String[cmdList.size()]);

            // Get working directory -- Fixes Bug: 736631
			String workingDir =
				((IFileEditorInput) fTextEditor.getEditorInput())
					.getFile()
					.getLocation()
					.makeAbsolute()
					.removeLastSegments(1)
					.toString();
			

			/*
			 * Due to Java Bug #4763384 sleep for a very small amount of time
			 * immediately after starting the subprocess
			*/
			proc = Runtime.getRuntime().exec(cmdParams, null, new File(workingDir));
			Thread.sleep(1);

			InputStream in = proc.getErrorStream();		
			OutputStream out = proc.getOutputStream();
            //TODO which charset?
            Reader inr = new InputStreamReader(in);
            this.srt.read(inr);

            try {			
                PerlExecutableUtilities.writeStringToStream(text, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
			out.close();
			
			String content = srt.getResult();
            inr.close();		
			in.close();
            
            //TODO check if content is empty (indicates error)

			// DEBUG start
			System.out.println("-----------------------------------------");
			System.out.println("           OUTPUT");
			System.out.println("-----------------------------------------");
			System.out.println(content);
			System.out.println("-----------------------------------------");
			// DEBUG END

			String line = null;
			List lines = new ArrayList();
			int index;

			// Compare checksums
			int hashCode = content.toString().hashCode();

			if (previousHashCode == hashCode) {

				return true;
			}

			previousHashCode = hashCode;

			StringTokenizer st = new StringTokenizer(content, "\n");

			int lineCount = 0;

			while (st.hasMoreTokens()) {
				line = st.nextToken();
				if (line.indexOf("\r") != -1) {
					line = line.substring(0, line.indexOf("\r"));
				}

				lines.add(line);
				if (++lineCount >= maxErrorsShown) {
					break;
				}

			}

			//Delete markers
			resource.deleteMarkers(IMarker.PROBLEM, true, 1);

			// Hash for tracking line severity
			Map lineHash = new Hashtable();

			// Markers have to be added in reverse order
			// Otherwise lower line number will appear at the end of the list
			for (int i = lines.size() - 1; i >= 0; i--) {
				line = (String) lines.get(i);

				// If error message is not from temporary file, skip
				// TODO
				// temporarily deleted
				/*
				if (line.indexOf(tmpFileName) == -1) {
					continue;
				}
				*/

				// Delete filename from error message
				StringBuffer lineSb = new StringBuffer(line);
				line = lineSb.toString();

				if ((index = line.indexOf(PERL_ERROR_INDICATOR)) != -1) {

					// truncatedLIne is the stripped error-line up to the next " " after the line number if present
					// To avoid cluttering with other "." and "," which might occur in the error message
					String truncatedLine = line;
					if (truncatedLine
						.indexOf(" ", index + PERL_ERROR_INDICATOR.length() + 1)
						!= -1) {
						truncatedLine =
							truncatedLine.substring(
								0,
								truncatedLine.indexOf(
									" ",
									index + PERL_ERROR_INDICATOR.length() + 1));
					}

					int endIndex;
					if ((endIndex = truncatedLine.indexOf(".", index)) == -1) {
						endIndex = truncatedLine.indexOf(",", index);
					}

					if (endIndex == -1) {
						continue;
					}

					String lineNr =
						truncatedLine.substring(
							index + PERL_ERROR_INDICATOR.length(),
							endIndex);

					// If there is an addition to the error message
					if (i + 1 < lines.size()) {
						if (((String) lines.get(i + 1)).startsWith(" ")) {
							line += " " + (String) lines.get(i + 1);
						}
					}

					// Check if it's a warning
					boolean isWarning = false;

					for (int x = 0; x < WARNING_STRINGS.length; x++) {
						if (truncatedLine.indexOf(WARNING_STRINGS[x]) != -1) {
							isWarning = true;
						}
					}

					Map attributes = new HashMap(11);

					if (isWarning) {
						attributes.put(
							IMarker.SEVERITY,
							new Integer(IMarker.SEVERITY_WARNING));
					} else {
						attributes.put(
							IMarker.SEVERITY,
							new Integer(IMarker.SEVERITY_ERROR));
					}

					attributes.put(IMarker.MESSAGE, line);

					attributes.put(
						IMarker.LINE_NUMBER,
						new Integer(Integer.parseInt(lineNr)));

					// Check if a marker with a higher severity already exists
					boolean doUnderline;
					Object obj =
						lineHash.get(new Integer(Integer.parseInt(lineNr)));
					if (obj == null) {
						doUnderline = true;
					} else if (
						((Integer) obj).intValue() == IMarker.SEVERITY_ERROR) {
						doUnderline = false;
					} else {
						doUnderline = true;
					}

					if (doUnderline) {
						lineHash.put(
							new Integer(Integer.parseInt(lineNr)),
							isWarning
								? new Integer(IMarker.SEVERITY_WARNING)
								: new Integer(IMarker.SEVERITY_ERROR));
						// Get start and end offset
						int lineOffset = 0;
						try {
							lineOffset =
								fSourceViewer.getDocument().getLineOffset(
									Integer.parseInt(lineNr) - 1);
						} catch (Exception ex) {
							continue;
						}

						int endOfLine = text.indexOf("\n", lineOffset);
						String markerLine;

						if (endOfLine != -1) {
							markerLine = text.substring(lineOffset, endOfLine);
						} else {
							markerLine = text.substring(lineOffset);
						}

						char[] bytes = markerLine.toCharArray();

						int start = 0;
						while (start < bytes.length) {
							if (bytes[start] != '\t' && bytes[start] != ' ') {
								break;
							}
							start++;
						}

						start += lineOffset;

						int end = start + markerLine.trim().length();

						attributes.put(IMarker.CHAR_START, new Integer(start));
						attributes.put(IMarker.CHAR_END, new Integer(end));
					}
					AddEditorMarker ed = new AddEditorMarker(fTextEditor);
					ed.addMarker(attributes, IMarker.PROBLEM);

				}
			}

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
}
