
package org.epic.spellchecker;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.bdaum.SpellChecker.preferences.SpellCheckerPreferences;


public class PerlSpellCheckerPlugin extends AbstractUIPlugin {

	// Das Plugin-Singleton.
	private static PerlSpellCheckerPlugin plugin;

	/**
	 * The constructor.
	 */
	public PerlSpellCheckerPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
	}

	/**
	 * Returns the shared instance.
	 */
	public static PerlSpellCheckerPlugin getDefault() {
		return plugin;
	}

	/**
	 * Initialisierung des PreferenceStores
	 */
	protected void initializeDefaultPluginPreferences() {
		IPreferenceStore store = getPreferenceStore();
		SpellCheckerPreferences preferences = new PerlSpellCheckerPreferences();
		preferences.initializeDefaults(store);
	}

}

