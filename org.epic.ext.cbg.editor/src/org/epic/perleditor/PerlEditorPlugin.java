package org.epic.perleditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import java.util.*;

import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * The main plugin class to be used in the desktop.
 */
public class PerlEditorPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static PerlEditorPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	public static final String PERl_EXECUTABLE_PREFERENCE = "PERL_EXECUTABLE";
	private static final String PERl_EXECUTABLE_DEFAULT = "perl";
	
	public static final String WARNINGS_PREFERENCE = "SHOW_WARNINGS";
	private static final boolean WARNINGS_DEFAULT = true;
	
	
	public static final String INTERPRETER_TYPE_PREFERENCE ="INTERPRETER_TYPE";
	public static final String INTERPRETER_TYPE_STANDARD = "Standard";
	public static final String INTERPRETER_TYPE_CYGWIN = "Cygwin";
	
	/**
	 * The constructor.
	 */
	public PerlEditorPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		try {
			resourceBundle= ResourceBundle.getBundle("org.epic.perleditor.PerlEditorPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static PerlEditorPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= PerlEditorPlugin.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	

	
	/** 
	 * Initializes a preference store with default preference values 
	 * for this plug-in.
	 * @param store the preference store to fill
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault(PERl_EXECUTABLE_PREFERENCE, PERl_EXECUTABLE_DEFAULT);
		PreferenceConstants.initializeDefaultValues(store);
		store.setDefault(INTERPRETER_TYPE_PREFERENCE, INTERPRETER_TYPE_STANDARD);

	}
	
	/**
	 * Return the bad words preference as an array of
	 * Strings.
	 * @return String[]
	 */
	public String getExecutablePreference() {
		return getPreferenceStore().getString(PERl_EXECUTABLE_PREFERENCE);
	}
	
	public String getDefaultExecutablePreference() {
		return PERl_EXECUTABLE_DEFAULT;
	}
	
	/**
	 * Set the bad words preference
	 * @param String [] elements - the Strings to be 
	 * 	converted to the preference value
	 */
	public void setExecutablePreference(String value) {
		
		getPreferenceStore().setValue(PERl_EXECUTABLE_PREFERENCE, value);
	}
	
	public boolean getWarningsPreference() {
		String value = getPreferenceStore().getString(WARNINGS_PREFERENCE);
		
		return value.equals("1")?true:false;
	}
	
	public boolean getDefaultWarningsPreference() {
		return WARNINGS_DEFAULT;
	}
	
	public void setWarningsPreference(boolean value) {
		getPreferenceStore().setValue(WARNINGS_PREFERENCE, value==true?"1":"0");
	}
}
