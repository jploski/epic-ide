package org.epic.spellchecker;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;

import com.bdaum.SpellChecker.preferences.SpellCheckerPreferencePage;

/**
 * Diese Klasse implementiert die PreferencePage f?r 
 * die Java-Rechtschreibepr?fung
 */
public class PerlSpellCheckerPreferencePage
	extends SpellCheckerPreferencePage {

	/*
	* @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	*/
	public IPreferenceStore doGetPreferenceStore() {
		return PerlSpellCheckerPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * @see com.bdaum.SpellChecker.preferences.SpellCheckerPreferencePage
	 *                                         #getPreferenceHelpContextID()
	 */
	protected String getPreferenceHelpContextID() {
		return "com.bdaum.SpellChecker.Java.java_preferences_context";
	}

	/**
	 * Wir f?gen Java-spezifische Optionen zu den Feldeditoren hinzu.
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage
	 *                                        #createFieldEditors()
	 */
	public void createFieldEditors() {
		//Initalisierung der Default-Werte erzwingen
		super.createFieldEditors();
		Composite composite = getFieldEditorParent();
		addField(
			new BooleanFieldEditor(
				PerlSpellCheckerPreferences.IGNORECOMPOUNDS,
				"Ignore &Compounds (Words containing '->' or '::')",
				composite));
		addField(
			new BooleanFieldEditor(
				PerlSpellCheckerPreferences.CHECKPOD,
				"Check &POD",
				composite));
		addField(
			new BooleanFieldEditor(
				PerlSpellCheckerPreferences.CHECKCOMMENTS,
				"Check C&omments",
				composite));
		addField(
			new BooleanFieldEditor(
				PerlSpellCheckerPreferences.CHECKSTRINGLITERALS,
				"Check String &Literals",
				composite));
	}
}
