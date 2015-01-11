package org.epic.debug.local;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.console.IConsole;
import org.epic.core.PerlCore;
import org.epic.core.util.PerlExecutableUtilities;
import org.epic.debug.DebugTarget;
import org.epic.debug.LaunchConfigurationDelegate;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;
import org.epic.debug.PerlTarget;
import org.epic.debug.util.ExecutionArguments;
import org.epic.debug.util.RemotePort;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Executes launch configurations of type "Perl Local".
 */
public class LocalLaunchConfigurationDelegate
    extends LaunchConfigurationDelegate
{   
    protected void doLaunch(
        ILaunchConfiguration configuration,
        String mode,
        ILaunch launch,
        IProgressMonitor monitor) throws CoreException
    {
        try
        {
            PerlTarget target =
                isDebugMode(launch)
                ? startDebugTarget(configuration, launch, monitor)
                : startRunTarget(configuration, launch, monitor);
            
            launch.addDebugTarget(target);
        }
        catch (CoreException e)
        {
            launch.terminate();
            if (e.getStatus().getCode() != DebugTarget.SESSION_TERMINATED)
                throw e;
        }
    }
    
    public boolean preLaunchCheck(
        ILaunchConfiguration configuration,
        String mode,
        IProgressMonitor monitor) throws CoreException
    {
        if (!PerlEditorPlugin.getDefault().requirePerlInterpreter(true))
        {
            return false;
        }
        else return super.preLaunchCheck(configuration, mode, monitor);
    }
    
    private String[] createCommandLine(ILaunch launch)
        throws CoreException
    {   
        ILaunchConfiguration configuration = launch.getLaunchConfiguration();
        
        String perlParams =
            configuration.getAttribute(
                PerlLaunchConfigurationConstants.ATTR_PERL_PARAMETERS,
                "");
        perlParams = VariablesPlugin.getDefault().getStringVariableManager()
            .performStringSubstitution(perlParams);

        String progParams =
            configuration.getAttribute(
                PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
                "");
        progParams = VariablesPlugin.getDefault().getStringVariableManager()
            .performStringSubstitution(progParams);

        boolean consoleOutput = configuration.getAttribute(
            IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true);        
    
        List<String> fCmdList = PerlExecutableUtilities.getPerlCommandLine(
            PerlCore.create(getProject(launch)));
    
        fCmdList.add("-I" + PerlDebugPlugin.getDefault().getInternalDebugInc());
        
        // Add absolute path to local working directory to make
        // perl -d refer to modules in the same directory by their
        // absolute rather than relative paths (relevant when setting
        // breakpoints).
        fCmdList.add("-I" + PerlExecutableUtilities.resolveIncPath(
            getLocalWorkingDir(launch).toFile().getAbsolutePath()));
            
        if (launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE))
        {
            fCmdList.add("-d");
        }
        if (PerlEditorPlugin.getDefault().getBooleanPreference(
            PreferenceConstants.DEBUG_SHOW_WARNINGS))
        {
            fCmdList.add("-w");
        }
        if (PerlEditorPlugin.getDefault().getBooleanPreference(
            PreferenceConstants.DEBUG_TAINT_MODE))
        {
            fCmdList.add("-T");
        }        
        if (consoleOutput)
        {
            fCmdList.add("-Mautoflush_epic");
        }
        if (perlParams != null && perlParams.length() > 0)
        {
            ExecutionArguments exArgs = new ExecutionArguments(perlParams);
            fCmdList.addAll(exArgs.getProgramArgumentsL());
        }
        
        if (isCygwin())
        {
            IPath cygwinPath = getPathMapper(launch).getDebuggerPath(
                getScriptPath(launch), null);
            
            if (cygwinPath != null) fCmdList.add(cygwinPath.toString());
            else throw new CoreException(new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                MessageFormat.format(
                    "Could not translate path {0} into a Cygwin path.\n" +
                    "Make sure your Cygwin mounts are configured properly.",
                    new String[] { getScriptPath(launch).toOSString() }),
                null
                ));
        }
        else
            fCmdList.add(getScriptPath(launch).toString());
        
        if (progParams != null && progParams.length() > 0)
        {
            ExecutionArguments exArgs = new ExecutionArguments(progParams);
            fCmdList.addAll(exArgs.getProgramArgumentsL());
        }
    
        return fCmdList.toArray(new String[fCmdList.size()]);
    }
    
    private void dumpLaunchDetails(        
        String[] cmdParams,
        String[] env,
        IPath workingDir)
    {
        if (!PerlEditorPlugin.getDefault().getBooleanPreference(
            PreferenceConstants.DEBUG_DEBUG_CONSOLE))
        {
            return;
        }

        StringBuffer buf = new StringBuffer("Starting Perl debugger:\n");
        buf.append("Command line:\n");
        for (int i = 0; i < cmdParams.length; i++)
        {
            buf.append(cmdParams[i]);
            buf.append('\n');
        }
        buf.append("Working directory: ");
        buf.append(workingDir.toFile().getAbsolutePath());
        buf.append("\nEnvironment:\n");
        for (int i = 0; i < env.length; i++)
        {
            buf.append(env[i]);
            buf.append('\n');
        }
        ILog log = PerlDebugPlugin.getDefault().getLog();
        log.log(new Status(
            IStatus.INFO,
            PerlDebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            buf.toString(),
            null));
    }
    
    /**
     * Returns the default working directory used when none is specified
     * in the launch configuration - the parent directory of the script. 
     */
    private File getDefaultWorkingDir(ILaunch launch) throws CoreException
    {
        return getScriptPath(launch).removeLastSegments(1).toFile();
    }
    
    private IPath getLocalWorkingDir(ILaunch launch) throws CoreException
    {
        try
        {        
            File workingDir = verifyWorkingDirectory(
                launch.getLaunchConfiguration());
            if (workingDir == null) workingDir = getDefaultWorkingDir(launch);        
            return Path.fromOSString(workingDir.getAbsolutePath());
        }
        catch (CoreException e)        
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not start Perl interpreter: invalid working directory",
                e);
            throw e;
        }
    }
    
    private IPath getScriptPath(ILaunch launch) throws CoreException
    {
        String scriptFile =
            launch.getLaunchConfiguration().getAttribute(
                PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
                "");
    
        return getProject(launch).getFile(new Path(scriptFile)).getLocation();
    }
    
    /**
     * Returns the working directory path specified by the given launch
     * configuration, or <code>null</code> if none.
     * 
     * @param configuration  launch configuration
     * @return the working directory path specified by the given launch
     *         configuration, or <code>null</code> if none
     * @exception CoreException
     *            if unable to retrieve the attribute
     */
    private IPath getWorkingDirectoryPath(ILaunchConfiguration configuration) throws CoreException
    {
        String path = configuration.getAttribute(
            PerlLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
            (String) null);

        if (path == null) return null;
        else
        {
            path = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(path);
            return new Path(path);
        }
    }
    
    private Process startPerlInterpreter(
        String[] cmdParams,
        String[] env,
        IPath workingDir) throws CoreException
    {
        try
        {
            Process perlProcess =
                Runtime.getRuntime().exec(
                    cmdParams,
                    env,
                    workingDir.toFile());    
            
            return perlProcess;
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Failed to launch Perl interpreter process; " +
                "inspect log for details",
                e));
        }
    }

    private PerlTarget startDebugTarget(
        ILaunchConfiguration configuration,
        ILaunch launch,
        IProgressMonitor monitor) throws CoreException
    {
        RemotePort debugPort = new RemotePort("DebugTarget.mDebugPort");
        debugPort.startConnect();
        
        IProcess process = startPerlProcess(
            launch, "Perl Debugger", debugPort.getServerPort());

        if (debugPort.waitForConnect(true) != RemotePort.WAIT_OK)
        {
            PerlDebugPlugin.errorDialog(getTimeoutErrorMessage(process));
            launch.terminate();
            return null;
        }
        else
        {            
            return new DebugTarget(
                launch, process, debugPort, getPathMapper(launch));
        }
    }
    
    /**
     * Provides some troubleshooting hints in addition to the generic "timed out" message
     * by examining additional error messages found in the console.
     */
    private String getTimeoutErrorMessage(IProcess process)
    {
    	IConsole console = DebugUIPlugin.getDefault().getProcessConsoleManager().getConsole(process);
    	if (console instanceof ProcessConsole)
    	{
            String consoleContents = ((ProcessConsole) console).getDocument().get();
            if (consoleContents.indexOf("Use of uninitialized value in subroutine dereference at (null) line 1.") != -1 &&
                consoleContents.indexOf("perl5db.pl did not return a true value.") != -1)
            {
                return "Timed out while waiting for Perl debugger connection. " +
                	"The most likely reason is a broken version of PathTools in your Perl installation. " + 
                	"You can fix this problem manually by editing a single line in Cwd.pm, as suggested " + 
                	"in EPIC bug report 2907155 at SourceForge.";
            }
    	}
    	return "Timed out while waiting for Perl debugger connection.";
	}

	private IProcess startPerlProcess(
        ILaunch launch, String processName, int debugPort) throws CoreException
    {        
        String[] cmdParams = createCommandLine(launch);        
        String[] env = PerlDebugPlugin.getDebugEnv(launch, debugPort);
        IPath workingDir = getLocalWorkingDir(launch);
                
        dumpLaunchDetails(cmdParams, env, workingDir);
        Process perlProcess =
            startPerlInterpreter(cmdParams, env, workingDir);
        
        Map<String, String> attr = new HashMap<String, String>(1);
        attr.put(
            IProcess.ATTR_PROCESS_TYPE,
            PerlLaunchConfigurationConstants.PERL_PROCESS_TYPE);
        
        return DebugPlugin.newProcess(
            launch,
            perlProcess,
            processName,
            attr);
    }
    
    private PerlTarget startRunTarget(
        ILaunchConfiguration configuration,
        ILaunch launch,
        IProgressMonitor monitor) throws CoreException
    {
        IProcess process = startPerlProcess(launch, "Perl Interpreter", -1);
        return new RunLocalTarget(launch, process);
    }
    
    /**
     * Verifies the working directory specified by the given launch
     * configuration exists, and returns the working directory, or
     * <code>null</code> if none is specified.
     * 
     * @param configuration  launch configuration
     * @return the working directory specified by the given launch
     *         configuration, or <code>null</code> if none
     * @exception CoreException if unable to retrieve the attribute
     */
    private File verifyWorkingDirectory(ILaunchConfiguration configuration)
        throws CoreException
    {
        IPath path = getWorkingDirectoryPath(configuration);
        if (path == null) return null;
        
        if (path.isAbsolute())
        {
            File dir = new File(path.toOSString());
            if (dir.isDirectory()) return dir;
        }
        
        // If we get here, we assume that the entered path is workspace-relative.
        // This is true for paths that do not start with slash, but also for
        // paths that start with slash to which some variables may evaluate.
        // In any case, we try to locate the working directory in the workspace.

        IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if (res instanceof IContainer && res.exists())
            return res.getLocation().toFile();

        throw new CoreException(
            new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                MessageFormat.format(
                    "Working directory does not exist: {0}",
                    new String[] { path.toString() }),
                null
                ));
    }
}
