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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.browser.BrowserDescriptor;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.epic.core.views.browser.BrowserView;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;
import org.epic.debug.cgi.CustomBrowser;


public class LaunchConfigurationCGIBrowserTab
	extends AbstractLaunchConfigurationTab
{
	private Table browsersTable;
	private Label customBrowserPathLabel;
	private Text customBrowserPath;
	private Button customBrowserBrowse;

	public void createControl(Composite parent)
	{
        Font font = parent.getFont();

        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        GridLayout topLayout = new GridLayout();
        comp.setLayout(topLayout);
        comp.setFont(font);

		Label description = new Label(comp, SWT.NULL);
		description.setFont(font);
		description.setText("Browser used for showing HTML pages during CGI debugging:");

		browsersTable = new Table(comp, SWT.CHECK | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		browsersTable.setLayoutData(gd);
		browsersTable.setFont(font);
		browsersTable.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent selEvent)
			{
				if (selEvent.detail == SWT.CHECK)
				{
					TableItem item = (TableItem) selEvent.item;
					if (item.getChecked())
					{
						// Deselect others
						TableItem[] items = browsersTable.getItems();
						for (int i = 0; i < items.length; i++)
						{
							if (items[i] == item)
								continue;
							else
								items[i].setChecked(false);
						}
					} else
					{
						// Do not allow deselection
						item.setChecked(true);
					}
					setEnabledCustomBrowserPath();
				}
				updateLaunchConfigurationDialog();
			}
			public void widgetDefaultSelected(SelectionEvent selEvent)
			{
				updateLaunchConfigurationDialog();
			}
		});
		// populate table with browsers
		BrowserDescriptor[] aDescs =
			BrowserManager.getInstance().getBrowserDescriptors();
		for (int i = 0; i < aDescs.length; i++)
		{
			TableItem item = new TableItem(browsersTable, SWT.NONE);
			item.setText(aDescs[i].getLabel());
			item.setGrayed(aDescs.length == 1);
		}
		TableItem item = new TableItem(browsersTable, SWT.NONE);
		item.setText("Built-in Browser");
		item.setGrayed(aDescs.length == 1);
		
		createCustomBrowserPathPart(comp);
	}

	public void initializeFrom(ILaunchConfiguration config)
	{
		String attrBrowserID = null;
		String attrBrowserPath = null;
		try
		{
			attrBrowserID =
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_BROWSER_ID,
					(String) null);
			attrBrowserPath =
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_CUSTOM_BROWSER_PATH,
					(String) null);
		} catch (CoreException e)
		{
            DebugPlugin.log(e);
		}

		TableItem[] items = browsersTable.getItems();
		BrowserDescriptor[] browsers =
			BrowserManager.getInstance().getBrowserDescriptors();

		if( attrBrowserID.equals(BrowserView.ID_BROWSER) )
			items[items.length-1].setChecked(true);
		
		for (int i = 0; i < browsers.length; i++)
		{
			if (browsers[i].getID().equals(attrBrowserID))
				items[i].setChecked(true);
		}
		
		customBrowserPath.setText(attrBrowserPath);
		setEnabledCustomBrowserPath();
	}

	protected void updateParamsFromConfig(ILaunchConfiguration config)
	{
		initializeFrom(config);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
		TableItem[] items = browsersTable.getItems();
		
		for (int i = 0; i < items.length; i++)
		{
			if (items[i].getChecked())
			{
				String browserID;
				if( i == items.length-1 )
					browserID = BrowserView.ID_BROWSER;
				else					
					browserID =
					BrowserManager
						.getInstance()
						.getBrowserDescriptors()[i]
						.getID();
				config.setAttribute(
					PerlLaunchConfigurationConstants.ATTR_BROWSER_ID,
					browserID);
				break;
			}
		}

		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_CUSTOM_BROWSER_PATH,
			customBrowserPath.getText());
	}

	public String getBrowserID()
	{
		TableItem[] items = browsersTable.getItems();
		
		if (items[items.length-1].getChecked()) return BrowserView.ID_BROWSER;

		for (int i = 0; i < items.length; i++)
		{
			if (items[i].getChecked())
			{
				String browserID =
					BrowserManager
						.getInstance()
						.getBrowserDescriptors()[i]
						.getID();

				return browserID;
			}
		}
		return null;
	}

	public void dispose()
	{
	}

	public boolean isValid(ILaunchConfiguration config)
	{
		setErrorMessage(null);
		setMessage(null);

		if (CustomBrowser.isCustomBrowserID(getBrowserID()))
			if (customBrowserPath.getText() == null
				|| customBrowserPath.getText().length() == 0)
			{
				setErrorMessage("Specify Custom BrowserProject"); //$NON-NLS-1$
				return false;
			}

		return true;
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config)
	{
		Preferences pref = HelpPlugin.getDefault().getPluginPreferences();
		String browserPath =
			pref.getString(org.eclipse.help.internal.browser.CustomBrowser.CUSTOM_BROWSER_PATH_KEY);

		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_BROWSER_ID, BrowserManager.getInstance().getDefaultBrowserID()); //$NON-NLS-1$;
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_CUSTOM_BROWSER_PATH,
			browserPath);
    }
    
	public String getName()
	{
		return "Browser";
	}

	public Image getImage()
	{
		return
			PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				PerlDebugImages.DESC_OBJS_LaunchTabCGI);
	}

	//**********************************************************
	private void setEnabledCustomBrowserPath()
	{
		TableItem[] items = browsersTable.getItems();
		for (int i = 0; i < items.length-1; i++)
		{
			if (items[i].getChecked())
			{
				boolean enabled =
					(HelpPlugin.PLUGIN_ID + ".base.custombrowser").equals(
						BrowserManager
							.getInstance()
							.getBrowserDescriptors()[i]
							.getID());
				customBrowserPathLabel.setEnabled(enabled);
				customBrowserPath.setEnabled(enabled);
				customBrowserBrowse.setEnabled(enabled);
				break;
			}
		}

	}

	protected void createCustomBrowserPathPart(Composite mainComposite)
	{
		Font font = mainComposite.getFont();

		// vertical space
		new Label(mainComposite, SWT.NULL);

		Composite bPathComposite = new Composite(mainComposite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 3;
		bPathComposite.setLayout(layout);
		bPathComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		customBrowserPathLabel = new Label(bPathComposite, SWT.LEFT);
		customBrowserPathLabel.setFont(font);
		//customBrowserPathLabel.setText(WorkbenchResources.getString("CustomBrowserPreferencePage.Program")); //$NON-NLS-1$

		customBrowserPath = new Text(bPathComposite, SWT.BORDER);
		customBrowserPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		customBrowserPath.setFont(font);
		customBrowserPath.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent evt)
			{
				updateLaunchConfigurationDialog();
			}
		});
		//customBrowserPath.setText(
		//	HelpPlugin.getDefault().getPluginPreferences().getString(
		//		CustomBrowser.CUSTOM_BROWSER_PATH_KEY));

		customBrowserBrowse = new Button(bPathComposite, SWT.NONE);
		customBrowserBrowse.setFont(font);
		customBrowserBrowse.setText("Select Custom Browser"); //$NON-NLS-1$
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		customBrowserBrowse.setLayoutData(data);
		customBrowserBrowse.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent event)
			{
			}
			public void widgetSelected(SelectionEvent event)
			{
				FileDialog d = new FileDialog(getShell());
				d.setText("CustomBrowserPreferencePage.Details"); //$NON-NLS-1$
				String file = d.open();
				if (file != null)
				{
					customBrowserPath.setText("\"" + file + "\" %1");
				}
			}
		});
		setEnabledCustomBrowserPath();
	}
}