
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

package org.epic.debug.ui;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.ProjectAndFileBlock;

public class LaunchConfigurationMainTab
    extends AbstractLaunchConfigurationTab
{
    protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

    protected ProjectAndFileBlock fFileToExecuteBlock;

    public LaunchConfigurationMainTab()
    {
        fFileToExecuteBlock   = new ProjectAndFileBlock();
    }

	public void createControl(Composite parent)
	{
		Font font = parent.getFont();

		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
        topLayout.verticalSpacing = 0;
		comp.setLayout(topLayout);
        comp.setFont(font);
        
		fFileToExecuteBlock.createControl(comp);
	}

	public void initializeFrom(ILaunchConfiguration config)
	{
		fFileToExecuteBlock.initializeFrom(config);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
		fFileToExecuteBlock.performApply(config);
	}
    
    public String getErrorMessage()
    {
        String m = super.getErrorMessage();
        return m == null ? fFileToExecuteBlock.getErrorMessage() : m;
    }
    
    public String getMessage()
    {
        String m = super.getMessage();
        return m == null ? fFileToExecuteBlock.getMessage() : m;
    }
    
    public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog)
    {
        super.setLaunchConfigurationDialog(dialog);
        fFileToExecuteBlock.setLaunchConfigurationDialog(dialog);
    }

	public void dispose()
	{
	}

	public boolean isValid(ILaunchConfiguration config)
	{
		setErrorMessage(null);
		setMessage(null);

		return fFileToExecuteBlock.isValid(config);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config)
	{
		fFileToExecuteBlock.setDefaults(config);
	}

	public String getName()
	{
		return "Main";
	}

	public Image getImage()
	{
		return
			PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				PerlDebugImages.DESC_OBJS_LaunchTabMain);
	}
}