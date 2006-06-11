package org.epic.debug.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class LaunchConfigurationDialog
	extends AbstractLaunchConfigurationTabGroup {

	/**
	 * Constructor for LaucunchConfigurationDialog.
	 */
	public LaunchConfigurationDialog() {
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			 new LaunchConfigurationMainTab(true),
             new LaunchConfigurationArgumentsTab(),
             new org.eclipse.debug.ui.CommonTab()
		};
		setTabs(tabs);
	}
}
