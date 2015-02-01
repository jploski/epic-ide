package org.epic.perleditor;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.IFileTypeInfo;
import org.eclipse.team.core.Team;
import org.eclipse.ui.IWorkbenchWindow;
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
		PreferenceConstants.initializeDefaultValues(store);
		SourceFormatterPreferences.initializeDefaultValues(store);
		CodeAssistPreferences.initializeDefaultValues(store);
		TaskTagPreferences.initializeDefaults(store);
		MarkOccurrencesPreferences.initializeDefaultValues(store);

        System.setProperty(
            PreferenceConstants.SOURCE_CRITIC_ENABLED,
            store.getString(PreferenceConstants.SOURCE_CRITIC_ENABLED));
	}

	public String getPerlExecutable() {
		return getPreferenceStore().getString(PreferenceConstants.DEBUG_PERL_EXECUTABLE);
	}

	public void setPerlExecutable(String value) {

		getPreferenceStore().setValue(PreferenceConstants.DEBUG_PERL_EXECUTABLE, value);
        requirePerlErrorDisplayed = false;
        checkForPerlInterpreter(true);
	}
    
    public boolean getBooleanPreference(String name) {
        boolean ret = getPreferenceStore().getBoolean(name);
        if (ret) return true;
        
        // sadly, necessary for backward-compatibility with
        // old versions of EPIC:
        String value = getPreferenceStore().getString(name);
        return value.equals("1") ? true : false;
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
    
    protected ImageRegistry createImageRegistry()
    {
        return PerlPluginImages.getImageRegistry();
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
            "Check your configuration settings (\"Window/Preferences/Perl\").";

        PerlExecutor executor = new PerlExecutor();
        try
        {
            List<String> args = new ArrayList<String>(1);
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
                    "The executable specified in Perl Preferences " +
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