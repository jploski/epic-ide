package org.epic.perleditor.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.epic.core.preferences.LabelFieldEditor;
import org.epic.core.preferences.SpacerFieldEditor;
import org.epic.perleditor.PerlEditorPlugin;


/**
 * @author luelljoc
 *
 * Source Formatter preference page
 */
public class SourceFormatterPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/**
	 * SourceFormatterPreferencePage constructor
	 */
	public SourceFormatterPreferencePage() {
		super(GRID);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		setDescription("All changes will take effect next time the formatter is run.\n");
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
		return "org.epic.doc.preferencesSourceFormatter_context";
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	public void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		
		addField(
			new BooleanFieldEditor(
				SourceFormatterPreferences.CUDDLED_ELSE,
				"Cuddle else",
				composite));
		addField(
			new BooleanFieldEditor(
				SourceFormatterPreferences.BRACES_LEFT,
				"Opening brace on new line",
				composite));
		addField(
				new BooleanFieldEditor(
				SourceFormatterPreferences.LINE_UP_WITH_PARENTHESES,
				"Line up with parentheses",
				composite));
		addField(
						new BooleanFieldEditor(
						SourceFormatterPreferences.SWALLOW_OPTIONAL_BLANK_LINES,
						"Swallow optional blank lines",
						composite));
 

//        addField(new SpacerFieldEditor(composite));
//		addField(new LabelFieldEditor("Container tightness:", composite));
//		addField(new LabelFieldEditor("(values 0-2)", composite));
//
//		IntegerFieldEditor ctBraces = new IntegerFieldEditor(
//				SourceFormatterPreferences.CONTAINER_TIGHTNESS_BRACES,
//				"Braces",
//				composite);
//		ctBraces.setValidRange(0, 2);	
//		addField(ctBraces);
//		
//		IntegerFieldEditor ctParentheses = new IntegerFieldEditor(
//				SourceFormatterPreferences.CONTAINER_TIGHTNESS_PARENTHESES,
//				"Parentheses",
//				composite);
//		ctParentheses.setValidRange(0, 2);	
//		addField(ctParentheses);
//		
//		IntegerFieldEditor ctSquareBraces = new IntegerFieldEditor(
//				SourceFormatterPreferences.CONTAINER_TIGHTNESS_SQUARE_BRACKETS,
//				"Square brackets",
//				composite);
//		ctSquareBraces.setValidRange(0, 2);	
//		addField(ctSquareBraces);
		
		addField(new SpacerFieldEditor(composite));
		addField(new LabelFieldEditor("Additional PerlTidy options (separated by space)", composite));
		addField(new StringFieldEditor(
						SourceFormatterPreferences.PERLTIDY_OPTIONS,
						"Options",
						composite));
					
		addField(new SpacerFieldEditor(composite));
				addField(new LabelFieldEditor("HTML Export options (separated by space)", composite));
				addField(new StringFieldEditor(
								SourceFormatterPreferences.HTML_EXPORT_OPTIONS,
								"Options",
								composite));

	}

}
