package org.epic.perleditor.popupmenus;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;

import org.epic.perleditor.editors.PerlImages;
import org.epic.perleditor.views.ExplainErrorsView;
import org.epic.perleditor.views.PerlDocView;

import java.util.List;
import java.io.*;

public class PerlDocAction
	implements org.eclipse.ui.IEditorActionDelegate {

	private ITextEditor fTextEditor;
	private String selection;
	private String content;
	private String title;
	
	private Shell shell;

	public PerlDocAction() {
		shell =
			PerlEditorPlugin
				.getWorkbenchWindow().getShell();
				
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		fTextEditor = (ITextEditor) targetEditor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		selection =
			((TextSelection) fTextEditor.getSelectionProvider().getSelection())
				.getText();

		if (selection.length() == 0) {
			InputDialog inputDialog =
				new InputDialog(
					shell,
					PopupMessages.getString("PerlDoc.search.title") + " (" + action.getText() + ")", 
					PopupMessages.getString("PerlDoc.search.message"),
					"",
					null);
			int returnCode = inputDialog.open();

			if (returnCode == Window.OK) {
				selection = inputDialog.getValue();
			} else {
				return;
			}
		}

		String option = null;
		String actionId = action.getId();

		if (actionId.endsWith("Module")) {
			option = "-t";
		} else if (actionId.endsWith("ModuleSource")) {
			option = "-m";
		} else if (actionId.endsWith("BuiltinFunction")) {
			option = "-t -f";
		} else if (actionId.endsWith("Faq")) {
			option = "-t -q";
		}

		title = "Perldoc for " + selection + " (" + action.getText() + ")";

		content = getPerlDoc(option, selection);

		if (content.length() == 0) {
			MessageDialog.openInformation(
				shell,
				PopupMessages.getString("NoDocumentation.title"),
				PopupMessages.getString("NoDocumentation.message"));
		}
		else {
			PerlDocView view = null;
			IWorkbenchPage activePage =
				PerlEditorPlugin
					.getWorkbenchWindow()
					.getActivePage();
			try
			{
			view = (PerlDocView)	activePage.showView(
					"org.epic.perleditor.views.PerlDocView");
			} catch (PartInitException e)
			{
				e.printStackTrace();
			}
			view.setText(content);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {

	}

	private String getPerlDoc(String option, String selection) {

		String perlCode =
			"use Env qw(@PERL5LIB);\n\n"
				+ "push(@PERL5LIB, @INC);\n"
				+ "exec('perldoc "
				+ option
				+ " \""
				+ selection
				+ "\"');";
		String tmpFileName = null;
		String content = "";

		try {

			//			Construct command line parameters
			List cmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(
					(TextEditor) fTextEditor);

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
			Process proc = Runtime.getRuntime().exec(cmdParams, null, new File(workingDir));
			Thread.sleep(1);

            proc.getErrorStream().close();
			InputStream in = proc.getInputStream();
			OutputStream out = proc.getOutputStream();
            //TODO which charset?
            Writer outw = new OutputStreamWriter(out);

			try {
                outw.write(perlCode);
                outw.write(0x1a);  //this should avoid problem with Win98
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

}