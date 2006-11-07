package org.epic.perleditor;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.IFileTypeInfo;
import org.eclipse.team.core.Team;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.epic.core.util.PerlExecutor;
import org.epic.perleditor.editors.PerlDocumentProvider;
import org.epic.perleditor.editors.util.PerlColorProvider;
import org.epic.perleditor.preferences.*;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class PerlEditorPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static PerlEditorPlugin plugin;

	//Resource bundle.
	private ResourceBundle resourceBundle;

    private PerlColorProvider colorProvider = new PerlColorProvider();

	public static final String PERL_EXECUTABLE_PREFERENCE = "PERL_EXECUTABLE";

	private static final String PERL_EXECUTABLE_DEFAULT = "perl";

	public static final String WEB_BROWSER_PREFERENCE = "WEB_BROWSER";

	private static final String WEB_BROWSER_DEFAULT = "http://";

	public static final String WARNINGS_PREFERENCE = "SHOW_WARNINGS";

	public static final String TAINT_MODE_PREFERENCE = "USE_TAINT_MODE";
    
    public static final String DEBUG_CONSOLE_PREFERENCE = "ENABLE_DEBUG_CONSOLE";

	private static final boolean WARNINGS_DEFAULT = true;

	private static final boolean TAINT_MODE_DEFAULT = false;

	public static final String INTERPRETER_TYPE_PREFERENCE = "INTERPRETER_TYPE";

	public static final String INTERPRETER_TYPE_STANDARD = "Standard";

	public static final String INTERPRETER_TYPE_CYGWIN = "Cygwin";

	public static final String SYNTAX_VALIDATION_PREFERENCE = "SYNTAX_VALIDATION_PREFERENCE";
	public static final boolean SYNTAX_VALIDATION_PREFERENCE_DEFAULT = true;

	public static final String SYNTAX_VALIDATION_INTERVAL_PREFERENCE = "SYNTAX_VALIDATION_IDLE_INTERVAL";

	public static final int SYNTAX_VALIDATION_INTERVAL_DEFAULT = 400;

	private IDocumentProvider fDocumentProvider;

    private boolean requirePerlCheckPassed;
    private boolean requirePerlErrorDisplayed;

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
     * Returns a color with the requested RGB value.
     */
    public Color getColor(RGB rgb)
    {
        return colorProvider.getColor(rgb);
    }

    /**
     * Returns a color represented by the given preference setting.
     */
    public Color getColor(String preferenceKey)
    {
        return getColor(
            PreferenceConverter.getColor(getPreferenceStore(), preferenceKey)
            );
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
        store.setDefault(
            PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE,
            true);
		SourceFormatterPreferences.initializeDefaultValues(store);
		CodeAssistPreferences.initializeDefaultValues(store);
		TaskTagPreferences.initializeDefaults(store);
		MarkOccurrencesPreferences.initializeDefaultValues(store);
	}

	public String getExecutablePreference() {
		return getPreferenceStore().getString(PERL_EXECUTABLE_PREFERENCE);
	}

	public String getDefaultExecutablePreference() {
		return PERL_EXECUTABLE_DEFAULT;
	}

	public void setExecutablePreference(String value) {

		getPreferenceStore().setValue(PERL_EXECUTABLE_PREFERENCE, value);
        requirePerlErrorDisplayed = false;
        checkForPerlInterpreter(true);
	}

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
    
    public boolean getDebugConsolePreference() {
        String value = getPreferenceStore().getString(DEBUG_CONSOLE_PREFERENCE);

        return value.equals("1") ? true : false;
    }

    public void setDebugConsolePreference(boolean value) {
        getPreferenceStore().setValue(DEBUG_CONSOLE_PREFERENCE,
                value == true ? "1" : "0");
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
        PerlEditorPlugin plugin = getDefault();
		return plugin != null
            ? plugin.getBundle().getSymbolicName()
            : "org.epic.perleditor";
	}

	public synchronized IDocumentProvider getDocumentProvider() {
		if (fDocumentProvider == null)
			fDocumentProvider = new PerlDocumentProvider();
		return fDocumentProvider;
	}
    
    public static String getUniqueIdentifier()
    {
        PerlEditorPlugin plugin = getDefault();
        return plugin != null ? plugin.getBundle().getSymbolicName() : "org.epic.perleditor";
    }

    /**
     * @return false if no valid Perl interpreter has been available in
     *         Preferences since the plug-in's activation;
     *         true otherwise
     */
    public boolean hasPerlInterpreter()
    {
        return requirePerlCheckPassed;
    }

    /**
     * Same as {@link #hasPerlInterpreter}, but displays an error dialog
     * if false is returned.
     *
     * @param interactive
     *        true, if the check is performed in context of a user-requested
     *        action, false if the check is performed in context of a background
     *        operation
     */
    public boolean requirePerlInterpreter(boolean interactive)
    {
        if (!requirePerlCheckPassed) checkForPerlInterpreter(interactive);
        return requirePerlCheckPassed;
    }

    /**
     * Checks that a valid Perl interpreter is specified in Preferences
     * and updates the requirePerlCheckPassed flag. Displays an error dialog
     * if the check does not pass (but only once for background ops,
     * until Preferences are updated).
     */
    private void checkForPerlInterpreter(boolean interactive)
    {
        final String ERROR_TITLE = "Missing Perl interpreter";
        final String ERROR_MSG =
            "To operate correctly, EPIC requires a Perl interpreter. " +
            "Check your configuration settings (\"Window/Preferences/Perl EPIC\").";

        PerlExecutor executor = new PerlExecutor();
        try
        {
            List args = new ArrayList(1);
            args.add("-v");
            if (executor.execute(new File("."), args, "")
                .stdout.indexOf("This is perl") != -1)
            {
                requirePerlCheckPassed = true;
            }
            else
            {
                Status status = new Status(
                    IStatus.ERROR,
                    getPluginId(),
                    IStatus.OK,
                    "The executable specified in EPIC Preferences " +
                    "does not appear to be a valid Perl interpreter.",
                    null);

                getLog().log(status);
                if (!requirePerlErrorDisplayed || interactive)
                {
                    requirePerlErrorDisplayed = true;
                    showErrorDialog(ERROR_TITLE, ERROR_MSG, status);
                }
                requirePerlCheckPassed = false;
            }
        }
        catch (CoreException e)
        {
            getLog().log(e.getStatus());
            if (!requirePerlErrorDisplayed || interactive)
            {
                requirePerlErrorDisplayed = true;
                showErrorDialog(ERROR_TITLE, ERROR_MSG, e.getStatus());
            }
            requirePerlCheckPassed = false;
        }
        finally { executor.dispose(); }
    }

    private void showErrorDialog(
        final String title,
        final String msg,
        final IStatus status)
    {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                ErrorDialog.openError(null, title, msg, status);
            } });
    }

    public void stop(BundleContext context)
        throws Exception
    {
        colorProvider.dispose();
        super.stop(context);
    }
}