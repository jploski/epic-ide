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
	implements ILaunchConfigurationDelegate {
		
	private ILaunchConfiguration mLaunchConfiguration;

	/**
	 * Constructor for LaunchConfigurationDelegate.
	 */
	public LaunchConfigurationDelegate() {
		super();
	}



	/**
	 * Convenience method to get the launch manager.
	 * 
	 * @return the launch manager
	 */
	protected ILaunchManager getLaunchManager() {
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
		throws CoreException {
		
		DebugTarget target;	
		mLaunchConfiguration = configuration;
		System.out.println("Launch: "+mLaunchConfiguration.getLocation());
		launch.setSourceLocator(new SourceLocator ());
		target =new DebugTarget(launch);
		launch.addDebugTarget( target );
		target.getDebuger().generateDebugInitEvent();
		}

}
