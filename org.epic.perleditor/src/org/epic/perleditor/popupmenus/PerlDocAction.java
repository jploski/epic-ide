package org.epic.perleditor.popupmenus;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.views.PerlDocView;

public class PerlDocAction extends Action implements org.eclipse.ui.IEditorActionDelegate,
		org.eclipse.ui.IWorkbenchWindowActionDelegate {

	private ITextEditor fTextEditor;

	private String selection;

	private String content;

	private String title;

	private Shell shell;

	public PerlDocAction() {
		shell = PerlEditorPlugin.getWorkbenchWindow().getShell();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		fTextEditor = (ITextEditor) targetEditor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		run();
	}
	
	public void run() {
		
		// If fTextEditor is not set, use the current editor
		if(fTextEditor == null) {
		    fTextEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		}
		
		selection = ((TextSelection) fTextEditor.getSelectionProvider()
				.getSelection()).getText();

		if (selection.length() == 0) {
			InputDialog inputDialog = new InputDialog(shell, PopupMessages
					.getString("PerlDoc.search.title"), PopupMessages
					.getString("PerlDoc.search.message"), "", null);
			int returnCode = inputDialog.open();

			if (returnCode == Window.OK) {
				selection = inputDialog.getValue();
			} else {
				return;
			}
		}

		PerlDocView view = null;
		IWorkbenchPage activePage = PerlEditorPlugin.getWorkbenchWindow()
				.getActivePage();
		try {
			view = (PerlDocView) activePage
					.showView("org.epic.perleditor.views.PerlDocView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		view.search(selection, fTextEditor);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub
		fTextEditor = (TextEditor) window.getActivePage().getActiveEditor();
	}
}