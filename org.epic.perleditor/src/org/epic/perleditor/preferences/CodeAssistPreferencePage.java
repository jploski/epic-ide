package org.epic.perleditor.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.epic.perleditor.PerlEditorPlugin;


/**
 * @author luelljoc
 *
 * Source Formatter preference page
 */
public class CodeAssistPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/**
	 * SourceFormatterPreferencePage constructor
	 */
	public CodeAssistPreferencePage() {
		super(GRID);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		setDescription("All changes will take effect on next code assist action.\n");
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */
	public IPreferenceStore doGetPreferenceStore() {
		return PerlEditorPlugin.getDefault().getPreferenceStore();
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), getPreferenceHelpContextID());
	}
	
	protected String getPreferenceHelpContextID() {
		return "org.epic.perleditor.preferencesCodeAssist_context";
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	public void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		
		addField(new StringFieldEditor(
						CodeAssistPreferences.AUTO_ACTIVATION_CHARS,
						"Auto activation characters",
						composite));
						
		addField(
					new BooleanFieldEditor(
						CodeAssistPreferences.INSPECT_VARIABLES,
						"Inspect variables",
						composite));
		

	}

}
