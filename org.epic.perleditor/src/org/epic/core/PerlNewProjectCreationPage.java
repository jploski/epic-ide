/*******************************************************************************
 * Copyright (c) 2008 James A. Graham and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     James A. Graham (james.a.graham@gmail.com)
 *******************************************************************************/
package org.epic.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.epic.perleditor.preferences.ModuleStarterPreferencePage;

/**
 *
 * @author Jim Graham
 */

public class PerlNewProjectCreationPage extends WizardNewProjectCreationPage
{
	private Link moduleStarterPrefLink;
	private Button moduleButton;
	private Label moduleNameLabel;
	private Text moduleNameText;
	private boolean useModule = false;
	private boolean useModuleStarter = false;

	/**
	 * @param pageName
	 */
	public PerlNewProjectCreationPage(String pageName)
	{
		super(pageName);
		setTitle(ResourceMessages.getString("NewProject.title")); //$NON-NLS-1$
		setDescription(ResourceMessages.getString("NewProject.description")); //$NON-NLS-1$
	}

	public PerlNewProjectCreationPage()
	{
		this(PerlNewProjectCreationPage.class.getName());
	}

	public void createControl(Composite parent)
	{
		super.createControl(parent);

		useModuleStarter = ModuleStarterPreferencePage.isModuleStarterEnabled();

		Composite composite = (Composite) getControl();

		Group group = new Group(composite, SWT.NONE);
		group.setText("Module::Starter Information");
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		group.setLayout(new GridLayout(2, false));

		moduleButton = new Button(group, SWT.CHECK);
		moduleButton.setText("&Use Module::Starter");
		moduleButton.setSelection(false);
		moduleButton.setEnabled(useModuleStarter);
		moduleButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent event)
			{
				useModule = moduleButton.getSelection();
				setModuleName();
			}
		});
		moduleButton.setToolTipText(ResourceMessages
		    .getString("NewProject.moduleStarterTooltip"));

		// link element to Module::Starter pref page
		moduleStarterPrefLink = new Link(group, SWT.NONE);
		moduleStarterPrefLink.setFont(group.getFont());
		moduleStarterPrefLink.setText(ResourceMessages
		    .getString("NewProject.moduleStarterPrefDesc")); //$NON-NLS-1$
		moduleStarterPrefLink.setLayoutData(new GridData(GridData.END,
		    GridData.CENTER, false, false));
		moduleStarterPrefLink.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent event)
			{
				String moduleStarterID = ModuleStarterPreferencePage.MODULE_STARTER_PREF_ID;
				Map<String, Boolean> data = new HashMap<String, Boolean>();
				data.put("PropertyAndPreferencePage.nolink", Boolean.TRUE); //$NON-NLS-1$
				PreferencesUtil.createPreferenceDialogOn(getShell(),
				    moduleStarterID, new String[] { moduleStarterID }, data)
				    .open();
				// check if we changed the preferences
				toggleEnabled(ModuleStarterPreferencePage.isModuleStarterEnabled());
			}
		});

		moduleNameLabel = new Label(group, SWT.LEAD);
		moduleNameLabel.setText("Module Name");

		moduleNameText = new Text(group, SWT.LEFT | SWT.READ_ONLY);
		GridData moduleNameData = new GridData(GridData.FILL_HORIZONTAL);
		moduleNameData.widthHint = 250;
		moduleNameText.setLayoutData(moduleNameData);
		moduleNameText.setFont(parent.getFont());

        toggleEnabled(ModuleStarterPreferencePage.isModuleStarterEnabled());

		setModuleName();
	}
    
    public void setPageComplete(boolean complete)
    {
        super.setPageComplete(complete);
        setModuleName();
    }

    boolean getUseModule()
    {
        return useModule;
    }

    String getModuleName()
    {
        return moduleNameText.getText().trim();
    }

	private void setModuleName()
	{
        String projectName = getProjectName();
        if (projectName == null || !useModule) projectName = "";
        
        if (moduleNameText != null)        
            moduleNameText.setText(projectName.replaceAll("-", "::"));
	}
    
    private void toggleEnabled(boolean enabled)
    {
        moduleButton.setEnabled(enabled);
        moduleNameLabel.setEnabled(enabled);
        moduleNameText.setEnabled(enabled);
    }
}
