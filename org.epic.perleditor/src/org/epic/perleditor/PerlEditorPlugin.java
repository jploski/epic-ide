package org.epic.perleditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.*;
//import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.team.core.IFileTypeInfo;
import org.eclipse.team.core.Team;
import java.util.*;

import org.epic.perleditor.editors.PerlDocumentProvider;
import org.epic.perleditor.preferences.CodeAssistPreferences;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.preferences.SourceFormatterPreferences;
import org.epic.perleditor.preferences.TaskTagPreferences;

import cbg.editor.ColoringEditorTools;

/**
 * The main plugin class to be used in the desktop.
 */
public class PerlEditorPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static PerlEditorPlugin plugin;

	//Resource bundle.
	private ResourceBundle resourceBundle;

	public static final String PERL_EXECUTABLE_PREFERENCE = "PERL_EXECUTABLE";

	private static final String PERL_EXECUTABLE_DEFAULT = "perl";

	public static final String WEB_BROWSER_PREFERENCE = "WEB_BROWSER";

	private static final String WEB_BROWSER_DEFAULT = "http://";

	public static final String WARNINGS_PREFERENCE = "SHOW_WARNINGS";

	public static final String TAINT_MODE_PREFERENCE = "USE_TAINT_MODE";

	private static final boolean WARNINGS_DEFAULT = true;

	private static final boolean TAINT_MODE_DEFAULT = false;

	public static final String INTERPRETER_TYPE_PREFERENCE = "INTERPRETER_TYPE";

	public static final String INTERPRETER_TYPE_STANDARD = "Standard";

	public static final String INTERPRETER_TYPE_CYGWIN = "Cygwin";

	public static final String SYNTAX_VALIDATION_PREFERENCE = "SYNTAX_VALIDATION_PREFERENCE";
	public static final boolean SYNTAX_VALIDATION_PREFERENCE_DEFAULT = true;
	
	public static final String SYNTAX_VALIDATION_INTERVAL_PREFERENCE = "SYNTAX_VALIDATION_IDLE_INTERVAL";

	public static final int SYNTAX_VALIDATION_INTERVAL_DEFAULT = 400;

	private PerlDocumentProvider fDocumentProvider;

	private ColoringEditorTools editorTools;

	/**
	 * The constructor.
	 */
	//public PerlEditorPlugin(IPluginDescriptor descriptor) {
	public PerlEditorPlugin() {
		//super(descriptor);
		plugin = this;
		try {
			resourceBundle = ResourceBundle
					.getBundle("org.epic.perleditor.PerlEditorPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}

		// Set team file extensions
		String[] perlTypes = { "pl", "pm" };
		IFileTypeInfo[] fileTypes = Team.getAllTypes();

		int newTypesLength = fileTypes.length + perlTypes.length;
		String[] extensions = new String[newTypesLength];
		int[] types = new int[newTypesLength];

		int i;
		for (i = 0; i < fileTypes.length; i++) {
			extensions[i] = fileTypes[i].getExtension();
			types[i] = fileTypes[i].getType();
		}

		// Add Perl extensions to the list as ASCII
		for (; i < newTypesLength; i++) {
			extensions[i] = perlTypes[i - fileTypes.length];
			types[i] = Team.TEXT;
		}

		Team.setAllTypes(extensions, types);
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
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = PerlEditorPlugin.getDefault()
				.getResourceBundle();
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

	public static IWorkbenchWindow getWorkbenchWindow() {
		IWorkbenchWindow window = getDefault().getWorkbench()
				.getActiveWorkbenchWindow();
		if (window == null)
			window = getDefault().getWorkbench().getWorkbenchWindows()[0];
		return window;
	}

	public ColoringEditorTools getEditorTools() {
		if (editorTools == null)
			editorTools = new ColoringEditorTools();
		return editorTools;
	}

	/**
	 * Initializes a preference store with default preference values for this
	 * plug-in.
	 * 
	 * @param store
	 *            the preference store to fill
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault(PERL_EXECUTABLE_PREFERENCE, PERL_EXECUTABLE_DEFAULT);
		PreferenceConstants.initializeDefaultValues(store);
		store.setDefault(INTERPRETER_TYPE_PREFERENCE,
						INTERPRETER_TYPE_STANDARD);
		store.setDefault(SYNTAX_VALIDATION_INTERVAL_PREFERENCE,
				SYNTAX_VALIDATION_INTERVAL_DEFAULT);
		SourceFormatterPreferences.initializeDefaultValues(store);
		CodeAssistPreferences.initializeDefaultValues(store);
		TaskTagPreferences.initializeDefaults(store);
	}

	/**
	 * Return the bad words preference as an array of Strings.
	 * 
	 * @return String[]
	 */
	public String getExecutablePreference() {
		return getPreferenceStore().getString(PERL_EXECUTABLE_PREFERENCE);
	}

	public String getDefaultExecutablePreference() {
		return PERL_EXECUTABLE_DEFAULT;
	}

	/**
	 * Set the bad words preference
	 * 
	 * @param String []
	 *            elements - the Strings to be converted to the preference value
	 */
	public void setExecutablePreference(String value) {

		getPreferenceStore().setValue(PERL_EXECUTABLE_PREFERENCE, value);
	}

	/**
	 * Return the bad words preference as an array of Strings.
	 * 
	 * @return String[]
	 */
	public String getWebBrowserPreference() {
		return getPreferenceStore().getString(WEB_BROWSER_PREFERENCE);
	}

	public String getDefaultWebBrowserPreference() {
		return WEB_BROWSER_DEFAULT;
	}

	public void setWebBrowserPreference(String value) {

		getPreferenceStore().setValue(WEB_BROWSER_PREFERENCE, value);
	}

	public boolean getWarningsPreference() {
		String value = getPreferenceStore().getString(WARNINGS_PREFERENCE);

		return value.equals("1") ? true : false;
	}

	public boolean getDefaultWarningsPreference() {
		return WARNINGS_DEFAULT;
	}

	public void setTaintPreference(boolean value) {
		getPreferenceStore().setValue(TAINT_MODE_PREFERENCE,
				value == true ? "1" : "0");
	}

	public boolean getTaintPreference() {
		String value = getPreferenceStore().getString(TAINT_MODE_PREFERENCE);

		return value.equals("1") ? true : false;
	}

	public boolean getDefaultTaintPreference() {
		return TAINT_MODE_DEFAULT;
	}

	public void setWarningsPreference(boolean value) {
		getPreferenceStore().setValue(WARNINGS_PREFERENCE,
				value == true ? "1" : "0");
	}
	
	public boolean getSyntaxValidationPreference() {
		String value = getPreferenceStore().getString(SYNTAX_VALIDATION_PREFERENCE);

		return value.equals("1") ? true : false;
	}

	public boolean getDefaultSyntaxValidationPreference() {
		return SYNTAX_VALIDATION_PREFERENCE_DEFAULT;
	}
	
	public void setSyntaxValidationPreference(boolean value) {
		getPreferenceStore().setValue(SYNTAX_VALIDATION_PREFERENCE,
				value == true ? "1" : "0");
	}

	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}

	public synchronized PerlDocumentProvider getDocumentProvider() {
		if (fDocumentProvider == null)
			fDocumentProvider = new PerlDocumentProvider();
		return fDocumentProvider;
	}
}