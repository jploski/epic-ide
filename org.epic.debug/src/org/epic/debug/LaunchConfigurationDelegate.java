package org.epic.debug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.epic.core.Constants;
import org.epic.core.Perspective;

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
	Target mTarget;
	ILaunch mLaunch;

	/**
	 * Constructor for LaunchConfigurationDelegate.
	 */
	public LaunchConfigurationDelegate()
	{
		super();
	}

	/**
	 * Convenience method to get the launch manager.
	 * 
	 * @return the launch manager
	 */
	protected ILaunchManager getLaunchManager()
	{
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/*org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#getLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return null;
	}

	/**
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String, ILaunch, IProgressMonitor)
	 */
	
	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor)
	
		throws CoreException
	{
		//configuration.launch(mode,monitor);
		mLaunchConfiguration = configuration;
		mLaunch = launch;

		System.out.println("Launch: " + mLaunchConfiguration.getLocation());

		String cgi =
			launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
				((String) null));

		String remote =
			launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE,
				((String) null));
		launch.setSourceLocator(new SourceLocator());
		if (remote != null)
		{
			mTarget = new RemoteTarget(launch);
			//target = new CGITarget(launch);
			Thread start = new Thread()
			{
				public void run()
				{
					mTarget.start();
					if (!mTarget.isTerminated())
					{
						mLaunch.addDebugTarget(mTarget);
						PerlDB db = ((DebugTarget) mTarget)
						.getDebuger();
						if( db != null )					
						 db.generateDebugInitEvent();
					}
				};
			};

			start.start();
			
			// Switch to Debug Perspective
			Perspective.switchPerspective(Constants.DEBUG_PERSPECTIVE_ID);

			//			mTarget.start();
			//			mLaunch.addDebugTarget(mTarget);
			//((DebugTarget) mTarget).getDebuger().generateDebugInitEvent();
		}else 
		if (cgi != null)
		{
			mTarget = new CGITarget(launch);
			//target = new CGITarget(launch);
			Thread start = new Thread()
			{
				public void run()
				{
					mTarget.start();
					if (!mTarget.isTerminated())
					{
						mLaunch.addDebugTarget(mTarget);
						PerlDB db = ((DebugTarget) mTarget)
						.getDebuger();
						if( db != null )					
						 db.generateDebugInitEvent();
					}
				};
			};

			start.start();
			
			// Switch to Debug Perspective
			Perspective.switchPerspective(Constants.DEBUG_PERSPECTIVE_ID);

			//			mTarget.start();
			//			mLaunch.addDebugTarget(mTarget);
			//((DebugTarget) mTarget).getDebuger().generateDebugInitEvent();
		} else
			if (launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE))
			{
				mTarget = new DebugTargetLocal(launch);
				//target = new CGITarget(launch);
				mTarget.start();
				if (!mTarget.isTerminated())
				{
					mLaunch.addDebugTarget(mTarget);
					((DebugTargetLocal) mTarget)
						.getDebuger()
						.generateDebugInitEvent();
				}
				// Switch to Debug Perspective
				Perspective.switchPerspective(Constants.DEBUG_PERSPECTIVE_ID);
			} else
			{
				mTarget = new RunTarget(launch);
				mTarget.start();
				mLaunch.addDebugTarget(mTarget);
			}

	}

	/**
	 * Returns the set of projects to use when searching for errors or <code>null</code> 
	 * if no search is to be done.  
	 * 
	 * @param projects the list of projects to sort into build order
	 * @return a list of projects.
	 */
	protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) throws CoreException {
	String prjName=null;
		try
		{
			
			prjName =
				configuration.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					(String)null);
			

		} catch (Exception ce)
		{
			PerlDebugPlugin.log(ce);
		}
		
		if( prjName == null) return(null);
		IProject p[]= new IProject[1];
		p[0]=
			PerlDebugPlugin.getWorkspace().getRoot().getProject(prjName);

		
		return p;
	}
	
	
}
