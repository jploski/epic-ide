package org.epic.perleditor.actions;

import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;


public class ValidateSourceAction extends PerlEditorAction
{
    //~ Constructors

    public ValidateSourceAction(PerlEditor editor)
    {
        super(editor);

    }

    //~ Methods

    protected void doRun()
    {
        getEditor().revalidateSyntax();
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.VALIDATE_SYNTAX;
    }
}
