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

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.browser.BrowserDescriptor;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.epic.debug.cgi.CustomBrowser;


public class LaunchConfigurationCGIBrowserTab
	extends AbstractLaunchConfigurationTab
{

	/**
	 * A launch configuration tab that displays and edits project and
	 * main type name launch configuration attributes.
	 * <p>
	 * This class may be instantiated. This class is not intended to be subclassed.
	 * </p>
	 * @since 2.0
	 */

	private Table browsersTable;
	private Label customBrowserPathLabel;
	private Text customBrowserPath;
	private Button customBrowserBrowse;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String PERL_NATURE_ID =
		"org.epic.perleditor.perlnature";

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent)
	{
		Font font = parent.getFont();

		//noDefaultAndApplyButton();
		Composite mainComposite = new Composite(parent, SWT.NULL);
		setControl(mainComposite);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		//data.grabExcessHorizontalSpace = true;
		mainComposite.setLayoutData(data);
		mainComposite.setFont(font);

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComposite.setLayout(layout);

		Label description = new Label(mainComposite, SWT.NULL);
		description.setFont(font);
		description.setText("select browser to use for showing HTML pages during CGI debugging");
		createSpacer(mainComposite);

		Label tableDescription = new Label(mainComposite, SWT.NULL);
		tableDescription.setFont(font);
		tableDescription.setText("Current selection:");
		//data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		//description.setLayoutData(data);
		browsersTable = new Table(mainComposite, SWT.CHECK | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		//.heightHint = convertHeightInCharsToPixels(6);
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
//			if (BrowserManager
//				.getInstance()
//				.getDefaultBrowserID()
//				.equals(aDescs[i].getID()))
//				item.setChecked(true);
//			else
//				item.setChecked(false);
			item.setGrayed(aDescs.length == 1);
		}

		createCustomBrowserPathPart(mainComposite);
		
		 
		

	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TableItem[] items = browsersTable.getItems();
		BrowserDescriptor[] browsers =
			BrowserManager.getInstance().getBrowserDescriptors();

		for (int i = 0; i < browsers.length; i++)
		{
			if (browsers[i].getID().equals(attrBrowserID))
				items[i].setChecked(true);
		}

		customBrowserPath.setText(attrBrowserPath);

	}

	protected void updateParamsFromConfig(ILaunchConfiguration config)
	{
		initializeFrom(config);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config)
	{

		TableItem[] items = browsersTable.getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (items[i].getChecked())
			{
				String browserID =
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
		for (int i = 0; i < items.length; i++)
		{
			if (items[i].getChecked())
			{
				String browserID =
					BrowserManager
						.getInstance()
						.getBrowserDescriptors()[i]
						.getID();

				return (browserID);
			}
		}
		return (null);
	}

	

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose()
	{
	}

	/**
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot()
	{
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
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

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
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
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName()
	{
		return "Browser"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage()
	{
		return (
			PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				PerlDebugImages.DESC_OBJS_LaunchTabCGI));
	}

	//**********************************************************
	private void createSpacer(Composite parent)
	{
		Label spacer = new Label(parent, SWT.NONE);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.BEGINNING;
		spacer.setLayoutData(data);
	}

	private void setEnabledCustomBrowserPath()
	{
		TableItem[] items = browsersTable.getItems();
		for (int i = 0; i < items.length; i++)
		{
			if (items[i].getChecked())
			{
				boolean enabled =
					(HelpPlugin.PLUGIN_ID + ".custombrowser").equals(
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