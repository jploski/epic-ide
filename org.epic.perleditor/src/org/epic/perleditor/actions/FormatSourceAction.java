package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import org.epic.perleditor.editors.util.SourceFormatter;
import org.epic.perleditor.editors.PerlEditor;

public class FormatSourceAction extends Action implements
		org.eclipse.ui.IEditorActionDelegate {
	PerlEditor editor = null;

	/**
	 * Constructs and updates the action.
	 */
	public FormatSourceAction() {
		super();
	}

	public void run() {
		if (editor == null) {
			return;
		}

		IDocument document = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());
		ISourceViewer viewer = editor.getViewer();
		int topIndex = viewer.getTextWidget().getTopIndex();
		int carretOffset = viewer.getTextWidget().getCaretOffset();
		String text = new SourceFormatter().doConversion(document.get());

		if (text != null) {
			document.set(text);
			viewer.getTextWidget().setTopIndex(topIndex);
			viewer.getTextWidget().setCaretOffset(carretOffset);
			viewer.getTextWidget().redraw();
			// Re-validate Syntax
			if (editor != null) {
				editor.revalidateSyntax(true);
			}
		}
	}

	public void setEditor(PerlEditor editor) {
		this.editor = editor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		if (targetEditor instanceof PerlEditor) {
			setEditor((PerlEditor) targetEditor);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		run();
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

}