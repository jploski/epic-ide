package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorMessages;

public class ToggleCommentAction extends Action implements
    org.eclipse.ui.IEditorActionDelegate {

  PerlEditor editor = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IEditorPart)
   */
  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    if (targetEditor instanceof PerlEditor) {
      editor = ((PerlEditor) targetEditor);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run() {
  	Action action;
  	 // If fTextEditor is not set, use the current editor
    if(editor == null) {
    	TextEditor textEditor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    	if(textEditor instanceof PerlEditor) {
    		editor = (PerlEditor) textEditor;
    	}
    }
  	
    if (editor != null) {
      Point selRange = editor.getViewer().getSelectedRange();
      IDocument myDoc = editor.getViewer().getDocument();
      System.out.println("Range: x=" + selRange.x + "; y=" + selRange.y);
      try {
        int line1 = myDoc.getLineOfOffset(selRange.x);
        int line2 = 0;
        if (selRange.y > 0) {
          line2 = myDoc.getLineOfOffset(selRange.x + selRange.y - 1);
        } else {
          line2 = myDoc.getLineOfOffset(selRange.x + selRange.y);
        }
        boolean noCommentFound = true;

        for (int i = myDoc.getLineOfOffset(selRange.x); i <= line2
            && noCommentFound; i++) {
          System.out.println("Line: " + myDoc.getLineOffset(i) + "; Länge="
              + myDoc.getLineLength(i) + "; 1. zwei Zeichen="
              + myDoc.getChar(myDoc.getLineOffset(i))
              + myDoc.getChar(myDoc.getLineOffset(i) + 1));
          if (myDoc.getChar(myDoc.getLineOffset(i)) != '#') {
            noCommentFound = false;
          }
        }

        if (noCommentFound) {
          action = new TextOperationAction(PerlEditorMessages
              .getResourceBundle(), "Uncomment.", editor,
              ITextOperationTarget.STRIP_PREFIX);
        } else {
          action = new TextOperationAction(PerlEditorMessages
              .getResourceBundle(), "Comment.", editor,
              ITextOperationTarget.PREFIX);
        }
        action.run();
      } catch (BadLocationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
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