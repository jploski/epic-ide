/*
 * This file is a duplicate of org.epic.perleditor.preferences.PerlEditorPreferencePage
 * to allow the use of the Colorer plugin
 * Changes are marked with todo tags!
 */
package org.epic.ext.unsupported.colorer;


import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.swt.SWT;
import org.epic.perleditor.editors.PerlEditorMessages;



/**
 * Contributes interesting Java actions to the desktop's Edit menu and the toolbar.
 */
public class PerlActionContributor extends TextEditorActionContributor {

	protected FormatSourceAction formatSourceAction;
	protected RetargetTextEditorAction fContentAssist;
	protected RetargetTextEditorAction fComment;
	protected RetargetTextEditorAction fUncomment;

	/**
	 * Default constructor.
	 */
	public PerlActionContributor() {
		super();

        // Somehow the key bindings don't work in RC2
        formatSourceAction = new FormatSourceAction("&Format\tCtrl+Shift+F");
        formatSourceAction.setAccelerator(SWT.CTRL | SWT.SHIFT | 'F');
        
		fContentAssist = new RetargetTextEditorAction(PerlEditorMessages.getResourceBundle(), "ContentAssistProposal.");
		fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		
		fComment = new RetargetTextEditorAction(PerlEditorMessages.getResourceBundle(), "Comment.");
		fComment.setActionDefinitionId(IJavaEditorActionDefinitionIds.COMMENT);

		fUncomment = new RetargetTextEditorAction(PerlEditorMessages.getResourceBundle(), "Uncomment.");
		fComment.setActionDefinitionId(IJavaEditorActionDefinitionIds.UNCOMMENT);
	
	}
	
	/*
	 * @see IEditorActionBarContributor#init(IActionBars)
	 */
	public void init(IActionBars bars) {
		super.init(bars);
		IMenuManager menuManager= bars.getMenuManager();
		IMenuManager editMenu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		IMenuManager sourceMenu = new MenuManager("&Source");
		menuManager.insertAfter(editMenu.getId(), sourceMenu);
		sourceMenu.add(formatSourceAction);
		sourceMenu.add(fContentAssist);
		sourceMenu.add(fComment);
		sourceMenu.add(fUncomment);
		
	}
	
	private void doSetActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		PerlEditor editor= null;
		
		if (part instanceof PerlEditor)
			editor = (PerlEditor) part;
			
	    formatSourceAction.setEditor(editor);
		fContentAssist.setAction(getAction(editor, "ContentAssist"));
		fComment.setAction(getAction(editor, "Comment"));
		fUncomment.setAction(getAction(editor, "Uncomment"));

	}
	
	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		doSetActiveEditor(part);
	}
	
	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {
		doSetActiveEditor(null);
		super.dispose();
	}
}
