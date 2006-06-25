package org.epic.perleditor.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.core.util.StatusFactory;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.popupmenus.PopupMessages;
import org.epic.perleditor.views.PerlDocView;


public class PerlDocAction extends PerlEditorAction
{
    //~ Constructors

    public PerlDocAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        ITextEditor editor = getEditor();

        String selection = ((TextSelection) editor.getSelectionProvider().getSelection()).getText();
        Shell shell = PerlEditorPlugin.getWorkbenchWindow().getShell();

        if (selection.length() == 0)
        {
            InputDialog inputDialog =
                new InputDialog(shell, PopupMessages.getString("PerlDoc.search.title"),
                    PopupMessages.getString("PerlDoc.search.message"), "", null);

            int returnCode = inputDialog.open();

            if (returnCode == Window.OK)
            {
                selection = inputDialog.getValue();
            }
            else
            {
                return;
            }
        }

        PerlDocView view = null;
        IWorkbenchPage activePage = PerlEditorPlugin.getWorkbenchWindow().getActivePage();

        try
        {
            view = (PerlDocView) activePage.showView("org.epic.perleditor.views.PerlDocView");
            view.search(selection, editor);
        }
        catch (PartInitException e)
        {
            PerlEditorPlugin.getDefault().getLog().log(
                StatusFactory.createError(PerlEditorPlugin.getPluginId(), e.getMessage(), e));
        }
        catch (CoreException e)
        {
            PerlEditorPlugin.getDefault().getLog().log(
                StatusFactory.createError(PerlEditorPlugin.getPluginId(), e.getMessage(), e));
        }
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.PERL_DOC;
    }
}
