package cbg.editor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import cbg.editor.rules.ColorManager;

/**
 * The main plugin class to be used in the desktop.
 */
public class EditorPlugin extends AbstractUIPlugin {
	public static final String STATUS_CATEGORY_MODE = "cbg.editor.modeStatus";
	public ColorManager getColorManager() {
		if (colorManager == null) {
			colorManager = new ColorManager(getPreferenceStore());
		}
		return colorManager;
	}
	public ColoringEditorTools getEditorTools() {
		if (editorTools == null)
			editorTools = new ColoringEditorTools();
		return editorTools;
	}
	//The shared instance.
	private static EditorPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
   private ColoringEditorTools editorTools;
    private ColorManager colorManager;

	
	/**
	 * The constructor.
	 */
	public EditorPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		try {
			resourceBundle= ResourceBundle.getBundle("cbg.editor.EditorPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static EditorPlugin getDefault() {
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
		ResourceBundle bundle= EditorPlugin.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public static ImageDescriptor getImageDescriptor(String imageName) {
		String iconPath = "icons/";
		try {
			URL installURL = getDefault().getBundle().getEntry("/");
			URL url = new URL(installURL, iconPath + imageName);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put("box", getImageDescriptor("boxIcon.gif").createImage());
	}

	public static Image getImage(String handle) {
		return getDefault().getImageRegistry().get(handle);
	}

	protected void initializeDefaultPluginPreferences() {			
		super.initializeDefaultPluginPreferences();
		IPreferenceStore store = getPreferenceStore();
		ColorManager.initDefaultColors(store);
		store.setDefault(ColoringSourceViewerConfiguration.SPACES_FOR_TABS, false);
		store.setDefault(ColoringSourceViewerConfiguration.PREFERENCE_TAB_WIDTH, 4);
	}
	public static void logError(String message, Exception exception) {
		if(getDefault() == null) return;
		getDefault().getLog().log(new Status(IStatus.ERROR, (String) getDefault().getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME), 
			IStatus.OK, message, exception));
	}
}
