/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package org.epic.debug;

//import java.lang.reflect.InvocationTargetException;



import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
//import org.eclipse.jface.viewers.ILabelProvider;
//import org.eclipse.swt.events.SelectionAdapter;
//import org.eclipse.swt.events.SelectionEvent;

//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.swt.widgets.Text;
//import org.eclipse.ui.dialogs.ElementListSelectionDialog;
//import org.eclipse.ui.dialogs.SelectionDialog;
//import org.eclipse.ui.help.WorkbenchHelp;



public class LaunchConfigurationMainTabCGI
	extends LaunchConfigurationMainTab {

/**
 * A launch configuration tab that displays and edits project and
 * main type name launch configuration attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed.
 * </p>
 * @since 2.0
 */


	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		super.performApply(config);
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_AUTO_RECONNECT, true);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
				config.setAttribute(PerlLaunchConfigurationConstants.ATTR_AUTO_RECONNECT, true);
		}
	
}
