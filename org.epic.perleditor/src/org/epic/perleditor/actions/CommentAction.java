package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorMessages;

public class CommentAction extends Action implements
		org.eclipse.ui.IEditorActionDelegate {

	PerlEditor editor = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		if (targetEditor instanceof PerlEditor) {
			editor = ((PerlEditor) targetEditor);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if(editor != null) {
			action =
				new TextOperationAction(
					PerlEditorMessages.getResourceBundle(),
					"Comment.",
					editor,
					ITextOperationTarget.PREFIX);
			
			action.run();
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}
	
}