/*
 * Created on Jun 7, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.perleditor.popupmenus.refactor;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;
import org.epic.perleditor.editors.util.SourceFormatter;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;

/**
 * @author luelljoc
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ExtractSubroutineAction extends Action implements
		org.eclipse.ui.IEditorActionDelegate, IWorkbenchWindowActionDelegate {

	ITextEditor fTextEditor;

	/**
	 * @see Action#run()
	 */
	public void run() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		fTextEditor = (TextEditor) targetEditor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {

		String subroutineName;

		Shell shell = PerlEditorPlugin.getWorkbenchWindow().getShell();

		TextSelection selection = ((TextSelection) fTextEditor
				.getSelectionProvider().getSelection());

		if (selection.getText().length() == 0) {
			MessageDialog.openInformation(shell, "No selection",
					"Nothing has been selected.");
			return;
		}

		InputDialog inputDialog = new InputDialog(shell, "Subroutine Name",
				"Name of Subroutine", "", null);

		int returnCode = inputDialog.open();

		if (returnCode == Window.OK) {
			subroutineName = inputDialog.getValue();
			String delimiter = "<CALL-CODE>";
			String res = getSubroutineCallAndCode(selection.getText(),
					subroutineName, delimiter);

			if (res.length() > 0) {
				String[] result = res.split(delimiter);
				
				// Delete trailing \n
				if(result[0].endsWith("\n")) {
					result[0] = result[0].substring(0, result[0].lastIndexOf("\n"));
				}
				
				IDocument doc = fTextEditor.getDocumentProvider().getDocument(
						fTextEditor.getEditorInput());
				try {
					
					// Repace the selection with the subroutine call
					doc.replace(selection.getOffset(), selection.getLength(), result[0]);
					
					int offset = -1;
					
					FindReplaceDocumentAdapter docFind = new FindReplaceDocumentAdapter(doc);
					

					IRegion regionEnd = docFind.find(selection.getOffset(), "^__END__", true, true, false, true);
					offset = regionEnd!=null ? regionEnd.getOffset() : doc.getLength();						
					
					// Tidy Perl code
					SourceFormatter formatter = new SourceFormatter();
					String subTidy = formatter.doConversion(result[1]);
					
					// If something goes wrong, use original code
					if(subTidy.length() == 0) {
						subTidy = result[1];
					}
					
					String lineSep = getLineSeparator(doc.get());
					
					// Insert the subroutine code
					doc.replace(offset, 0, lineSep + subTidy);
					
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			} else {
				MessageDialog.openInformation(shell, "Error",
				"Subroutine could not be generated.");
			}

		} else {
			return;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

	private String getSubroutineCallAndCode(String codeSnippet, String subName,
			String delimiter) {

		codeSnippet = codeSnippet.replaceAll("'", "\\\\'");
		subName = subName.replaceAll("'", "\\\\'");

		String perlCode = "use Devel::Refactor;\n\n"
				+ "my $refactory = Devel::Refactor->new('" + subName
				+ "', '" + codeSnippet + "');\n"
				+ "print $refactory->get_sub_call() . \"" + delimiter
				+ "\" . $refactory->get_new_code();";

		String content = null;

		try {

			//			Construct command line parameters
			List cmdList = PerlExecutableUtilities
					.getPerlExecutableCommandLine((TextEditor) fTextEditor);

			String[] cmdParams = (String[]) cmdList.toArray(new String[cmdList
					.size()]);

			URL installURL = PerlEditorPlugin.getDefault().getDescriptor()
					.getInstallURL();
			URL perlModulesURL = Platform.resolve(new URL(installURL,
					"perlutils/modules"));

			/*
			 * Due to Java Bug #4763384 sleep for a very small amount of time
			 * immediately after starting the subprocess
			 */
			Process proc = Runtime.getRuntime().exec(cmdParams, null,
					new File(perlModulesURL.getPath()));
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
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			out.close();

			content = PerlExecutableUtilities.readStringFromStream(in);
			in.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return content;
	}
	
	private String getLineSeparator(String text) {
		String lineSep = System.getProperty("line.separator");
				
		if (text.indexOf(System.getProperty("line.separator")) == -1) {
			if (text.indexOf("\r\n") > 0) {
				lineSep = "\r\n";
			} else if (text.indexOf("\n") > 0) {
				lineSep = "\n";
			} else if (text.indexOf("\r") > 0) {
				lineSep = "\r";
			}
		}
		
		return lineSep;
	
	}

}