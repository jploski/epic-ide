package org.epic.perleditor.preferences;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author ptraeder
 */
public class TaskTagsListEditor extends ListEditor {  
	
	public TaskTagsListEditor() {
		super();
	}
	
	public TaskTagsListEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#getPreferenceStore()
	 */
	public IPreferenceStore getPreferenceStore() {
		return PerlEditorPlugin.getDefault().getPreferenceStore();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
	 */
	protected void doLoadDefault() {
		super.doLoadDefault();
		TaskTagPreferences.initializeDefaults(getPreferenceStore());
	}		
	
	protected String getNewInputObject() {
		String result = null;
		
		// open an input dialog so that the user can enter a new task tag
		Shell shell =
			PerlEditorPlugin
			.getWorkbenchWindow().getShell();    
		InputDialog inputDialog = new InputDialog(shell, "New Task Tag...", "Enter new Task Tag:", "", null);
		int returnCode = inputDialog.open();
		
		if (returnCode == Window.OK) {
			result = inputDialog.getValue();
		}
		
		return result;
	}  
	
	protected String createList(String[] items) {
		return TaskTagPreferences.createList(items);
	}
	
	protected String[] parseString(String stringList) {
		return TaskTagPreferences.parseStringList(stringList);
	}
	
}
