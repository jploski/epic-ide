package org.epic.perleditor.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.epic.perleditor.PerlEditorPlugin;


/**
 * @author luelljoc
 * 
 * Source Formatter default settings
 */
public class SourceFormatterPreferences {
	public static final String CUDDLED_ELSE = "cuddledElse";
	public static final String BRACES_LEFT = "bracesLeft";
	public static final String LINE_UP_WITH_PARENTHESES = "lineUpWithParenttheses";
	public static final String CONTAINER_TIGHTNESS_BRACES = "containerTightnessBraces";
	public static final String CONTAINER_TIGHTNESS_PARENTHESES = "containerTightnessParentheses";
	public static final String CONTAINER_TIGHTNESS_SQUARE_BRACKETS = "containerTightnessSquareBrackets";

	
	
	/**
	 * Default values intitialization
	 * Calls initializeDefaultValues()
	 * @param store
	 */
	public void initializeDefaults(IPreferenceStore store) {
		initializeDefaultValues(store);
	}
	
	/**
	 * Static method to initialize default values
	 * @param store
	 */
	public static void initializeDefaultValues(IPreferenceStore store) {
		store.setDefault(CUDDLED_ELSE, false);
		store.setDefault(BRACES_LEFT, false);
		store.setDefault(LINE_UP_WITH_PARENTHESES, false);
		store.setDefault(CONTAINER_TIGHTNESS_BRACES, 1);
		store.setDefault(CONTAINER_TIGHTNESS_PARENTHESES, 1);
		store.setDefault(CONTAINER_TIGHTNESS_SQUARE_BRACKETS, 1);
}

	

	/**
	 * Method getPluginPreferences.
	 * @return Preferences
	 */
	public Preferences getPluginPreferences() {
		return PerlEditorPlugin.getDefault().getPluginPreferences();
	}

}
