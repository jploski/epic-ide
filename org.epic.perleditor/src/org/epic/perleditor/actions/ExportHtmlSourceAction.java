package org.epic.perleditor.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceFormatter;
import org.epic.perleditor.preferences.SourceFormatterPreferences;


public class ExportHtmlSourceAction extends PerlEditorAction
{
    //~ Static fields/initializers

    private static String lastSelectedDir;

    //~ Constructors

    public ExportHtmlSourceAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        PerlEditor editor = getEditor();
        IFileEditorInput editorInput = (IFileEditorInput) editor.getEditorInput();
        String filePath = editorInput.getFile().getLocation().makeAbsolute().toString();

        // Display directory dialog
        DirectoryDialog directoryDialog =
            new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                SWT.SAVE);
        directoryDialog.setText("Select Output Directory");
        directoryDialog.setMessage("HTML Export...");

        directoryDialog.setFilterPath(lastSelectedDir);

        String outputDir = directoryDialog.open();

        if (outputDir != null)
        {
            lastSelectedDir = outputDir;

            // Export options
            List cmdList = new ArrayList();

            cmdList.add("-html");
            cmdList.add("-opath");
            cmdList.add(outputDir);

            // Add additional options
            IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();
            StringTokenizer st =
                new StringTokenizer(store.getString(
                        SourceFormatterPreferences.HTML_EXPORT_OPTIONS));
            while (st.hasMoreTokens())
            {
                cmdList.add(st.nextToken());
            }

            // last thing has to be the input file name
            cmdList.add(filePath);

            SourceFormatter.format(editor.getViewer().getDocument().get(), cmdList, getLog());
        }
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.HTML_EXPORT;
    }
}
