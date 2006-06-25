package org.epic.perleditor.editors;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.TextEditorActionContributor;

import org.epic.perleditor.actions.ToggleMarkOccurrencesAction;


/**
 * Associates global action handlers contributed via actionSets in the plug-in's manifest with their
 * PerlEditorAction counterparts in the currently active PerlEditor.
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
        if (! (part instanceof PerlEditor)) { return; }

        PerlEditor editor = (PerlEditor) part;

        // Bind actions contributed by the active editor
        String[] perlEditorActionIds = PerlEditorActionIds.getEditorActions();
        for (int i = 0; i < perlEditorActionIds.length; i++)
        {
            getActionBars().setGlobalActionHandler(perlEditorActionIds[i],
                getAction(editor, perlEditorActionIds[i]));
        }

        getActionBars().updateActionBars();
    }

    public void init(IActionBars bars, IWorkbenchPage page)
    {
        super.init(bars, page);

        // bind global actions that effect all open editor instances
        bars.setGlobalActionHandler(PerlEditorActionIds.TOGGLE_MARK_OCCURRENCES,
                toggleMarkOccurrencesAction);
    }
}
