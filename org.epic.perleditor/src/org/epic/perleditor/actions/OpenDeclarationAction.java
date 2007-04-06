package org.epic.perleditor.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Shell;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;

/**
 * Attempts to find and open declaration of a selected element or,
 * if no selection exists, of the element over whose invocation
 * the caret is located.
 * 
 * First, the selected text is interpreted as a subroutine name.
 * If the search fails, it is interpreted as a package name and
 * the search is repeated. If that one also fails, the user is
 * notified. 
 * 
 * @author LeO (original implementation)
 * @author jploski (complete rewrite)
 */
public class OpenDeclarationAction extends PerlEditorAction
{
    private final OpenSubDeclaration openSub;
    private final OpenPackageDeclaration openPackage;
    
    //~ Constructors

    public OpenDeclarationAction(PerlEditor editor)
    {
        super(editor);
        
        this.openSub = new OpenSubDeclaration(this);
        this.openPackage = new OpenPackageDeclaration(this);
    }
    
    //~ Methods

    /**
     * Runs the action using the given selection within the editor.
     */
    public void run(ITextSelection selection)
    {
        AbstractOpenDeclaration.Result res1 = openSub.run(selection);
        if (!res1.isFound())
        {
            AbstractOpenDeclaration.Result res2 = openPackage.run(selection);
            if (!res2.isFound()) reportFailure(res1);
        }
    }
    
    protected void doRun()
    {
        AbstractOpenDeclaration.Result res1 = openSub.run();
        if (!res1.isFound())
        {
            AbstractOpenDeclaration.Result res2 = openPackage.run();
            if (!res2.isFound()) reportFailure(res1);
        }
    }
    
    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.OPEN_DECLARATION;
    }
    
    private void messageBox(String title, String message)
    {
        Shell shell;
        shell = PerlEditorPlugin.getWorkbenchWindow().getShell();
        MessageDialog.openInformation(shell, title, message);
    }
    
    private void reportFailure(AbstractOpenDeclaration.Result result)
    {
        switch(result.statusCode)
        {
        case AbstractOpenDeclaration.Result.NOT_FOUND:
            messageBox(
                "Declaration not found",
                "Could not locate declaration for \"" + result.searchString + "\".\n" +
                "Check Perl Include Path in Project Properties.");
            break;
        case AbstractOpenDeclaration.Result.INVALID_SEARCH:
            messageBox(
                "Nothing was selected",
                "No valid name could be located within the selection scope.");
            break;
        case AbstractOpenDeclaration.Result.MODULE_NOT_FOUND:
            messageBox(
                "Module file not found",
                "Could not locate module file for package " + result.targetModule + "\n" +
                "Check Perl Include Path in Project Properties."
                );
            break;
        }
    }
}