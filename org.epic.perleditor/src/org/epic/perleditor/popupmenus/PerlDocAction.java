package org.epic.perleditor.popupmenus;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.actions.PerlEditorAction;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.views.PerlDocView;

public class PerlDocAction extends PerlEditorAction
{
	private String selection;
	private Shell shell;

    public PerlDocAction()
    {
    }
    
	public PerlDocAction(PerlEditor editor)
    {
        super(editor);
		shell = PerlEditorPlugin.getWorkbenchWindow().getShell();
	}
	
	public void run()
    {
        ITextEditor editor = getEditor();
        
		selection = ((TextSelection) editor.getSelectionProvider()
            .getSelection()).getText();

		if (selection.length() == 0)
        {
			InputDialog inputDialog = new InputDialog(
                shell,
                PopupMessages.getString("PerlDoc.search.title"),
                PopupMessages.getString("PerlDoc.search.message"),
                "",
                null);

			int returnCode = inputDialog.open();

			if (returnCode == Window.OK) selection = inputDialog.getValue();
            else return;
		}

		PerlDocView view = null;
		IWorkbenchPage activePage =
            PerlEditorPlugin.getWorkbenchWindow().getActivePage();

		try
        {
            view = (PerlDocView) activePage.showView(
                "org.epic.perleditor.views.PerlDocView");
		}
        catch (PartInitException e)
        {
			e.printStackTrace();
		}
		view.search(selection, editor);
	}

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.PERL_DOC;
    }
}