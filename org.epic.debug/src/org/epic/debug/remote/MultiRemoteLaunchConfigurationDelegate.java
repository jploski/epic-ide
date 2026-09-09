package org.epic.debug.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.Launch;
import org.epic.debug.*;
import org.epic.debug.db.PerlDebugThread;
import org.epic.debug.util.*;

/**
 * Executes launch configurations of type "Perl Remote".
 */
public class MultiRemoteLaunchConfigurationDelegate extends
		LaunchConfigurationDelegate {
	protected void doLaunch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		final CreateMultiRemotePackageJob job = new CreateMultiRemotePackageJob(this, launch);
		if (shouldCreateDebugPackage(launch))
		{
			job.schedule();
		}

		int debugPortNo = Integer.parseInt(getEpicDebuggerPort(launch));
		RemotePort debugPort = new RemotePort("DebugTarget.mDebugPort",
				debugPortNo, debugPortNo);

		if (!debugPort.startConnect()) {
			PerlDebugPlugin.errorDialog("Could not listen on local debug port "
					+ debugPortNo
					+ "\nCheck that it is not bound by another application.");
			launch.terminate();
			return;
		}

		ILaunchConfiguration launchConfig = getLaunchConfiguration();
		if (launchConfig == null) {
			PerlDebugPlugin.errorDialog("Could not create debug configuration Perl remote ");
			launch.terminate();
			return;
		}

		final ILaunchConfiguration iLaunchConfig = launchConfig;
		final ILaunch iLaunch = launch;
		final IProgressMonitor iMonitor = monitor;
		final RemotePort iDebugPort = debugPort;
		final String iMode = mode;
		monitor.done();
		
		new Runnable() {

			public void run() {
				boolean run = true;
				while (run && !iLaunch.isTerminated() && !iMonitor.isCanceled()) {
					if (iDebugPort.waitForConnect(true, false) == RemotePort.WAIT_OK) {
						try{
							String req=iDebugPort.getReadStream().readLine();
							if("CONN".equals(req) || "CONR".equals(req)){
								//client wants to connect
								final int port = RemotePort.findFreePort();
								iDebugPort.getWriteStream().write(port + "\n");
								iDebugPort.getWriteStream().flush();
								final boolean redirect="CONR".equals(req);
								// spin off a new remote debug launch on the new port,
								// then go back to listening on our port for more
								// connections
								new Runnable() {
									public void run() {
										try {
											ILaunchConfigurationWorkingCopy lcw = iLaunchConfig
													.getWorkingCopy();
											lcw.setAttribute(
													PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT,
													port+"");
											lcw.setAttribute( PerlLaunchConfigurationConstants.ATTR_MD5_BREAKS, true);
											lcw.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_CREATE_DEBUG_PACKAGE, false);
											lcw.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST, getEpicDebuggerIP(iLaunch));
											lcw.setAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME, getProjectName(iLaunch));
											lcw.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_CAPTURE_OUTPUT, redirect);
											lcw.doSave().launch(iMode, iMonitor);
										} catch (Exception e) {
										}
									}
								}.run();

							}else if("DNMS".equals(req)){
								//send an mstart_epicDB.pl to the requestor
								iDebugPort.getWriteStream().write(job.getStartScript());
								iDebugPort.getWriteStream().flush();
							}
						}catch(Exception e){
						}finally{
							iDebugPort.startReconnect();
						}
					}

				}
			}
		}.run();

		if (monitor.isCanceled()) {
			launch.terminate();
			debugPort.shutdown();
			return;
		}
		if (launch.isTerminated()) {
			debugPort.shutdown();
			return;
		}
		
		try
        {
            MultiRemoteDebugTarget target = new MultiRemoteDebugTarget(
                launch, debugPort);
            launch.addDebugTarget(target);
        }
        catch (CoreException e)
        {
            launch.terminate();
            if (e.getStatus().getCode() != DebugTarget.SESSION_TERMINATED)
                throw e;
        }
	}

	protected IProject getProject(ILaunch launch) throws CoreException {
		return super.getProject(launch);
	}

	File getDebugPackageFile(ILaunch launch) throws CoreException
	{
		 return new File(new File(
		 launch.getLaunchConfiguration().getAttribute(
		 PerlLaunchConfigurationConstants.ATTR_REMOTE_DEBUG_PACKAGE_PATH,
		 "")).getParentFile(), "mstart_epicDB.pl");
	}

	String getProjectName(ILaunch launch) throws CoreException {
		return launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
	}

	String getEpicDebuggerIP(ILaunch launch) throws CoreException {
		return launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST, "");
	}

	String getEpicDebuggerPort(ILaunch launch) throws CoreException {
		return launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT, "");
	}
	
	boolean getShouldRedirectIO(ILaunch launch) throws CoreException {
		return launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE_CAPTURE_OUTPUT, true);
	}
	
	String getRemoteProjectDir(ILaunch launch) throws CoreException {
		return md5Breaks(launch) ? "" : launch.getLaunchConfiguration()
				.getAttribute(
						PerlLaunchConfigurationConstants.ATTR_REMOTE_DEST, "");
	}

	Path getScriptPath(ILaunch launch) throws CoreException {
		return new Path(launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE, ""));
	}

	private boolean shouldCreateDebugPackage(ILaunch launch)
			throws CoreException {
		return launch
				.getLaunchConfiguration()
				.getAttribute(
						PerlLaunchConfigurationConstants.ATTR_REMOTE_CREATE_DEBUG_PACKAGE,
						true);
	}

	boolean md5Breaks(ILaunch launch) throws CoreException {
		return launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_MD5_BREAKS, false);
	}

	ILaunchConfiguration getLaunchConfiguration() {
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault()
					.getLaunchManager().getLaunchConfigurations();
			for (int i = 0; i < configs.length; i++) {
				ILaunchConfiguration config = configs[i];
				if (config.getName().equals("PerlMultiDontUse"))
					return config;
			}
			ILaunchConfigurationType[] configTypes = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes();
			for (ILaunchConfigurationType configType:configTypes){
				if(configType.getName().equals("Perl Remote")){
					ILaunchConfigurationWorkingCopy config=configType.newInstance(null, "PerlMultiDontUse");
					return config.doSave();
				}
			}
		} catch (CoreException e) {
			PerlDebugPlugin.log(e);
		}
		return null;
	}

	static int launchNum = 0;
}
