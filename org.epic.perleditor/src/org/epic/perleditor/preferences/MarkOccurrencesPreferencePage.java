package org.epic.perleditor.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * This class is used to create a preference page for marking occurrences.
 * Several buttons determine, if generally occurrences of a text will be marked,
 * which types of a text should be marked and if the marks will be kept if the
 * selection changed.
 * 
 * @author Katrin Dust
 * 
 */
public class MarkOccurrencesPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	/*
	 * Boolean field to determine if occurrences should be highlighted
	 */
	private BooleanFieldEditor markOcc;

	/*
	 * Boolean field to determine if occurrences of variables will be marked
	 */
	private BooleanFieldEditor variable;

	/*
	 * Boolean field to determine if occurrences of comments will be marked
	 */
	private BooleanFieldEditor comment;

	/*
	 * Boolean field to determine if occurrences of numbers will be marked
	 */
	private BooleanFieldEditor number;

	/*
	 * Boolean field to determine if occurrences of literals will be marked
	 */
	private BooleanFieldEditor literal;

    /*
     * Boolean field to determine if occurrences of subroutine/attribute names will be marked
     */
    private BooleanFieldEditor name;
    
	/*
	 * Boolean field to determine if occurrences of operators will be marked
	 */
	private BooleanFieldEditor operator;

	/*
	 * Boolean field to determine if occurrences of keywords will be marked
	 */
	private BooleanFieldEditor keyword;

	/*
	 * Boolean field to determine if occurrences of pods will be marked
	 */
	private BooleanFieldEditor pod;

	/*
	 * Boolean field to determine if occurrences should be kept when selection
	 * changed
	 */
	private BooleanFieldEditor keepMarks;

	/**
	 * Constructor sets layout (GRID) and the preference store
	 * 
	 */
	public MarkOccurrencesPreferencePage() {
		super(GRID);
		setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * The method creates and adds the fields
	 */
	protected void createFieldEditors() {
		// create fields
		this.markOcc = new BooleanFieldEditor(
				MarkOccurrencesPreferences.MARK_OCCURRENCES,
				"Mark occurrences of the selected element in the current file",
				getFieldEditorParent());
		this.variable = new BooleanFieldEditor(
				MarkOccurrencesPreferences.VARIABLE, "Variable",
				getFieldEditorParent());
		this.comment = new BooleanFieldEditor(
				MarkOccurrencesPreferences.COMMENT, "Comment",
				getFieldEditorParent());
		this.number = new BooleanFieldEditor(MarkOccurrencesPreferences.NUMBER,
				"Number", getFieldEditorParent());
		this.literal = new BooleanFieldEditor(
				MarkOccurrencesPreferences.LITERAL, "Literal",
				getFieldEditorParent());
        this.name = new BooleanFieldEditor(
            MarkOccurrencesPreferences.NAME, "Name (subroutines/attributes)",
            getFieldEditorParent());
		this.operator = new BooleanFieldEditor(
				MarkOccurrencesPreferences.OPERATOR, "Operator",
				getFieldEditorParent());
		this.keyword = new BooleanFieldEditor(
				MarkOccurrencesPreferences.KEYWORD, "Keyword",
				getFieldEditorParent());
		this.pod = new BooleanFieldEditor(MarkOccurrencesPreferences.POD,
				"POD", getFieldEditorParent());
		this.keepMarks = new BooleanFieldEditor(
				MarkOccurrencesPreferences.KEEP_MARKS,
				"Keep marks when the selection changes",
				getFieldEditorParent());
		addField(markOcc);
		addField(variable);
		addField(comment);
		addField(literal);
        addField(name);
		addField(number);
		addField(operator);
		addField(keyword);
		addField(pod);
		addField(keepMarks);
		boolean defaultMarkOcc = PerlEditorPlugin.getDefault()
				.getPreferenceStore().getBoolean(
						MarkOccurrencesPreferences.MARK_OCCURRENCES);
		this.setButtonsEnabled(defaultMarkOcc);

	}

	/**
	 * Calls the super method to restore default values. Sets buttons enabled or
	 * disabled depending on the default value of the mark occurrence button.
	 */
	protected void performDefaults() {
		super.performDefaults();
		boolean defaultMarkOcc = PerlEditorPlugin.getDefault()
				.getPreferenceStore().getBoolean(
						MarkOccurrencesPreferences.MARK_OCCURRENCES);
		this.setButtonsEnabled(defaultMarkOcc);
	}

	/**
	 * nothing to do
	 */
	public void init(IWorkbench workbench) {
	}

	/**
	 * If the property of the mark occurrence changed to false, the other
	 * fields will be disabled (otherwise enabled).
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() != this.markOcc) {
			return;
		}
		boolean isMarkOcc = new Boolean(
            event.getNewValue().toString()).booleanValue();
		setButtonsEnabled(isMarkOcc);

	}

	/**
	 * Enables or disables buttons.
	 * 
	 * @param enabled
	 *        true to enable, false to disable buttons
	 */
	private void setButtonsEnabled(boolean enabled) {
		variable.setEnabled(enabled, getFieldEditorParent());
		comment.setEnabled(enabled, getFieldEditorParent());
		number.setEnabled(enabled, getFieldEditorParent());
		literal.setEnabled(enabled, getFieldEditorParent());
        name.setEnabled(enabled, getFieldEditorParent());
		operator.setEnabled(enabled, getFieldEditorParent());
		keyword.setEnabled(enabled, getFieldEditorParent());
		pod.setEnabled(enabled, getFieldEditorParent());
		keepMarks.setEnabled(enabled, getFieldEditorParent());
	}
}
