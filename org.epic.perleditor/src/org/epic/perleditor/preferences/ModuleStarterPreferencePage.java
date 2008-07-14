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
package org.epic.perleditor.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.epic.core.util.WidgetUtils;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author James Graham
 */
public class ModuleStarterPreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage
{
    public static final String MODULE_STARTER_PREF_ID = "org.epic.perleditor.preferences.ModuleStarterPreferencePage"; //$NON-NLS-1$

    private static final String DEFAULT_LOCATION = "/usr/bin/module-starter";
    private static final String ENTER_PATH_MSG = "Enter path to module-starter executable";
    private static final String INVALID_PATH_ERR = "Invalid module-starter location";

    private Button moduleEnabledButton;
    private Button useDefaultButton;
    private Button useCustomButton;
    private Text customLocationText;

    private Button overrideConfigButton;
    private Text authorText;
    private Text emailText;
    private Text addnOptsText;

    private List errors = new ArrayList(5);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    protected Control createContents(Composite parent)
    {
        Composite composite = createComposite(parent, 1);
        moduleEnabledButton = WidgetUtils.createButton(composite,
            "Enable Module::Starter", SWT.CHECK | SWT.LEFT);
        moduleEnabledButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                validateEnabled();
                toggleEnabled(moduleEnabledButton.getSelection());
            }
        });

        Composite locations = WidgetUtils.createGroup(composite,
            GridData.FILL_HORIZONTAL);

        useDefaultButton = WidgetUtils
            .createButton(locations, "Default location (" + DEFAULT_LOCATION
                + ")", SWT.RADIO | SWT.LEFT);
        useDefaultButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                validateLocation(DEFAULT_LOCATION);
            }
        });
        useDefaultButton.setSelection(true);

        useCustomButton = WidgetUtils.createButton(locations,
            "Custom location", SWT.RADIO | SWT.LEFT);
        useCustomButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                customLocationText.setText(loadString(
                    PreferenceConstants.MODULE_STARTER_LOCATION,
                    DEFAULT_LOCATION));
                customLocationText.setEnabled(useCustomButton.getSelection()
                    && moduleEnabledButton.getSelection());

                validateLocation(customLocationText.getText());
            }
        });

        customLocationText = WidgetUtils.createText(locations,
            "Path to module-starter");
        customLocationText.addModifyListener(new ModifyListener()
        {
            public void modifyText(ModifyEvent e)
            {
                validateLocation(customLocationText.getText());
            }
        });

        Composite fields = WidgetUtils.createGroup(composite,
            GridData.FILL_HORIZONTAL);

        // add button to override config options.
        overrideConfigButton = WidgetUtils.createButton(fields,
            "Override module-starter config file", SWT.CHECK);
        overrideConfigButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                authorText.setEnabled(overrideConfigButton.getSelection()
                    && moduleEnabledButton.getSelection());
                emailText.setEnabled(overrideConfigButton.getSelection()
                    && moduleEnabledButton.getSelection());
                addnOptsText.setEnabled(overrideConfigButton.getSelection()
                    && moduleEnabledButton.getSelection());
            }
        });
        overrideConfigButton.setSelection(false);

        Composite cols = createComposite(fields, 2);

        WidgetUtils.createLabel(cols, "Module Author");
        authorText = WidgetUtils.createText(cols, System
            .getProperty("user.name"));
        authorText.setEnabled(false);

        WidgetUtils.createLabel(cols, "Module Author Email");
        emailText = WidgetUtils.createText(cols, "");
        emailText.setEnabled(false);

        WidgetUtils.createLabel(cols, "Additional options");
        addnOptsText = WidgetUtils.createText(cols, "");
        addnOptsText.setEnabled(false);

        toggleEnabled(moduleEnabledButton.getSelection());

        loadPreferences();

        return null;
    }

    public static String getModuleStarter()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getString(
            PreferenceConstants.MODULE_STARTER_LOCATION);
    }

    public static boolean isModuleStarterEnabled()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
            PreferenceConstants.MODULE_STARTER_ENABLED);
    }

    /**
     *
     * @return <code>String</code> Module::Starter author name. Defaults to
     *         <code>user.name</code>
     */
    public static String getModuleStarterAuthor()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getString(
            PreferenceConstants.MODULE_STARTER_AUTHOR);
    }

    /**
     *
     * @return <code>String</code> email address of the Module::Starter author
     */
    public static String getModuleStarterEmail()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getString(
            PreferenceConstants.MODULE_STARTER_EMAIL);
    }

    /**
     *
     * @return String of additional options to pass to Module::Starter
     */
    public static String getModuleStarterAdditionalOpts()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getString(
            PreferenceConstants.MODULE_STARTER_ADDN_OPTS);
    }

    /**
     *
     * @return <fode>true</code> if the Module::Starter preferences are
     *         configured to override the default .module-starter files
     */
    public static boolean isOverrideConfig()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
            PreferenceConstants.MODULE_STARTER_OVERRIDE_CONFIG);
    }

    /*
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench)
    {
        setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());
    }

    /*
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    public boolean performOk()
    {
        storeBoolean(PreferenceConstants.MODULE_STARTER_ENABLED,
            moduleEnabledButton.getSelection());

        storeBoolean(PreferenceConstants.MODULE_STARTER_DEFAULT_LOCATION,
            useDefaultButton.getSelection());

        String location = customLocationText.getText().equals("") ? DEFAULT_LOCATION
            : customLocationText.getText();
        storeString(PreferenceConstants.MODULE_STARTER_LOCATION, location);

        storeBoolean(PreferenceConstants.MODULE_STARTER_OVERRIDE_CONFIG,
            overrideConfigButton.getSelection());
        storeString(PreferenceConstants.MODULE_STARTER_AUTHOR, authorText
            .getText());
        storeString(PreferenceConstants.MODULE_STARTER_EMAIL, emailText
            .getText());
        storeString(PreferenceConstants.MODULE_STARTER_ADDN_OPTS, addnOptsText
            .getText());

        return super.performOk();
    }

    /*
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults()
    {
        moduleEnabledButton.setSelection(true);
        useDefaultButton.setSelection(true);
        useCustomButton.setSelection(false);
        customLocationText.setText("");
        overrideConfigButton.setSelection(false);
        authorText.setText(System.getProperty("user.name"));
        emailText.setText("");
        addnOptsText.setText("");
        super.performDefaults();
    }

    /**
     * pops an error message off the error stack
     */
    protected void removeErrorMessage(String message)
    {
        errors.remove(message);
        if (errors.isEmpty())
        {
            addErrorMessage(null);
        }
        else
        {
            addErrorMessage((String) errors.get(errors.size() - 1));
        }
    }

    private void addErrorMessage(String message)
    {
        errors.remove(message);
        errors.add(message);

        setErrorMessage(message);
        setValid(message == null);
    }

    private Composite createComposite(Composite parent, int numCols)
    {
        return WidgetUtils.createComposite(parent, numCols);
    }

    private boolean loadBoolean(String name)
    {
        return getPreferenceStore().getBoolean(name);
    }

    private void loadPreferences()
    {
        boolean useDefault = loadBoolean(PreferenceConstants.MODULE_STARTER_DEFAULT_LOCATION);
        useDefaultButton.setSelection(useDefault);
        useCustomButton.setSelection(!useDefault);

        customLocationText.setText(loadString(
            PreferenceConstants.MODULE_STARTER_LOCATION, DEFAULT_LOCATION));
    }

    private String loadString(String name, String backup)
    {
        String value = getPreferenceStore().getString(name);
        return ("".equals(value) ? backup : value);
    }

    private void storeBoolean(String name, boolean value)
    {
        getPreferenceStore().setValue(name, value);
    }

    private void storeString(String name, String value)
    {
        getPreferenceStore().setValue(name, value);
    }

    private void toggleEnabled(boolean enabled)
    {
        System.setProperty(PreferenceConstants.MODULE_STARTER_ENABLED, Boolean
            .toString(enabled));

        useDefaultButton.setEnabled(enabled);
        useCustomButton.setEnabled(enabled);
        overrideConfigButton.setEnabled(enabled);

        customLocationText.setEnabled((enabled && !useDefaultButton
            .getSelection()));
        authorText.setEnabled(enabled && overrideConfigButton.getSelection());
        emailText.setEnabled(enabled && overrideConfigButton.getSelection());
        addnOptsText.setEnabled(enabled && overrideConfigButton.getSelection());
    }

    private void validateEnabled()
    {
        if (!moduleEnabledButton.getSelection())
        {
            removeErrorMessage(INVALID_PATH_ERR);
            return;
        }

        String location = DEFAULT_LOCATION;
        if (!useDefaultButton.getSelection())
        {
            location = customLocationText.getText();
        }

        validateLocation(location);
    }

    private void validateLocation(String location)
    {
        if (location == null || location.length() == 0)
        {
            if (!useDefaultButton.getSelection())
            {
                addErrorMessage(ENTER_PATH_MSG);
            }
            return;
        }

        removeErrorMessage(ENTER_PATH_MSG);

        if (!new File(location).isFile())
        {
            addErrorMessage(INVALID_PATH_ERR);
        }
        else
        {
            removeErrorMessage(INVALID_PATH_ERR);
        }
    }
}
