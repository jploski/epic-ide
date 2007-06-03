package org.epic.debug.remote;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.epic.debug.*;
import org.epic.debug.db.PerlDebugThread;
import org.epic.debug.util.*;

/**
 * Executes launch configurations of type "Perl Remote".
 */
public class RemoteLaunchConfigurationDelegate
    extends LaunchConfigurationDelegate
{
    protected void doLaunch(
        ILaunchConfiguration configuration,
        String mode,
        ILaunch launch,
        IProgressMonitor monitor) throws CoreException
    {
        RemotePathMapper mapper = new RemotePathMapper(
            getProject(launch),
            getRemoteProjectDir(launch));
        
        if (shouldCreateDebugPackage(launch))
        {
            Job job = new CreateRemotePackageJob(this, launch, mapper);
            job.schedule();
        }
        
        int debugPortNo = Integer.parseInt(getEpicDebuggerPort(launch));
        RemotePort debugPort = new RemotePort(
            "DebugTarget.mDebugPort", debugPortNo, debugPortNo);
        
        if (!debugPort.startConnect())
        {
            PerlDebugPlugin.errorDialog(
                "Could not listen on local debug port " + debugPortNo +
                "\nCheck that it is not bound by another application.");
            launch.terminate();
            return;
        }
        
        DebuggerProxy process = new DebuggerProxy(
            "Remote Debugger", launch, getEpicDebuggerIP(launch));               
        
        launch.addProcess(process);
        monitor.subTask("Waiting for debugger connection...");
        
        while (
            !launch.isTerminated() &&
            debugPort.waitForConnect(true, false) != RemotePort.WAIT_OK &&
            !monitor.isCanceled());

        if (monitor.isCanceled())
        {
            launch.terminate();
            debugPort.shutdown();
            return;
        }
        if (launch.isTerminated())
        {
            debugPort.shutdown();
            return;
        }
        
        try
        {
            RemoteDebugTarget target = new RemoteDebugTarget(
                launch, process, debugPort, mapper);
            launch.addDebugTarget(target);
            process.init((PerlDebugThread) target.getThreads()[0]);
        }
        catch (CoreException e)
        {
            launch.terminate();
            if (e.getStatus().getCode() != DebugTarget.SESSION_TERMINATED)
                throw e;
        }
    }
    
    protected IProject getProject(ILaunch launch) throws CoreException
    {
        return super.getProject(launch);
    }
    
    File getDebugPackageFile(ILaunch launch) throws CoreException
    {
        return new File(
            launch.getLaunchConfiguration().getAttribute(
                PerlLaunchConfigurationConstants.ATTR_REMOTE_DEBUG_PACKAGE_PATH,
                ""));
    }
    
    String getEpicDebuggerIP(ILaunch launch) throws CoreException
    {
        return launch.getLaunchConfiguration().getAttribute(
            PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST, "");
    }
    
    String getEpicDebuggerPort(ILaunch launch) throws CoreException
    {
        return launch.getLaunchConfiguration().getAttribute(
            PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT, "");
    }
    
    String getRemoteProjectDir(ILaunch launch) throws CoreException
    {
        return launch.getLaunchConfiguration().getAttribute(
            PerlLaunchConfigurationConstants.ATTR_REMOTE_DEST, "");
    }
    
    Path getScriptPath(ILaunch launch) throws CoreException
    {
        return new Path(launch.getLaunchConfiguration().getAttribute(
            PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE, ""));
    }
    
    private boolean shouldCreateDebugPackage(ILaunch launch) throws CoreException
    {
        return launch.getLaunchConfiguration().getAttribute(
            PerlLaunchConfigurationConstants.ATTR_REMOTE_CREATE_DEBUG_PACKAGE,
            true);
    }
}
