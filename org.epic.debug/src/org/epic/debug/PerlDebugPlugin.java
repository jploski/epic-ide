package org.epic.debug;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.epic.core.util.PerlExecutableUtilities;
import org.epic.core.util.PerlExecutor;
import org.epic.debug.ui.PerlImageDescriptorRegistry;
import org.epic.debug.ui.action.VariablesViewActionDelegate;
import org.epic.debug.util.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.osgi.framework.BundleContext;

public class PerlDebugPlugin extends AbstractUIPlugin
{
    final private static int mScreenLogLevel = 0;

    final private static int mLogLevel = Status.WARNING;

    final private static String mDefaultDebugPort = "4444";

    // The shared instance.
    private static PerlDebugPlugin plugin;

    // Resource bundle.
    private ResourceBundle resourceBundle;

    private static String mSystemEnv[];

    private static PerlImageDescriptorRegistry defaultPerlImageDescriptorRegistry = new PerlImageDescriptorRegistry();

    private final static String mDebugOptionsEnvPrefix = "PERLDB_OPTS=RemotePort=";

    private final static String mDebugOptionsValue = "DumpReused ReadLine=0 PrintRet=0";

    /**
     * This little piece of magic makes perl5db.pl compatible with
     * epic_breakpoints.pm (and avoid stepping into epic_breakpoints, too).
     */
    private static final String EPIC_BREAKPOINTS_PATCH =
        "{ use epic_breakpoints; my $osingle = $single; $single = 0; " +
        "$single = epic_breakpoints::_postponed($filename, $line) || $osingle; }\n";

    public PerlDebugPlugin()
    {
        plugin = this;
        try
        {
            resourceBundle = ResourceBundle
                .getBundle("org.epic.debug.DebugPluginResources");
        }
        catch (MissingResourceException x)
        {
            resourceBundle = null;
        }
    }

    public static void createDefaultIncPath(List<String> fInc) throws CoreException
    {
        fInc.addAll(runHelperScript("get_inc.pl"));
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
            shell.getDisplay().syncExec(
                new DisplayErrorThread(shell, message, status));
        }
    }

    /**
     * Extracts a file from the plug-in archive (or installation location) to a
     * temporary location.
     * 
     * @param src
     *            file name within the archive
     * @param dest
     *            file name in the temporary location or null if the file should
     *            retain its original name
     * @return path to the extracted file
     */
    public File extractTempFile(String src, String dest) throws IOException
    {
        File destFile = new File(getStateLocation().toString(),
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
            while ((len = in.read(buf)) > 0)
                out.write(buf, 0, len);
    
            return destFile;
        }
        finally
        {
            if (in != null) try
            {
                in.close();
            }
            catch (Exception e)
            {
            }
            if (out != null) try
            {
                out.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     * Loads perl5db.pl from the local Perl distribution and patches
     * it to make it compatible with epic_breakpoints.pm. Saves the
     * patched copy to a location from where it will be picked up
     * by "perl -d".
     * 
     * @return on success, file with the patched perl5db.pl
     */
    public File patchPerl5Db() throws IOException, CoreException
    {
        List<String> inc = new ArrayList<String>();
        createDefaultIncPath(inc);
        
        String interpreterType = PerlEditorPlugin.getDefault()
            .getPreferenceStore().getString(
                PreferenceConstants.DEBUG_INTERPRETER_TYPE);

        IPathMapper mapper;        
        if (PreferenceConstants.DEBUG_INTERPRETER_TYPE_CYGWIN.equals(interpreterType))
            mapper = new CygwinPathMapper();
        else
            mapper = new NullPathMapper();
        
        File perl5DbFile = null;
        StringBuffer searchPath = new StringBuffer();
        for (Iterator<String> i = inc.iterator(); i.hasNext();)
        {
            File dir = mapper.getEpicPath(
                new Path((String) i.next())).toFile();

            if (searchPath.length() > 0) searchPath.append(File.pathSeparatorChar);
            searchPath.append(dir.getAbsolutePath());

            File f = new File(dir, "perl5db.pl");
            if (f.exists()) { perl5DbFile = f; break; }
        }

        if (perl5DbFile == null)
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Fatal: failed to find source perl5db.pl for epic_breakpoints.pm (searched in " + searchPath + ")",
                null));

        // Note: we do not use String.replaceAll because of bug 1734045
        String perl5db = loadScript("perl5db.pl", new FileInputStream(perl5DbFile));
        String marker = "return unless $postponed_file{$filename};";
        int i = perl5db.indexOf(marker);
        if (i == -1) throw new CoreException(new Status(
            IStatus.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            "Fatal: failed to patch perl5db.pl for epic_breakpoints.pm",
            null));

        StringBuffer newPerl5Db = new StringBuffer();
        newPerl5Db.append(perl5db.substring(0, i));
        newPerl5Db.append(EPIC_BREAKPOINTS_PATCH);
        newPerl5Db.append(perl5db.substring(i));

        File destFile = new File(getStateLocation().toString(), "perl5db.pl");
        BufferedOutputStream out = null;
        try
        {
            out = new BufferedOutputStream(new FileOutputStream(destFile));
            out.write(newPerl5Db.toString().getBytes("ISO-8859-1"));
            return destFile;
        }
        finally
        {
            if (out != null) try { out.close(); } catch (IOException e) { }
        }
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

    /**
     * Returns the active workbench window
     * 
     * @return the active workbench window
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow()
    {
        return getDefault().getWorkbench().getActiveWorkbenchWindow();
    }

    public static String[] getDebugEnv(ILaunch launch, int debugPort)
        throws CoreException
    {
        String[] env = DebugPlugin.getDefault().getLaunchManager()
            .getEnvironment(launch.getLaunchConfiguration());
    
        if (env == null) // nothing set up in CGI env tab => use standard env
        {
            env = readNativeEnv();
        }
        if (launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE))
        {
            List<String> envList = new ArrayList<String>(Arrays.asList(env));
            envList.add(getPerlDebugEnv(debugPort));
            env = envList.toArray(new String[envList.size()]);
        }
        return env;
    }

    /**
     * Returns the shared instance.
     */
    public static PerlDebugPlugin getDefault()
    {
        return plugin;
    }

    public static String getDefaultDebugPort()
    {
        return mDefaultDebugPort;
    }

    public static PerlImageDescriptorRegistry getDefaultDesciptorImageRegistry()
    {
        return defaultPerlImageDescriptorRegistry;
    
    }

    /**
     * @return path to a directory containing internal EPIC modules that need to
     *         be accessible through an executed script's include path while in
     *         debug mode; the returned path is ready to be passed as a value of
     *         "-I" to the interpreter
     */
    public String getInternalDebugInc() throws CoreException
    {
        File dumpvarFile;
        try
        {
            extractTempFile("autoflush_epic.pm", null);
            extractTempFile("epic_breakpoints.pm", null);
            patchPerl5Db();
            dumpvarFile = extractTempFile("dumpvar_epic.pm", null);
    
            return PerlExecutableUtilities.resolveIncPath(dumpvarFile
                .getParentFile().getAbsolutePath());
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(IStatus.ERROR, PerlDebugPlugin
                .getUniqueIdentifier(), IStatus.OK,
                "extractTempFile failed on dumpvar_epic.pm", e));
        }
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle()
    {
        return resourceBundle;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     */
    public static String getResourceString(String key)
    {
        ResourceBundle bundle = PerlDebugPlugin.getDefault()
            .getResourceBundle();
        try
        {
            return bundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return key;
        }
    }

    public static String[] getSystemEnv()
    {
        return (mSystemEnv);
    }

    public static String getUniqueIdentifier()
    {
        PerlDebugPlugin plugin = getDefault();
        return plugin != null ? plugin.getBundle().getSymbolicName()
            : "org.epic.debug";
    }

    public static IWorkbenchWindow getWorkbenchWindow()
    {
        IWorkbenchWindow window = getDefault().getWorkbench()
            .getActiveWorkbenchWindow();
        if (window == null)
            window = getDefault().getWorkbench().getWorkbenchWindows()[0];
        return window;
    }

    /**
     * Returns the workspace instance.
     */
    public static IWorkspace getWorkspace()
    {
        return ResourcesPlugin.getWorkspace();
    }

    /**
     * @return the source text of the requested helper script
     */
    public String loadHelperScript(String scriptName) throws CoreException
    {
        try
        {
            return loadScript(scriptName, getBundle().getEntry(scriptName).openStream());
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not find helper script " + scriptName,
                e));
        }
    }

    /**
     * Logs the specified status with this plug-in's log.
     * 
     * @param status
     *            status to log
     */
    public static void log(IStatus status)
    {
        getDefault().getLog().log(status);
        Throwable e = status.getException();
        if (e != null) e.printStackTrace();
    }

    public static void log(Throwable e)
    {
        log(new Status(IStatus.ERROR, getUniqueIdentifier(), 0,
            "Debug Error", e)); //$NON-NLS-1$
    }

    public void logInfo(String fText)
    {
        log(IStatus.INFO, fText, null);
    }

    public void logInfo(String fText, Exception fException)
    {
        log(IStatus.INFO, fText, fException);
    }

    public void logOK(String fText)
    {
        log(IStatus.OK, fText, null);
    }

    public void logOK(String fText, Exception fException)
    {
        log(IStatus.OK, fText, fException);
    }

    public void logWarning(String fText)
    {
        log(IStatus.WARNING, fText, null);
    }

    public void logWarning(String fText, Exception fException)
    {
        log(IStatus.WARNING, fText, fException);
    }

    public void logError(String fText)
    {
        log(IStatus.ERROR, fText, null);
    }

    public void logError(String fText, Exception fException)
    {
        log(IStatus.ERROR, fText, fException);
    }

    public void start(BundleContext context) throws Exception
    {
        super.start(context);
    
        getLog().addLogListener(
            new LogWriter(
                new File(getStateLocation() + File.separator + ".log"),
                mLogLevel));
        getLog().addLogListener(new LogWriter(System.err, mScreenLogLevel));

        // The enablement of the "Show xxx variables" actions in the Variables
        // view depends on our pluginState. Unfortunately, for some reason
        // they do not become automatically enabled after the plug-in is loaded.
        // So we enable them here as a workaround...
        VariablesViewActionDelegate.enableVariablesViewActions();
    }

    private static String getPerlDebugEnv(int debugPort)
    {
        String host = null;
    
        try
        {
            host = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            log(e);
            host = "127.0.0.1";
        }
        return mDebugOptionsEnvPrefix + host + ":" + debugPort + " "
            + mDebugOptionsValue;
    }
    
    private String loadScript(String scriptName, InputStream inStream)
        throws CoreException
    {
        BufferedReader in = null;
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
    
        try
        {
            in = new BufferedReader(new InputStreamReader(inStream, "ISO-8859-1"));
    
            String line;
            while ((line = in.readLine()) != null)
                out.println(line);
            out.close();
            return sw.toString();
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not load script " + scriptName,
                e));
        }
        finally
        {
            if (in != null) try
            {
                in.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    private void log(int fSeverity, String fText, Exception fException)
    {
        IStatus status, result;
        MultiStatus multiStatus;
    
        if (fException != null)
        {
            if (fException instanceof CoreException)
            {
                multiStatus = new MultiStatus(getUniqueIdentifier(), fSeverity,
                    fText, null);
    
                multiStatus.add(((CoreException) fException).getStatus());
                result = multiStatus;
            }
            else
            {
                // TODO I doubt the code below qualifies as correct use of
                // MultiStatus...
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter swr = new PrintWriter(out);
                fException.printStackTrace(swr);
                String buf = (fException.getMessage() + "\n" + out.toString());
                StringTokenizer tok = new StringTokenizer(buf, "\n");
                multiStatus = new MultiStatus(getUniqueIdentifier(), fSeverity,
                    fText, /* fException */null);
                while (tok.hasMoreElements())
                {
                    status = new Status(fSeverity, getUniqueIdentifier(), 0,
                        tok.nextToken(), null);
                    multiStatus.add(status);
                }
    
                result = multiStatus;
            }
        }
        else
        {
            result = new Status(fSeverity, getUniqueIdentifier(), 0, fText,
                fException);
        }
        log(new Status(fSeverity, getUniqueIdentifier(), 0, fText, fException));
        errorDialog(fText, result);
    }

    private static String[] readNativeEnv() throws CoreException
    {
        // Use a random marker (current time) to increase
        // the likelihood of properly parsing environment
        // variables with multi-line values

        String marker = System.currentTimeMillis() + " ";
        List<String> lines = runHelperScript("get_env.pl", Arrays
            .asList(new String[] { marker }));
        List<String> envList = new ArrayList<String>();

        StringBuffer buf = new StringBuffer();
        for (Iterator<String> i = lines.iterator(); i.hasNext();)
        {
            String line = i.next();

            if (!line.startsWith(marker)) // continuation
            {
                buf.append(System.getProperty("line.separator"));
                buf.append(line);
            }
            else
            {
                if (buf.length() > 0) envList.add(buf.toString());
                buf.setLength(0);
                buf.append(line.substring(marker.length()));
            }
        }
        if (buf.length() > 0) envList.add(buf.toString());
        return envList.toArray(new String[envList.size()]);
    }

    private static List<String> runHelperScript(String scriptName) throws CoreException
    {
        return runHelperScript(scriptName, Collections.<String>emptyList());
    }

    private static List<String> runHelperScript(String scriptName, List<String> scriptArgs)
        throws CoreException
    {
        PerlExecutor executor = new PerlExecutor();
        try
        {
            File scriptFile = PerlDebugPlugin.getDefault().extractTempFile(
                scriptName, null);
            List<String> args = new ArrayList<String>(1);
            args.add(scriptFile.getAbsolutePath());
            args.addAll(scriptArgs);
            return executor.execute(scriptFile.getParentFile(), args, "")
                .getStdoutLines();
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(IStatus.ERROR, PerlDebugPlugin
                .getUniqueIdentifier(), IStatus.OK,
                "extractTempFile failed on " + scriptName, e));
        }
        finally
        {
            executor.dispose();
        }
    }

    private static class DisplayErrorThread implements Runnable
    {
        Shell mShell;

        String mMessage;

        IStatus mStatus;

        public DisplayErrorThread(Shell fShell, String fMessage, IStatus fStatus)
        {
            mShell = fShell;
            mMessage = fMessage;
            mStatus = fStatus;
        }

        public void run()
        {
            if (mStatus == null) MessageDialog.openError(mShell, "EPIC Error",
                mMessage);
            else ErrorDialog.openError(mShell, "Error", null, mStatus); //$NON-NLS-1$

        }
    }
}