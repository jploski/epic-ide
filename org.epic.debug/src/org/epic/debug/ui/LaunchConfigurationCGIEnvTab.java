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

import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;

public class LaunchConfigurationCGIEnvTab
	extends AbstractLaunchConfigurationTab
	implements IPropertyChangeListener
{
    private static final Pattern ENV_VAR_PATTERN =
        Pattern.compile("^\\s*[^\\s]+\\s*=.*");

	private ListEditor fEnvVar;

	public void createControl(Composite parent)
	{
        Font font = parent.getFont();

        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        GridLayout topLayout = new GridLayout();
        topLayout.verticalSpacing = 0;
        comp.setLayout(topLayout);
        comp.setFont(font);
        
		GridData gd;

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		gd = new GridData(GridData.FILL_HORIZONTAL);

		Composite envComp = new Composite(comp, SWT.NONE);
		envComp.setLayout(layout);
		envComp.setLayoutData(gd);
		envComp.setFont(font);

		fEnvVar = new ListEditor("Environment Variables", envComp, this);
		fEnvVar.fillIntoGrid(envComp, 3);
		fEnvVar.setPropertyChangeListener(this);
		createVerticalSpacer(comp, 1);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		gd = new GridData(GridData.FILL_HORIZONTAL);
	}

	public void initializeFrom(ILaunchConfiguration config)
	{
		updateParamsFromConfig(config);
	}

	protected void updateParamsFromConfig(ILaunchConfiguration config)
	{
        fEnvVar.initilizeFrom(config);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
			"OK");

		fEnvVar.doApply(config);
	}

	public void dispose()
	{
	}

	public boolean isValid(ILaunchConfiguration config)
	{
		setErrorMessage(null);
		setMessage(null);

		String[] items = this.fEnvVar.getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (!ENV_VAR_PATTERN.matcher(items[i]).matches())
			{
				setErrorMessage("Invalid Environment Variable Entry at Line "+(i+1) );
				return false;
			}
		}
		return true;
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config)
	{
		String root =
			ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
            root);
		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
            root);
		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE,
            root);
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_DEBUG_PORT,
			PerlDebugPlugin.getDefaultDebugPort());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
			"OK");
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_CGI_ENV,
			(Map) null);
	}

	public String getName()
	{
		return "CGI Environment";
	}

	public Image getImage()
	{
		return
			PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				PerlDebugImages.DESC_OBJS_LaunchTabCGI);
	}

	public void propertyChange(PropertyChangeEvent event)
	{
		updateLaunchConfigurationDialog();
	}

	public void update()
	{
		updateLaunchConfigurationDialog();
	}
}