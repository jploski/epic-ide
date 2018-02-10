package org.epic.perleditor.preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.epic.perleditor.PerlEditorPlugin;


/**
 * @author luelljoc
 * 
 * Source Formatter default settings
 */
public class SourceFormatterPreferences {
    public static final String CUDDLED_ELSE = "Formatter.cuddledElse";
    public static final String BRACES_LEFT = "Formatter.bracesLeft";
    public static final String LINE_UP_WITH_PARENTHESES = "Formatter.lineUpWithParenttheses";
//	public static final String CONTAINER_TIGHTNESS_BRACES = "Formatter.containerTightnessBraces";
//	public static final String CONTAINER_TIGHTNESS_PARENTHESES = "Formatter.containerTightnessParentheses";
//	public static final String CONTAINER_TIGHTNESS_SQUARE_BRACKETS = "Formatter.containerTightnessSquareBrackets";
    public static final String SWALLOW_OPTIONAL_BLANK_LINES ="Formatter.swallowOptionalBlankLines";
    public static final String PERLTIDY_OPTIONS = "Formatter.perltidyOptions";
    public static final String HTML_EXPORT_OPTIONS = "Formatter.htmlExportOptions";
    
    
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
//		store.setDefault(CONTAINER_TIGHTNESS_BRACES, 1);
//		store.setDefault(CONTAINER_TIGHTNESS_PARENTHESES, 1);
//		store.setDefault(CONTAINER_TIGHTNESS_SQUARE_BRACKETS, 1);
        store.setDefault(PERLTIDY_OPTIONS, "");
        store.setDefault(HTML_EXPORT_OPTIONS, "-toc");
}

    

    /**
     * Method getPluginPreferences.
     * @return IEclipsePreferences
     */
    public IEclipsePreferences getPluginPreferences() {
        return DefaultScope.INSTANCE.getNode( PerlEditorPlugin.getPluginId() );
    //  return PerlEditorPlugin.getDefault().getPluginPreferences();
    }

}
