/*
 * Created on Jan 3, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.perleditor.editors.util;

import gnu.regexp.RE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ui.IEditorDescriptor;
import org.epic.core.Constants;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.AddEditorMarker;

/**
 * @author luelljoc
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PerlValidator {

	private static final String PERL_CMD_EXT = "-c";
	private static final String PERL_ERROR_INDICATOR = " at - line ";
	private static int maxErrorsShown = 10;
	//	private static final String[] WARNING_STRINGS =
	//		{ "possible", "Useless", "may", "better written as" };
	private static HashMap errorMessagesHash = null;

	private static final int BUF_SIZE = 1024;
	
	private static void initializeErrorsAndWarnings() {
				//		Initialize Hash containing possible Warning/Error messages
				try {
					 if (errorMessagesHash == null) {
						 errorMessagesHash = new HashMap();
	
						 ResourceBundle errorBundle =
							 ResourceBundle.getBundle(
								 "org.epic.perleditor.editors.errorsAndWarnings");
	
						 RE re;
	
						 // Populate the error messages hash
						 for (Enumeration enum = errorBundle.getKeys();
							 enum.hasMoreElements();
							 ) {
								 String index = (String) enum.nextElement();
								 String complete = (String) errorBundle.getObject(index);
								 int tabIndex = complete.indexOf("\t");
								 String key = complete.substring(0, tabIndex);
								 String value = complete.substring(tabIndex + 1);
		
								 String convKey = new String(key);
		
								 // Substitute "( ) [ ]?|*+\" with .
								 re = new RE("[\\(\\)\\[\\]\\?\\|\\*\\+\\\\]");
								 convKey = re.substituteAll(convKey, ".");
		
								 //	Substitute "%s %c %d %lx" with .*
								 re = new RE("%([sdcl][x]{0,1})");
								 convKey = re.substituteAll(convKey, ".*");
		
								 // Substitute %.[0-9]s with .*
								 re = new RE("%\\.[0-9]s");
								 convKey = re.substituteAll(convKey, ".*");
		
								 errorMessagesHash.put(convKey, value);
						 }
					 }
				}
				catch(Exception e) {
					 e.printStackTrace();
				}
	}

	public static boolean validate(IResource resource) {
		try {
			//	Check if resource should be validated
			IEditorDescriptor defaultEditorDescriptor =
				PerlEditorPlugin
					.getDefault()
					.getWorkbench()
					.getEditorRegistry()
					.getDefaultEditor(resource.getFullPath().toString());

			if (defaultEditorDescriptor == null) {
				return false;
			}

			if (!defaultEditorDescriptor.getId().equals(Constants.PERL_EDITOR_ID)
				|| resource.getFileExtension().equals(Constants.EMB_PERL_FILE_EXTENSION)) {
				return false;
			}

			StringBuffer sourceCode = new StringBuffer();

			//	Get the file content
			char[] buf = new char[BUF_SIZE];
			File inputFile =
				new File(resource.getLocation().makeAbsolute().toString());
			BufferedReader in = new BufferedReader(new FileReader(inputFile));

			int read = 0;
			while ((read = in.read(buf)) > 0) {
				sourceCode.append(buf, 0, read);
			}
			in.close();

			validate(resource, sourceCode.toString());

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static void validate(IResource resource, String sourceCode) {
		Process proc = null;
		Map attributes = new HashMap(11);
		
		
		// Initilize Errors and Warnngs Hash;
		initializeErrorsAndWarnings();

		try {
			// Construct command line parameters
			List cmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(
					resource.getProject());
			cmdList.add(PERL_CMD_EXT);

			if (PerlEditorPlugin.getDefault().getWarningsPreference()) {
				cmdList.add("-w");
			}

			if (PerlEditorPlugin.getDefault().getTaintPreference()) {
				cmdList.add("-T");
			}

			String[] cmdParams =
				(String[]) cmdList.toArray(new String[cmdList.size()]);

			// Get working directory -- Fixes Bug: 736631
			String workingDir =
				resource
					.getLocation()
					.makeAbsolute()
					.removeLastSegments(1)
					.toString();

			/*
			 * Due to Java Bug #4763384 sleep for a very small amount of time
			 * immediately after starting the subprocess
			*/
			proc =
				Runtime.getRuntime().exec(
					cmdParams,
					null,
					new File(workingDir));
			Thread.sleep(1);

			proc.getInputStream().close();
			InputStream in = proc.getErrorStream();
			OutputStream out = proc.getOutputStream();
			//TODO which charset?
			Reader inr = new InputStreamReader(in);
			Writer outw = new OutputStreamWriter(out);

			StringReaderThread srt = new StringReaderThread();
			srt.read(inr);

			try {
				outw.write(sourceCode);
				outw.write(0x1a); //this should avoid problem with Win98
				outw.flush();
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
			String perlDiag = null;
			List lines = new ArrayList();
			int index;

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
				perlDiag = "";

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

					Object[] keys = errorMessagesHash.keySet().toArray();

					for (int entry = 0; entry < keys.length; entry++) {
						RE re = new RE((String) keys[entry]);

						String errorMsgOnly =
							index != -1
								? truncatedLine.substring(0, index)
								: truncatedLine;

						if (re.getAllMatches(errorMsgOnly).length > 0) {
							String value =
								(String) errorMessagesHash.get(keys[entry]);

							// (W, D & S) are warnings
							if (value.startsWith("(W")
								|| value.startsWith("(D")
								|| value.startsWith("(S")) {
								isWarning = true;
							}

							// Not used at the moment
							perlDiag = value;
							break;
						}

					}

					if (isWarning) {
						attributes.put(
							IMarker.SEVERITY,
							new Integer(IMarker.SEVERITY_WARNING));
					} else {
						attributes.put(
							IMarker.SEVERITY,
							new Integer(IMarker.SEVERITY_ERROR));
					}

					String diag = "";
//TODO add diagnostics
//					if (perlDiag.length() > 0) {
//						diag = "\n" + perlDiag;
//						RE re = new RE("\\n");
//						diag = re.substituteAll(diag, "\n     ");
////						re = new RE("^\\(.*?\\)");
////						diag = re.substituteAll(diag, "");
//						diag += "\n";
//					}

					attributes.put(IMarker.MESSAGE, line + diag);

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
							Document document = new Document(sourceCode);
							lineOffset =
								document.getLineOffset(
									Integer.parseInt(lineNr) - 1);
						} catch (Exception ex) {
							continue;
						}

						int endOfLine = sourceCode.indexOf("\n", lineOffset);
						String markerLine;

						if (endOfLine != -1) {
							markerLine =
								sourceCode.substring(lineOffset, endOfLine);
						} else {
							markerLine = sourceCode.substring(lineOffset);
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

					// Add markers
					AddEditorMarker ed = new AddEditorMarker();
					ed.addMarker(resource, attributes, IMarker.PROBLEM);

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	public static void parseTasks(IResource resource, String sourceCode) {
  	
		// TODO get the strings marking a todo from the configuration
		// at the moment, we´re assuming something like "# TODO text" 
		final String TODO_STRING = "# TODO ";
  	
		Map attributes = new HashMap(11);

		// delete the old markers
		try {
			resource.deleteMarkers(IMarker.TASK, true, 1);
		}
		catch (CoreException ce) {
			System.out.println("couldn´t delete the old task markers : ");
			ce.printStackTrace();
		}

		// get the source and search for all occurrences of "# TODO"
		Document document = new Document(sourceCode);
		try {																	
			int currentPos = -1;
			do {
				currentPos = document.search(currentPos+1, TODO_STRING, true, false, false);
				
				if (currentPos > -1) {
					System.out.println("found " + TODO_STRING + " at pos. " + currentPos + ". adding marker.");				
					
					AddEditorMarker ed = new AddEditorMarker();
					// the starting char is simple - 
					attributes.put(IMarker.CHAR_START, new Integer(currentPos));
					// the ending char is the first newline after the start position (assuming that each todo ends at the end of the line)
					int stopPos = document.search(currentPos, "\n", true, true, false);					
					attributes.put(IMarker.CHAR_END, new Integer(stopPos));
					
					// get the number of the line in which the current position is
					int lineCountToHere = document.getNumberOfLines(0, currentPos);
					attributes.put(IMarker.LINE_NUMBER, new Integer(lineCountToHere));
					
					// the text that should be displayed is everything between the end of the TODO marker and the end of the line
					int textStartPos = currentPos + TODO_STRING.length();				 	
					String todoText = document.get(textStartPos, stopPos-textStartPos);
					attributes.put(IMarker.MESSAGE, todoText);
					attributes.put(IMarker.TRANSIENT, Boolean.TRUE);																								
															
					ed.addMarker(resource, attributes, IMarker.TASK);					
				}        
			} while (currentPos > -1);
		}
		catch (BadLocationException ble1) {
			ble1.printStackTrace();			
		}  
	}	
	
}
