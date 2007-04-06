package org.epic.perleditor.actions;

import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;


public class Jump2BracketAction extends PerlEditorAction
{
    //~ Constructors

    public Jump2BracketAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        getEditor().jumpToMatchingBracket();
    }

    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.MATCHING_BRACKET;
    }
}
