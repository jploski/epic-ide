package org.epic.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;

public class PerlExecutor {

	private TextEditor fTextEditor;

	public PerlExecutor(TextEditor textEditor) {
		super();
		this.fTextEditor = textEditor;
	}

	public String[] execute(String perlCode) {
		ArrayList al = new ArrayList();
		
		try {

			// Construct command line parameters
			List cmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(
					fTextEditor);
			
			String[] cmdParams =
				(String[]) cmdList.toArray(new String[cmdList.size()]);
			

			//Get working directory -- Fixes Bug: 736631
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
			Process proc =
				Runtime.getRuntime().exec(
					cmdParams,
					null,
					new File(workingDir));
			Thread.sleep(1);

			proc.getErrorStream().close();
			InputStream in = proc.getInputStream();
			OutputStream out = proc.getOutputStream();
			//TODO which charset?
			Writer outw = new OutputStreamWriter(out);

			try {
				outw.write(perlCode);
				outw.write(0x1a); //this should avoid problem with Win98
				outw.flush();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			out.close();

			String content = PerlExecutableUtilities.readStringFromStream(in);
			in.close();

			String line;
			StringTokenizer st = new StringTokenizer(content, "\n");
			while (st.hasMoreTokens()) {
				line = st.nextToken();
				if (line.indexOf("\r") != -1) {
					line = line.substring(0, line.indexOf("\r"));
				}

				al.add(line);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return (String[]) al.toArray(new String[0]);
	}
	
}
