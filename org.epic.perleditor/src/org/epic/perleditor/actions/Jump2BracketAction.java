package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.editors.PerlEditor;

public class Jump2BracketAction extends Action implements
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
	public void run() {
		// If fTextEditor is not set, use the current editor
	    if(editor == null) {
	    	TextEditor textEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	    	if(textEditor instanceof PerlEditor) {
	    		editor = (PerlEditor) textEditor;
	    	}
	    }
		
		if(editor != null) {
      ISourceViewer viewer = editor.getViewer();

		  int matchPosition = ((PerlEditor) editor).findNextOccurance();
		  if (matchPosition >= 0) {
		    viewer.getTextWidget().setCaretOffset(matchPosition +1);
		    
		    int newLine = viewer.getTextWidget().getLineAtOffset(matchPosition +1 );
		    if (viewer.getBottomIndex() <= newLine) {
		      viewer.setTopIndex(viewer.getTopIndex() + (newLine - viewer.getBottomIndex()));
		    }
		    ((PerlEditor) editor).newCurosorPos();
		  }
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		run();
	}
	
}