package org.epic.perleditor.actions;

import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;

public class ValidateSourceAction extends PerlEditorAction
{
    public ValidateSourceAction()
    {
    }
    
	public ValidateSourceAction(PerlEditor editor)
    {
        super(editor);
	}

	public void run()
    {
		getEditor().revalidateSyntax();
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.VALIDATE_SYNTAX;
    }
}