package org.epic.spellchecker;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;

import com.bdaum.SpellChecker.SpellCheckerPlugin;
import com.bdaum.SpellChecker.preferences.SpellCheckerPreferences;
import com.swabunga.spell.engine.Configuration;


public class PerlSpellCheckerPreferences extends SpellCheckerPreferences {

	public static final String CHECKPOD = "checkPod";
	public static final String CHECKCOMMENTS = "checkComments";
	public static final String CHECKSTRINGLITERALS = "stringLiterals";
	public static final String IGNORECOMPOUNDS = "ignoreCompounds";


	/**
	 * Method initializePublicPreferences.
	 * Java-spezifische Vorgabewerte für die öffentlichen Optionen.
	 * @param store - der PreferenceStore
	 */
	protected void initializePublicPreferences(IPreferenceStore store) {
		store.setDefault(
			SPELL_DICTIONARY,
			SpellCheckerPlugin.getDefaultDictionaryFileName());
		store.setDefault(Configuration.SPELL_THRESHOLD, 140);
		store.setDefault(Configuration.SPELL_IGNOREDIGITWORDS, true);
		store.setDefault(Configuration.SPELL_IGNOREINTERNETADDRESSES, true);
		store.setDefault(Configuration.SPELL_IGNOREMIXEDCASE, true);
		store.setDefault(Configuration.SPELL_IGNOREMULTIPLEWORDS, false);
		store.setDefault(
			Configuration.SPELL_IGNORESENTANCECAPITALIZATION,
			false);
		store.setDefault(Configuration.SPELL_IGNOREUPPERCASE, true);
		store.setDefault(IGNORECOMPOUNDS, true);
		store.setDefault(CHECKPOD, true);
		store.setDefault(CHECKCOMMENTS, true);
		store.setDefault(CHECKSTRINGLITERALS, true);
	}

	/**
	 * Method getPluginPreferences.
	 * @return Preferences
	 */
	public Preferences getPluginPreferences() {
		return PerlSpellCheckerPlugin.getDefault().getPluginPreferences();
	}

}
