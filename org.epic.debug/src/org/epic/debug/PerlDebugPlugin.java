package org.epic.debug;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.epic.debug.util.LogWriter;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;

/**
 * The main plugin class to be used in the desktop.
 */
public class PerlDebugPlugin extends AbstractUIPlugin {

	final private static int mScreenLogLevel = 0;

	final private static int mLogLevel = Status.WARNING;

	final private static String mDefaultDebugPort = "4444";

	//The shared instance.
	private static PerlDebugPlugin plugin;

	//Resource bundle.
	private ResourceBundle resourceBundle;

	private static String mSystemEnv[];

	private static PerlBreakpointManager mBreakPointmanager;

	private ArrayList mDebugger;

	private static PerlImageDescriptorRegistry defaultPerlImageDescriptorRegistry = new PerlImageDescriptorRegistry();

	private final static String mDebugOptionsEnvPrefix = "PERLDB_OPTS=RemotePort=";

	private final static String mDebugOptionsValue = "DumpReused ReadLine=0";

	//"PERLDB_OPTS=RemotePort=localhost:4444 DumpReused ReadLine=0";
	// frame=2";
	/**
	 * The constructor.
	 */
	public PerlDebugPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		try {
			resourceBundle = ResourceBundle
					.getBundle("org.epic.debug.DebugPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}

		mBreakPointmanager = new PerlBreakpointManager(DebugPlugin.getDefault());
		mDebugger = new ArrayList();

		getLog().addLogListener(
				new LogWriter(new File(getStateLocation() + File.separator
						+ ".log"), mLogLevel));
		getLog().addLogListener(new LogWriter(System.err, mScreenLogLevel));
		//		log(
		//			new Status(
		//				IStatus.INFO,
		//				getUniqueIdentifier(),
		//				150,
		//				"Plugin Started",
		//				null));
		//		log(
		//			new Status(
		//				IStatus.WARNING,
		//				getUniqueIdentifier(),
		//				150,
		//				"Plugin Started",
		//				null));
	}

	public void logOK(String fText, Exception fException) {
		log(IStatus.OK, fText, fException);
	}

	public void logOK(String fText) {
		log(IStatus.OK, fText, null);
	}

	public void logInfo(String fText, Exception fException) {
		log(IStatus.INFO, fText, fException);
	}

	public void logInfo(String fText) {
		log(IStatus.INFO, fText, null);
	}

	public void logWarning(String fText, Exception fException) {
		log(IStatus.WARNING, fText, fException);
	}

	public void logWarning(String fText) {
		log(IStatus.WARNING, fText, null);
	}

	public void logError(String fText, Exception fException) {
		log(IStatus.ERROR, fText, fException);
	}

	public void logError(String fText) {
		log(IStatus.ERROR, fText, null);
	}

	private void log(int fSeverity, String fText, Exception fException) {

		Status status, result;
		MultiStatus multiStatus;

		if (fException != null) {

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintWriter swr = new PrintWriter(out);
			fException.printStackTrace(swr);
			String buf = (fException.getMessage() + "\n" + out.toString());
			StringTokenizer tok = new StringTokenizer(buf, "\n");
			multiStatus = new MultiStatus(getUniqueIdentifier(), fSeverity,
					fText, fException);
			while (tok.hasMoreElements()) {
				status = new Status(fSeverity, getUniqueIdentifier(), 0, tok
						.nextToken(), null);
				multiStatus.add(status);
			}

			result = multiStatus;
		} else {
			result = new Status(fSeverity, getUniqueIdentifier(), 0, fText,
					fException);
		}
		log(new Status(fSeverity, getUniqueIdentifier(), 0, fText, fException));
		errorDialog(fText, result);
	}

	static String[] createEnvArrays(Target fTarget) {
		Process proc = null;
		String env = null;
		String[] debugEnv;
		int count;
		//String command[]= {PerlExecutableUtilities.getPerlExecPath(),
		// "-e","'while(($k,$v)= each %ENV){ print\"$k=$v\\n\";}'"};
		//String command = "while(($k,$v)= each %ENV){ print\"$k=$v\\n\";}";
		String command[]= {PerlExecutableUtilities.getPerlExecPath(),PerlDebugPlugin.getPlugInDir()+"get_env.pl"};
		try {

			proc = Runtime.getRuntime().exec(
					command);
		
			Thread.sleep(1);

		//	InputStream in = proc.getInputStream();

		

		proc.getErrorStream().close();
		} catch (Exception e) {
			getDefault().logError("Error reading environment: check Perl executable preference !");
		}
		InputStream in = proc.getInputStream();

		try {
			env = PerlExecutableUtilities.readStringFromStream(in);
			in.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		StringTokenizer s = new StringTokenizer(env, "\r\n");
		count = s.countTokens();

		mSystemEnv = new String[count];
		if (fTarget instanceof DebugTarget)
			debugEnv = new String[count + 1];
		else
			debugEnv = new String[count];

		String token;

		for (int x = 0; x < count; ++x) {
			token = s.nextToken();
			mSystemEnv[x] = token;
			debugEnv[x] = token;
		}

		if (fTarget instanceof DebugTarget)
			debugEnv[count] = getPerlDebugEnv((DebugTarget) fTarget);
		return debugEnv;
		//mDebugEnv[count+1] = "PERL5DB=BEGIN {require'perl5db.pl'}";
	}

	
	
	
	public static void createDefaultIncPath(List fInc) {
		Process proc = null;
		String erg = null;
		int count;
		//String command[]= {PerlExecutableUtilities.getPerlExecPath(),
		// "-e","'while(($k,$v)= each %ENV){ print\"$k=$v\\n\";}'"};
		//String command = "while(($k,$v)= each %ENV){ print\"$k=$v\\n\";}";
		String command[]= {PerlExecutableUtilities.getPerlExecPath(),PerlDebugPlugin.getPlugInDir()+"get_inc.pl"};
		try {

			proc = Runtime.getRuntime().exec(
					command);
		
			Thread.sleep(1);

		//	InputStream in = proc.getInputStream();

		

		proc.getErrorStream().close();
		} catch (Exception e) {
			getDefault().logError("Error reading include path: check Perl executable preference !");
		}
		InputStream in = proc.getInputStream();

		try {
			erg = PerlExecutableUtilities.readStringFromStream(in);
			in.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		StringTokenizer s = new StringTokenizer(erg, "\r\n");
		count = s.countTokens();

		

		String token;

		for (int x = 0; x < count; ++x) {
			token = s.nextToken();
					fInc.add(token);
		}

	}

	public static String getPerlDebugEnv(DebugTarget fTarget) {
		String port = null;
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		;

		port = fTarget.getDebugPort();
		
		/* avoids problems with local software firewall*/
		if(fTarget.isLocal()) host ="127.0.0.1";
		return (mDebugOptionsEnvPrefix + host + ":" + port + " " + mDebugOptionsValue);
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
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = PerlDebugPlugin.getDefault()
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

	static String getUniqueIdentifier() {
		return ("org.epic.debug.perldebugger.perl");
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status
	 *            status to log
	 */
	private static void log(IStatus status) {
		getDefault().getLog().log(status);
		Throwable e = status.getException();
		if (e != null)
			e.printStackTrace();
	}

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	public static IWorkbenchWindow getWorkbenchWindow() {
		IWorkbenchWindow window = getDefault().getWorkbench()
				.getActiveWorkbenchWindow();
		if (window == null)
			window = getDefault().getWorkbench().getWorkbenchWindows()[0];
		return window;
	}

	/**
	 * Returns the active workbench shell or <code>null</code> if none
	 * 
	 * @return the active workbench shell or <code>null</code> if none
	 */
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window == null)
			window = getDefault().getWorkbench().getWorkbenchWindows()[0];
		if (window != null) {

			return window.getShell();
		}
		return null;
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), 0,
				"Debug Error", e)); //$NON-NLS-1$
	}

	public static void errorDialog(String message) {
		errorDialog(message, null);
	}

	public static void errorDialog(String message, IStatus status) {

		Shell shell = getActiveWorkbenchShell();
		if (shell != null) {
			shell.getDisplay().syncExec(
					new DisplayErrorThread(shell, message, status));
		}
	}

	static class DisplayErrorThread implements Runnable {
		Shell mShell;

		String mMessage;

		IStatus mStatus;

		public DisplayErrorThread(Shell fShell, String fMessage, IStatus fStatus) {
			mShell = fShell;
			mMessage = fMessage;
			mStatus = fStatus;
		}

		public void run() {
			if (mStatus == null)
				MessageDialog.openError(mShell, "EPIG Error", mMessage);
			else
				ErrorDialog.openError(mShell, "Error", null, mStatus); //$NON-NLS-1$

		}
	}

	public static PerlImageDescriptorRegistry getDefaultDesciptorImageRegistry() {
		return (defaultPerlImageDescriptorRegistry);

	}

	public static String[] getSystemEnv() {
		return (mSystemEnv);
	}

	public static String[] getDebugEnv(Target fTarget) {
		return (createEnvArrays(fTarget));
	}

	public static PerlBreakpointManager getPerlBreakPointmanager() {
		return (mBreakPointmanager);
	}

	public void registerDebugTarget(Target fDebug) {
		mDebugger.add(fDebug);
	}

	public void unregisterDebugTarget(Target fDebug) {
		mDebugger.remove(fDebug);
	}

	public void registerDebugEventListener(IDebugEventSetListener fListener) {
		DebugPlugin.getDefault().addDebugEventListener(fListener);
	}

	public void unregisterDebugEventListener(IDebugEventSetListener fListener) {
		DebugPlugin.getDefault().removeDebugEventListener(fListener);
	}

	public void shutdown() {
		Target db;
		Iterator i = mDebugger.iterator();
		while (i.hasNext()) {
			db = (Target) i.next();
			if (db != null)
				db.shutdown(false);
		}
	}

	public static String getDefaultDebugPort() {
		return mDefaultDebugPort;
	}

	static public String getPlugInDir()
	{
		URL installURL =
			getDefault().getDescriptor().getInstallURL();
			
		try
		{
			installURL = Platform.resolve(installURL);
		} catch (IOException e)
		{
			getDefault().logError(
				"Error retrieving Plugin dir",
				e);
		}
		String path =installURL.getPath();
		if( path.charAt(0) == '/' && path.charAt(2)==':' && path.charAt(3) == '/')
			path = path.substring(1);
		return (path);
	}

}