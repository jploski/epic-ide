package org.epic.perleditor.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IFileEditorInput;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.PodChecker;

public class PodCheckerAction extends PerlEditorAction
{

    public PodCheckerAction(PerlEditor editor)
    {
        super(editor);
    }

    protected void doRun()
    {
        // TODO: check if editor is dirty before running
        IFile file = ((IFileEditorInput) getEditor().getEditorInput()).getFile();

        PodChecker.podchecker(file, getLog());
    }

    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.POD_CHECKER;
    }

}
