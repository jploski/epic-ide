package org.epic.perleditor.editors;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Associates global action handlers contributed via actionSets
 * in the plug-in's manifest with their PerlEditorAction counterparts
 * in the currently active PerlEditor. 
 */
public class PerlActionContributor extends TextEditorActionContributor
{
	public PerlActionContributor()
    {
	}

	public void setActiveEditor(IEditorPart part)
    {
		super.setActiveEditor(part);
        if (!(part instanceof PerlEditor)) return;
        
        PerlEditor editor = (PerlEditor) part;
        
        setGlobalActionHandler(editor, PerlEditorActionIds.CONTENT_ASSIST);
        setGlobalActionHandler(editor, PerlEditorActionIds.HTML_EXPORT);
        setGlobalActionHandler(editor, PerlEditorActionIds.VALIDATE_SYNTAX);
        setGlobalActionHandler(editor, PerlEditorActionIds.FORMAT_SOURCE);
        setGlobalActionHandler(editor, PerlEditorActionIds.TOGGLE_COMMENT);
        setGlobalActionHandler(editor, PerlEditorActionIds.OPEN_SUB);
        setGlobalActionHandler(editor, PerlEditorActionIds.PERL_DOC);
        setGlobalActionHandler(editor, PerlEditorActionIds.MATCHING_BRACKET);                           

        getActionBars().updateActionBars();
	}
    
    private void setGlobalActionHandler(ITextEditor perlEditor, String perlActionID)
    {
        getActionBars().setGlobalActionHandler(
            perlActionID, getAction(perlEditor, perlActionID));
    }
}
