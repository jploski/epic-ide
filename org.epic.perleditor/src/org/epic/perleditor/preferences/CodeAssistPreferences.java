package org.epic.perleditor.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.epic.perleditor.PerlEditorPlugin;


/**
 * @author luelljoc
 * 
 * Source Formatter default settings
 */
public class CodeAssistPreferences {
    public static final String AUTO_ACTIVATION_CHARS = "CodeAssist.autoActivationChars";
    public static final String INSPECT_VARIABLES = "CodeAssist.inspectVariables";
    public static final String AUTO_ACTIVATION_DELAY = "CodeAssist.autoActivationDelay";
    public static final String EXTERNAL_ASSISTANT_PATH = "CodeAssist.externalAssistantPath";
    
    
    /**
     * Default values initialization
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
        store.setDefault(AUTO_ACTIVATION_CHARS, ">:<$%@");
        store.setDefault(INSPECT_VARIABLES, true);
        store.setDefault(AUTO_ACTIVATION_DELAY, 500);
        store.setDefault(EXTERNAL_ASSISTANT_PATH, "");
    }

    

    /**
     * Method getPluginPreferences.
     * @return Preferences
     */
    public Preferences getPluginPreferences() {
        return PerlEditorPlugin.getDefault().getPluginPreferences();
    }

}
