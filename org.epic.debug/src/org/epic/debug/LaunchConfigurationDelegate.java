package org.epic.debug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.*;
import org.epic.core.Constants;
import org.epic.core.Perspective;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class LaunchConfigurationDelegate
	extends org.eclipse.debug.core.model.LaunchConfigurationDelegate
{
	private ILaunchConfiguration mLaunchConfiguration;
	private Target mTarget;
    private ILaunch mLaunch;

	/**
	 * Convenience method to get the launch manager.
	 * 
	 * @return the launch manager
	 */
	protected ILaunchManager getLaunchManager()
	{
		return DebugPlugin.getDefault().getLaunchManager();
	}

	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode)
        throws CoreException
    {
		return null;
	}

	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor) throws CoreException
	{
		mLaunchConfiguration = configuration;
		mLaunch = launch;

		System.out.println("Launch: " + mLaunchConfiguration.getLocation());
        
        launch.setSourceLocator(new SourceLocator());

		String cgi =
			launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
				((String) null));

		String remote =
			launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE,
				((String) null));

		if (remote != null) launchRemote();           
        else if (cgi != null) launchCGI();
        else launchLocal();
        
        // Switch to Debug Perspective
        if (launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE))
            Perspective.switchPerspective(Constants.DEBUG_PERSPECTIVE_ID);
	}      

	/**
	 * Returns the set of projects to use when searching for errors or <code>null</code> 
	 * if no search is to be done.  
	 * 
	 * @param projects the list of projects to sort into build order
	 * @return a list of projects.
	 */
	protected IProject[] getProjectsForProblemSearch(
        ILaunchConfiguration configuration,
        String mode) throws CoreException
    {
        try
		{
			String prjName =
				configuration.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					(String)null);

            IProject p[] = new IProject[1];
            p[0] = PerlDebugPlugin.getWorkspace().getRoot().getProject(prjName);
            return p;
        }
        catch (Exception ce)
		{
			PerlDebugPlugin.log(ce);
            return null;
		}
	}

    private void launchCGI()
    {
        mTarget = new CGITarget(mLaunch);
        startTargetThread();
    }
    
    private void launchLocal() throws DebugException
    {
        if (!PerlEditorPlugin.getDefault().requirePerlInterpreter(true)) return;
            
        if (mLaunch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE))
            mTarget = new DebugTargetLocal(mLaunch);
        else
            mTarget = new RunTarget(mLaunch);
        
        mTarget.start();
        mLaunch.addDebugTarget(mTarget);
        
        // If the target is terminated after start, an error occurred.
        // We need to also terminate the launch to make it possible to
        // remove it from the list:
        if (mTarget.isTerminated()) mLaunch.terminate();
    }

    private void launchRemote()
    {        
        mTarget = new RemoteTarget(mLaunch);
        startTargetThread();
    }
    
    private void startTargetThread()
    {
        new Thread("EPIC-Debugger:startTargetThread")
        {
            public void run()
            {
                mTarget.start();
                if (!mTarget.isTerminated())
                {
                    mLaunch.addDebugTarget(mTarget);
                    PerlDB db = ((DebugTarget) mTarget).getDebugger();
                    if (db != null) db.generateDebugInitEvent();
                }
            };
        }.start();
    }
}
