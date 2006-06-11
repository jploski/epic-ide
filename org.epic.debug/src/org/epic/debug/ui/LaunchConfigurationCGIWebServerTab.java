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

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;

public class LaunchConfigurationCGIWebServerTab
	extends AbstractLaunchConfigurationTab
	implements IPropertyChangeListener
{
	private Text fCGISuffix;
	private DirectoryFieldEditor fCGIRootDir;
	private DirectoryFieldEditor fHTMLRootDir;
	private FileFieldEditor fHTMLRootFile;

	public void createControl(Composite parent)
	{
        Font font = parent.getFont();

        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 1;
        comp.setLayout(topLayout);
        comp.setFont(font);
        
        Composite fields = new Composite(comp, SWT.NONE);
        GridLayout fieldsLayout = new GridLayout();
        fieldsLayout.numColumns = 3;
        fields.setFont(font);
        fields.setLayout(fieldsLayout);
        fields.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        createHTMLRootDirectoryGroup(fields);
        createStartupFileGroup(fields);
        createCGIRootDirectoryGroup(fields);
        createVerticalSpacer(comp, 1);
        createCGISuffixGroup(comp);
	}

	public void initializeFrom(ILaunchConfiguration config)
	{
		updateParamsFromConfig(config);
	}

	private void updateParamsFromConfig(ILaunchConfiguration config)
	{
		try
		{
			fHTMLRootDir.setStringValue(
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
					(String) null));
			fHTMLRootFile.setStringValue(
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE,
					(String) null));
			fCGIRootDir.setStringValue(
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
					(String) null));
			fCGISuffix.setText(
                config.getAttribute(
				    PerlLaunchConfigurationConstants.ATTR_CGI_FILE_EXTENSION,
					".cgi,.pl"));
		}
        catch (CoreException e)
		{
            PerlDebugPlugin.log(e);
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
			this.fHTMLRootDir.getStringValue());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE,
			this.fHTMLRootFile.getStringValue());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
			this.fCGIRootDir.getStringValue());
		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_FILE_EXTENSION,
			this.fCGISuffix.getText());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
			"OK");
	}

	public void dispose()
	{
	}

	public boolean isValid(ILaunchConfiguration config)
	{
		setErrorMessage(null);
		setMessage(null);

		String value = fHTMLRootDir.getStringValue();

		if (value == null)
		{
			setErrorMessage("HTML Root Directory is missing");
			return false;
		}

		File file = new File(value);
		if (!file.exists() || !file.isDirectory())
		{
			setErrorMessage("HTML Root Directory is invalid");
			return false;
		}

		value = fHTMLRootFile.getStringValue();

		if (value == null)
		{
			setErrorMessage("HTML Startup File is missing");
			return false;
		}

		file = new File(value);
		if (!file.exists() || !file.isFile())
		{
			setErrorMessage("HTML Startup File is invalid");
			return false;
		}

		if (value.indexOf(fHTMLRootDir.getStringValue()) != 0)
		{
			setErrorMessage("HTML Startup File must be located within HTML Root Directory (or one of its subfolders)");
			return false;
		}

		value = fCGIRootDir.getStringValue();
		if (value == null)
		{
			setErrorMessage("CGI Root Directory is missing");
			return false;
		}

		file = new File(value);
		if (!file.exists() || !file.isDirectory())
		{
			setErrorMessage("CGI Root Directory is invalid");
			return false;
		}
		return true;
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config)
	{
		String root =
			ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();

		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
            root);
		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE,
            root);
        config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
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
		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_FILE_EXTENSION,
			".cgi,.pl");
	}

	public String getName()
	{
		return "Web Server";
	}

	public Image getImage()
	{
		return (
			PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				PerlDebugImages.DESC_OBJS_LaunchTabCGI));
	}

	public void propertyChange(PropertyChangeEvent event)
	{
		if (event.getSource() == fHTMLRootDir)
		{            
			if (fHTMLRootFile
				.getStringValue()
				.indexOf(fHTMLRootDir.getStringValue())
				!= 0)
				fHTMLRootFile.setStringValue(fHTMLRootDir.getStringValue());
		}

		updateLaunchConfigurationDialog();
	}

	public void update()
	{
		updateLaunchConfigurationDialog();
	}
    
    private void createCGIRootDirectoryGroup(Composite parent)
    {       
        fCGIRootDir = new DirectoryFieldEditor(
                "",
                "CGI &Root Directory:",
                parent);
        fCGIRootDir.fillIntoGrid(parent, 3);
        fCGIRootDir.setPropertyChangeListener(this);
    }
    
    private void createCGISuffixGroup(Composite parent)
    {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        comp.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        comp.setLayoutData(gd);
        comp.setFont(parent.getFont());

        Label label = new Label(comp, SWT.NONE);
        label.setText("File Extension for CGI Files (comma-separated list, e.g. \".cgi,.pl\"):");
        label.setFont(parent.getFont());
        gd = new GridData(GridData.FILL_HORIZONTAL);
        label.setLayoutData(gd);

        fCGISuffix = new Text(comp, SWT.SINGLE | SWT.BORDER);

        gd = new GridData(GridData.FILL_HORIZONTAL);
        fCGISuffix.setLayoutData(gd);
        fCGISuffix.setFont(parent.getFont());
        fCGISuffix.addModifyListener(new ModifyListener()
        {
            public void modifyText(ModifyEvent evt)
            {
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void createHTMLRootDirectoryGroup(Composite parent)
    {       
        fHTMLRootDir = new DirectoryFieldEditor(
                "",
                "&HTML Root Directory:",
                parent);
        fHTMLRootDir.fillIntoGrid(parent, 3);
        fHTMLRootDir.setPropertyChangeListener(this);
    }
    
    private void createStartupFileGroup(Composite parent)
    {
        fHTMLRootFile = new FileFieldEditor(
                "",
                "HTML &Startup File:",
                parent);
        fHTMLRootFile.fillIntoGrid(parent, 3);
        fHTMLRootFile.setPropertyChangeListener(this);
    }
}