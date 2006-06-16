package org.epic.perleditor.editors;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.perleditor.actions.ToggleMarkOccurrencesAction;


/**
 * Associates global action handlers contributed via actionSets
 * in the plug-in's manifest with their PerlEditorAction counterparts
 * in the currently active PerlEditor. 
 */
public class PerlActionContributor extends TextEditorActionContributor
{
    //~ Instance fields

    private ToggleMarkOccurrencesAction toggleMarkOccurrencesAction;

    //~ Constructors

    public PerlActionContributor()
    {
        toggleMarkOccurrencesAction = new ToggleMarkOccurrencesAction();
    }

    //~ Methods
    
    public void dispose()
    {
        super.dispose();
        toggleMarkOccurrencesAction.dispose();
    }

    public void setActiveEditor(IEditorPart part)
    {
        super.setActiveEditor(part);
        if (!(part instanceof PerlEditor)) return;
        
        // Bind actions owned by the active editor

        PerlEditor editor = (PerlEditor) part;        

        bindEditorAction(editor, PerlEditorActionIds.CONTENT_ASSIST);
        bindEditorAction(editor, PerlEditorActionIds.HTML_EXPORT);
        bindEditorAction(editor, PerlEditorActionIds.VALIDATE_SYNTAX);
        bindEditorAction(editor, PerlEditorActionIds.FORMAT_SOURCE);
        bindEditorAction(editor, PerlEditorActionIds.TOGGLE_COMMENT);
        bindEditorAction(editor, PerlEditorActionIds.OPEN_SUB);
        bindEditorAction(editor, PerlEditorActionIds.PERL_DOC);
        bindEditorAction(editor, PerlEditorActionIds.MATCHING_BRACKET);
        bindEditorAction(editor, PerlEditorActionIds.TOGGLE_MARK_OCCURRENCES);
        
        // Bind actions owned by the PerlActionContributor

        getActionBars().setGlobalActionHandler(
            PerlEditorActionIds.TOGGLE_MARK_OCCURRENCES,
            toggleMarkOccurrencesAction);
        
        getActionBars().updateActionBars();
    }

    private void bindEditorAction(ITextEditor perlEditor, String perlActionID)
    {
        getActionBars().setGlobalActionHandler(
        	perlActionID, getAction(perlEditor, perlActionID));
    }
}
