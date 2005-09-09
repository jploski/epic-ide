package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.editors.PerlEditor;

public class Jump2BracketAction extends Action
    implements org.eclipse.ui.IEditorActionDelegate
{
    PerlEditor editor = null;

    public void setActiveEditor(IAction action, IEditorPart targetEditor)
    {
        if (targetEditor instanceof PerlEditor)
        {
            editor = ((PerlEditor) targetEditor);
        }
    }

    public void run()
    {
        // If fTextEditor is not set, use the current editor
        if (editor == null)
        {
            TextEditor textEditor = (TextEditor) PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (textEditor instanceof PerlEditor)
            {
                editor = (PerlEditor) textEditor;
            }
        }

        if (editor != null) ((PerlEditor) editor).jumpToMatchingBracket();
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
    }

    public void run(IAction action)
    {
        run();
    }
}