package org.epic.debug;

import org.eclipse.ui.plugin.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import java.util.*;
import java.io.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.debug.core.DebugPlugin
;



/**
 * The main plugin class to be used in the desktop.
 */
public class PerlDebugPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static PerlDebugPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	private static String mSystemEnv[];
	private static String mDebugEnv[];
	private static PerlBreakpointManager mBreakPointmanager;
	
	private static PerlImageDescriptorRegistry defaultPerlImageDescriptorRegistry = new PerlImageDescriptorRegistry();
	private final static String mDebugOptions = "PERLDB_OPTS=RemotePort=localhost:4444";
	/**
	 * The constructor.
	 */
	public PerlDebugPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		try {
			resourceBundle= ResourceBundle.getBundle("org.epic.debug.DebugPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		createEnvArrays();
		mBreakPointmanager = new PerlBreakpointManager( DebugPlugin.getDefault());
	}

	void createEnvArrays()
	{
		Process proc = null;
		String env=null;
		int count;
			try{
			proc =	Runtime.getRuntime().exec("perl  -e\"while(($k,$v)= each %ENV){ print\\\"$k=$v\\n\\\";}\"");
		}catch (Exception e){System.out.println("Failing to create Process !!!");}

		InputStream in = proc.getInputStream();
		StringBuffer content = new StringBuffer();
		
		byte[] buffer = new byte[1];
		
		try{
		while ((count = in.read(buffer)) > 0) {
			content.append(new String(buffer));
		}
		
		env = content.toString();
		in.close();
		} catch(Exception e){};
		
		StringTokenizer s = new StringTokenizer(env,"\r\n");
		count = s.countTokens();
		
		mSystemEnv = new String[count];
		mDebugEnv = new String[count+1];
		
		String token;
			
		for( int x=0; x < count; ++x)
		{ 
			token = s.nextToken();
			mSystemEnv[x] = token;
			mDebugEnv[x] = token;
		}
		
		mDebugEnv[count] = mDebugOptions;
		
	}
	/**
	 * Returns the shared instance.
	 */
	public static PerlDebugPlugin getDefault() {
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
		ResourceBundle bundle= PerlDebugPlugin.getDefault().getResourceBundle();
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
	static String getUniqueIdentifier()
	{ 
		return("org.epic.debug.perldebugger.perl"); 
	}
	
	/**
		 * Logs the specified status with this plug-in's log.
		 *
		 * @param status status to log
		 */
		public static void log(IStatus status) {
			getDefault().getLog().log(status);
		}
		
	/**
		 * Returns the active workbench window
		 *
		 * @return the active workbench window
		 */
		public static IWorkbenchWindow getActiveWorkbenchWindow() {
			return getDefault().getWorkbench().getActiveWorkbenchWindow();
		}

	/**
		 * Returns the active workbench shell or <code>null</code> if none
		 *
		 * @return the active workbench shell or <code>null</code> if none
		 */
		public static Shell getActiveWorkbenchShell() {
			IWorkbenchWindow window = getActiveWorkbenchWindow();
			if (window != null) {
				return window.getShell();
			}
			return null;
		}
		
	public static void log(Throwable e) {
			log(new Status(IStatus.ERROR, getUniqueIdentifier(), 150, "Internal Error", e));  //$NON-NLS-1$
		}
			
	public static void errorDialog(String message, IStatus status) {
		log(status);
	    Shell shell = getActiveWorkbenchShell();
				if (shell != null) {
					ErrorDialog.openError(shell, "Error", message, status); //$NON-NLS-1$
				}
	}
	
	/**
		 * Utility method with conventions
		 */
		public static void errorDialog(String message, Throwable t) {
			log(t);
			Shell shell = getActiveWorkbenchShell();
			if (shell != null) {
				IStatus status= new Status(IStatus.ERROR, getUniqueIdentifier(), 150, "Error logged from JDT Debug UI: ", t); //$NON-NLS-1$
				ErrorDialog.openError(shell,"Error", message, status); //$NON-NLS-1$
			}
		}
		
		public static PerlImageDescriptorRegistry getDefaultDesciptorImageRegistry()
		{ 
			return (defaultPerlImageDescriptorRegistry);
			        
		}
		
		public static String[] getSystemEnv()
		{
			return(mSystemEnv);
		}
		
		public static String[] getDebugEnv()
		{
			return(mDebugEnv);
		}

		public static PerlBreakpointManager getPerlBreakPointmanager()
		{
			 		return(mBreakPointmanager);
		}
}
