package org.epic.debug;

import org.eclipse.ui.plugin.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.epic.debug.util.LogWriter;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;

/**
 * The main plugin class to be used in the desktop.
 */
public class PerlDebugPlugin extends AbstractUIPlugin
{

	final private static int mScreenLogLevel = 0;
	final private static int mLogLevel = Status.WARNING;
	final private static String mDefaultDebugPort="4444";
	//The shared instance.
	private static PerlDebugPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;

	private static String mSystemEnv[];
	private static PerlBreakpointManager mBreakPointmanager;
	private ArrayList mDebugger;

	private static PerlImageDescriptorRegistry defaultPerlImageDescriptorRegistry =
		new PerlImageDescriptorRegistry();
	private final static String mDebugOptionsEnvPrefix =
		"PERLDB_OPTS=RemotePort=";
	private final static String mDebugOptionsValue =
			"DumpReused ReadLine=0";
	//"PERLDB_OPTS=RemotePort=localhost:4444 DumpReused ReadLine=0";
	// frame=2";
	/**
	 * The constructor.
	 */
	public PerlDebugPlugin(IPluginDescriptor descriptor)
	{
		super(descriptor);
		plugin = this;
		try
		{
			resourceBundle =
				ResourceBundle.getBundle("org.epic.debug.DebugPluginResources");
		} catch (MissingResourceException x)
		{
			resourceBundle = null;
		}

		mBreakPointmanager =
			new PerlBreakpointManager(DebugPlugin.getDefault());
		mDebugger = new ArrayList();

		getLog().addLogListener(
			new LogWriter(
				new File(getStateLocation() + File.separator + ".log"),
				mLogLevel));
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

	public void logOK(String fText, Exception fException)
	{
		log(IStatus.OK, fText, fException);
	}

	public void logOK(String fText)
	{
		log(IStatus.OK, fText, null);
	}

	public void logInfo(String fText, Exception fException)
	{
		log(IStatus.INFO, fText, fException);
	}

	public void logInfo(String fText)
	{
		log(IStatus.INFO, fText, null);
	}

	public void logWarning(String fText, Exception fException)
	{
		log(IStatus.WARNING, fText, fException);
	}

	public void logWarning(String fText)
	{
		log(IStatus.WARNING, fText, null);
	}

	public void logError(String fText, Exception fException)
	{
		log(IStatus.ERROR, fText, fException);
	}

	public void logError(String fText)
	{
		log(IStatus.ERROR, fText, null);
	}

	private void log(int fSeverity, String fText, Exception fException)
	{
		Status status =
			new Status(fSeverity, getUniqueIdentifier(), 0, fText, fException);
		log(status);
		errorDialog(fText, status);
	}
	static String[] createEnvArrays(ILaunch fLaunch)
	{
		Process proc = null;
		String env = null;
		String[] debugEnv;
		int count;
		try
		{
			proc =
				Runtime.getRuntime().exec(
					PerlExecutableUtilities.getPerlExecPath()
						+ " -e\"while(($k,$v)= each %ENV){ print\\\"$k=$v\\n\\\";}\"");
		} catch (Exception e)
		{
			System.out.println("Failing to create Process !!!");
		}

		InputStream in = proc.getInputStream();
		StringBuffer content = new StringBuffer();

		byte[] buffer = new byte[1];

		try
		{
			while ((count = in.read(buffer)) > 0)
			{
				content.append(new String(buffer));
			}

			env = content.toString();
			in.close();
		} catch (Exception e)
		{
		};

		StringTokenizer s = new StringTokenizer(env, "\r\n");
		count = s.countTokens();

		mSystemEnv = new String[count];
		debugEnv = new String[count + 1];

		String token;

		for (int x = 0; x < count; ++x)
		{
			token = s.nextToken();
			mSystemEnv[x] = token;
			debugEnv[x] = token;
		}

		debugEnv[count] = getPerlDebugEnv(fLaunch);
		return debugEnv;
		//mDebugEnv[count+1] = "PERL5DB=BEGIN {require'perl5db.pl'}";
	}

	static String getPerlDebugEnv(ILaunch fLaunch)
	{
		String port = null;
		String host = null;
		try
		{
			host =  InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};
		
		
		
		 port = PerlLaunchConfigurationConstants.getDebugPort(fLaunch);
		
		return (mDebugOptionsEnvPrefix+host+":"+port+" "+mDebugOptionsValue);
	}

	/**
	 * Returns the shared instance.
	 */
	public static PerlDebugPlugin getDefault()
	{
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace()
	{
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key)
	{
		ResourceBundle bundle =
			PerlDebugPlugin.getDefault().getResourceBundle();
		try
		{
			return bundle.getString(key);
		} catch (MissingResourceException e)
		{
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle()
	{
		return resourceBundle;
	}
	static String getUniqueIdentifier()
	{
		return ("org.epic.debug.perldebugger.perl");
	}

	/**
		 * Logs the specified status with this plug-in's log.
		 *
		 * @param status status to log
		 */
	private static void log(IStatus status)
	{
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
	public static IWorkbenchWindow getActiveWorkbenchWindow()
	{
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	/**
		 * Returns the active workbench shell or <code>null</code> if none
		 *
		 * @return the active workbench shell or <code>null</code> if none
		 */
	public static Shell getActiveWorkbenchShell()
	{
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window == null)
			window = getDefault().getWorkbench().getWorkbenchWindows()[0];
		if (window != null)
		{

			return window.getShell();
		}
		return null;
	}

	public static void log(Throwable e)
	{
		log(new Status(IStatus.ERROR, getUniqueIdentifier(), 0, "Debug Error", e)); //$NON-NLS-1$
	}

	public static void errorDialog(String message)
	{
		errorDialog(message, null);
	}

	public static void errorDialog(String message, IStatus status)
	{

		Shell shell = getActiveWorkbenchShell();
		if (shell != null)
		{
			shell.getDisplay().syncExec(new DisplayErrorThread(shell, message,status));
		}
	}
	
	static class DisplayErrorThread implements Runnable 
	{
					Shell  mShell;
					String mMessage;
					IStatus mStatus;
				
					public DisplayErrorThread(Shell fShell, String fMessage,IStatus fStatus)
					{
						mShell = fShell;
						mMessage = fMessage;
						fStatus = mStatus;
					}
				
					public void run()
					{
						if (mStatus == null)
							MessageDialog.openError(mShell, "EPIG Error", mMessage);
						else
							ErrorDialog.openError(mShell, "Error", mMessage, mStatus); //$NON-NLS-1$

					}
	 }
	
	
	
	
	public static PerlImageDescriptorRegistry getDefaultDesciptorImageRegistry()
	{
		return (defaultPerlImageDescriptorRegistry);

	}

	public static String[] getSystemEnv()
	{
		return (mSystemEnv);
	}

	public static String[] getDebugEnv(ILaunch fLaunch)
	{
		return (createEnvArrays(fLaunch));
	}

	public static PerlBreakpointManager getPerlBreakPointmanager()
	{
		return (mBreakPointmanager);
	}

	public void registerDebugTarget(Target fDebug)
	{
		mDebugger.add(fDebug);
	}

	public void unregisterDebugTarget(Target fDebug)
	{
		mDebugger.remove(fDebug);
	}

	public void registerDebugEventListener(IDebugEventSetListener fListener)
	{
		DebugPlugin.getDefault().addDebugEventListener(fListener);
	}

	public void unregisterDebugEventListener(IDebugEventSetListener fListener)
	{
		DebugPlugin.getDefault().removeDebugEventListener(fListener);
	}

	public void shutdown()
	{
		Target db;
		Iterator i = mDebugger.iterator();
		while (i.hasNext())
		{
			db = (Target) i.next();
			if (db != null)
				db.shutdown(false);
		}
	}
	
	public static String getDefaultDebugPort()
	{
		return mDefaultDebugPort;
	}
	
}
