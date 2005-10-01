package org.epic.perleditor.actions;

import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;

public class Jump2BracketAction extends PerlEditorAction
{    
    public Jump2BracketAction()
    {
    }
    
    public Jump2BracketAction(PerlEditor editor)
    {
        super(editor);
    }

    public void run()
    {
        getEditor().jumpToMatchingBracket();
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.MATCHING_BRACKET;
    }
}