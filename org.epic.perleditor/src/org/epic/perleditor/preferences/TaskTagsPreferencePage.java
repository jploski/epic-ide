package org.epic.perleditor.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;

/**
 * @author ptraeder
 */
public class TaskTagsPreferencePage
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage, ITaskTagConstants {
	
	public TaskTagsPreferencePage() {
		super(GRID);
		
		setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());
		setDescription("Define which strings mark task entries in your perl code:");
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), getPreferenceHelpContextID());    
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IEditorPart activeEditor = PlatformUI.getWorkbench()
		.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if(activeEditor instanceof PerlEditor) {
			((PerlEditor) activeEditor).refreshTaskView();
		}		
		return super.performOk();
	}
	
	
	
	protected String getPreferenceHelpContextID() {
		return "org.epic.perleditor.preferencesTaskTags_context";
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	
	public void createFieldEditors() {       
		addField(new TaskTagsListEditor(ITaskTagConstants.ID_TASK_TAGS, "Task Tags:", getFieldEditorParent()));
		addField(new BooleanFieldEditor(ID_IGNORE_CASE, "Ignore Case", getFieldEditorParent()));		
		addField(new BooleanFieldEditor(ID_WHITESPACE, "Allow leading whitespace", getFieldEditorParent()));
	}
	
	public void init(IWorkbench workbench) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	
}