package org.epic.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class LaunchConfigurationDelegate
	implements ILaunchConfigurationDelegate
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

		mLaunchConfiguration = configuration;
		mLaunch = launch;

		System.out.println("Launch: " + mLaunchConfiguration.getLocation());

		String cgi =
			launch.getLaunchConfiguration().getAttribute(
				PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
				((String) null));

		launch.setSourceLocator(new SourceLocator());

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
						((DebugTarget) mTarget)
							.getDebuger()
							.generateDebugInitEvent();
					}
				};
			};

			start.start();

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
			} else
			{
				mTarget = new RunTarget(launch);
				mTarget.start();
				mLaunch.addDebugTarget(mTarget);
			}

	}

}
