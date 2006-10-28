package org.epic.debug;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.epic.core.util.PerlExecutableUtilities;
import org.epic.core.util.PerlExecutor;
import org.epic.debug.ui.PerlImageDescriptorRegistry;
import org.epic.debug.util.LogWriter;
import org.osgi.framework.BundleContext;

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
	public PerlDebugPlugin() {

		plugin = this;
		try {
			resourceBundle = ResourceBundle
					.getBundle("org.epic.debug.DebugPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
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

		IStatus status, result;
		MultiStatus multiStatus;

		if (fException != null) {
            
            if (fException instanceof CoreException)
            {
                multiStatus = new MultiStatus(
                    getUniqueIdentifier(),
                    fSeverity,
                    fText,
                    null);
                
                multiStatus.add(((CoreException) fException).getStatus());
                result = multiStatus;
            }
            else
            {
                // TODO I doubt the code below qualifies as correct use of MultiStatus...                
    			ByteArrayOutputStream out = new ByteArrayOutputStream();
    			PrintWriter swr = new PrintWriter(out);
    			fException.printStackTrace(swr);
    			String buf = (fException.getMessage() + "\n" + out.toString());
    			StringTokenizer tok = new StringTokenizer(buf, "\n");
    			multiStatus = new MultiStatus(
                    getUniqueIdentifier(), fSeverity, fText, /*fException*/null);
    			while (tok.hasMoreElements()) {
    				status = new Status(
                        fSeverity, getUniqueIdentifier(), 0, tok.nextToken(), null);
    				multiStatus.add(status);
    			}
    
    			result = multiStatus;
            }
		} else {
			result = new Status(fSeverity, getUniqueIdentifier(), 0, fText,
					fException);
		}
		log(new Status(fSeverity, getUniqueIdentifier(), 0, fText, fException));
		errorDialog(fText, result);
	}
    
    private static List runHelperScript(String scriptName) throws CoreException
    {
        PerlExecutor executor = new PerlExecutor();
        try
        {
            File scriptFile = PerlDebugPlugin.getDefault().extractTempFile(
                scriptName, null);
            List args = new ArrayList(1);
            args.add(scriptFile.getAbsolutePath());
            return executor.execute(
                scriptFile.getParentFile(),
                args,
                "").getStdoutLines();
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "extractTempFile failed on " + scriptName,
                e));
        }
        finally { executor.dispose(); }
    }

	static String[] createEnvArrays(Target fTarget) throws CoreException
    {
        List outputLines = runHelperScript("get_env.pl");

        mSystemEnv = (String[]) outputLines.toArray(new String[outputLines.size()]);
        
        if (fTarget instanceof DebugTarget)
            outputLines.add(getPerlDebugEnv((DebugTarget) fTarget));

        return (String[]) outputLines.toArray(new String[outputLines.size()]);
	}
	
	public static void createDefaultIncPath(List fInc) throws CoreException {
        fInc.addAll(runHelperScript("get_inc.pl"));
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

	public static String getUniqueIdentifier()
    {
	    PerlDebugPlugin plugin = getDefault();
	    return plugin != null ? plugin.getBundle().getSymbolicName() : "org.epic.debug";
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
				MessageDialog.openError(mShell, "EPIC Error", mMessage);
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

	public static String[] getDebugEnv(Target fTarget) throws CoreException {
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

    /**
     * Extracts a file from the plug-in archive (or installation
     * location) to a temporary location.
     * 
     * @param src   file name within the archive
     * @param dest  file name in the temporary location or null
     *              if the file should retain its original name
     * @return path to the extracted file
     */
    public File extractTempFile(String src, String dest)
        throws IOException
	{
        File destFile = new File(
            getStateLocation().toString(),
            dest != null ? dest : src);
			
        InputStream in = null;
        OutputStream out = null;

		try
		{
            in = getBundle().getEntry(src).openStream();
            out = new FileOutputStream(destFile);
            
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            
            return destFile;
        }
        finally
		{
            if (in != null) try { in.close(); } catch (Exception e) { }
            if (out != null) try { out.close(); } catch (Exception e) { }
		}
	}

    /**
     * @return path to a directory containing internal EPIC modules
     *         that need to be accessible through an executed script's
     *         include path while in debug mode; the returned path is
     *         ready to be passed as a value of "-I" to the interpreter
     */
    public String getInternalDebugInc() throws CoreException
    {
        File dumpvarFile;
        try
        {
            PerlDebugPlugin.getDefault().extractTempFile(
                "autoflush_epic.pm", null);
            
            dumpvarFile = PerlDebugPlugin.getDefault().extractTempFile(
                "dumpvar_epic.pm", null);
            
            return PerlExecutableUtilities.resolveIncPath(
                dumpvarFile.getParentFile().getAbsolutePath());
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "extractTempFile failed on dumpvar_epic.pm",
                e));
        }
    }
    
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        
        mBreakPointmanager = new PerlBreakpointManager(DebugPlugin.getDefault());
        mDebugger = new ArrayList();

        getLog().addLogListener(
                new LogWriter(new File(getStateLocation() + File.separator
                        + ".log"), mLogLevel));
        getLog().addLogListener(new LogWriter(System.err, mScreenLogLevel));
    }
}