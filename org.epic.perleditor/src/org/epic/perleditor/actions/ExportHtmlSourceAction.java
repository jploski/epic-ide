package org.epic.perleditor.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.SourceFormatter;
import org.epic.perleditor.preferences.SourceFormatterPreferences;

public class ExportHtmlSourceAction extends Action implements org.eclipse.ui.IWorkbenchWindowActionDelegate {

	static private String lastSelectedDir = null;
	/**
	 * Constructs and updates the action.
	 */
	public ExportHtmlSourceAction() {
		super();
	}
	
	public void run(IAction action) {
		run();
	}

	public void run() {
		String filePath =
			((IFileEditorInput) PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage()
				.getActiveEditor()
				.getEditorInput())
				.getFile()
				.getLocation()
				.makeAbsolute()
				.toString();
		
				
		// Display directory dialog
		DirectoryDialog directoryDialog = new DirectoryDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
		directoryDialog.setText("Select Output Directory");
		directoryDialog.setMessage("HTML Export...");
		
		directoryDialog.setFilterPath(lastSelectedDir);
		
		String outputDir = directoryDialog.open();
		
		
		if(outputDir != null) {
			lastSelectedDir = outputDir;
			
			// Export options
			List cmdList = new ArrayList();
			
			cmdList.add("-html");
			cmdList.add("-opath");
			cmdList.add(outputDir);
			
			// Add additional options
			IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();
			StringTokenizer st = new StringTokenizer(store.getString(SourceFormatterPreferences.HTML_EXPORT_OPTIONS));
			while (st.hasMoreTokens()) {
				cmdList.add(st.nextToken());
			}
			
			// last thing has to be the input file name
			cmdList.add(filePath);

			new SourceFormatter().doConversion(null, cmdList);

		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		
	}

	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		
	}

}
