package org.epic.perleditor.actions;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceFormatter;
import org.epic.perleditor.editors.util.SourceRefactor;


/**
 * Extracts a subroutine from selected editor text
 *
 * @author luelljoc
 */
public class ExtractSubroutineAction extends PerlEditorAction
{
    //~ Constructors

    public ExtractSubroutineAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        Shell shell = PerlEditorPlugin.getWorkbenchWindow().getShell();

        TextSelection selection =
            ((TextSelection) getEditor().getSelectionProvider().getSelection());

        if (selection.getText().length() == 0)
        {
            MessageDialog.openInformation(shell, "No selection", "Nothing has been selected.");
            return;
        }

        InputDialog inputDialog =
            new InputDialog(shell, "Subroutine Name", "Name of Subroutine", "", null);
        int returnCode = inputDialog.open();

        // return unless the ok button was pressed
        if (returnCode != Window.OK) { return; }

        String[] result = SourceRefactor.extractMethod(inputDialog.getValue(), selection.getText(),
                getLog());

        if (result.length == 0)
        {
            MessageDialog.openInformation(shell, "Error", "Subroutine could not be generated.");
            return;
        }

        // Delete trailing \n
        if (result[0].endsWith("\n"))
        {
            result[0] = result[0].substring(0, result[0].lastIndexOf("\n"));
        }

        IDocument doc = getEditor().getDocumentProvider().getDocument(getEditor().getEditorInput());
        try
        {
            // Repace the selection with the subroutine call
            doc.replace(selection.getOffset(), selection.getLength(), result[0]);

            int offset = -1;

            FindReplaceDocumentAdapter docFind = new FindReplaceDocumentAdapter(doc);

            IRegion regionEnd =
                docFind.find(selection.getOffset(), "^__END__", true, true, false, true);
            offset = (regionEnd != null) ? regionEnd.getOffset() : doc.getLength();

            String lineSep = getLineSeparator(doc.get());

            // format and insert the new subroutine code
            doc.replace(offset, 0, lineSep + SourceFormatter.format(result[1], getLog()));
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.EXTRACT_SUBROUTINE;
    }

    private String getLineSeparator(String text)
    {
        String lineSep = System.getProperty("line.separator");

        if (text.indexOf(System.getProperty("line.separator")) == -1)
        {
            if (text.indexOf("\r\n") > 0)
            {
                lineSep = "\r\n";
            }
            else if (text.indexOf("\n") > 0)
            {
                lineSep = "\n";
            }
            else if (text.indexOf("\r") > 0)
            {
                lineSep = "\r";
            }
        }

        return lineSep;

    }

}
