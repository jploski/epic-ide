package org.epic.perleditor.preferences;

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

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class SourceCriticPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
    //~ Static fields/initializers

    private static final String DEFAULT_LOCATION = "/usr/bin/perlcritic";

    //~ Instance fields

    private Text customText;

    private Button enabledButton;

    private List errors = new ArrayList(5);
    private Button useCustomButton;

    private Button useDefaultButton;

    //~ Methods

    public static String getPerlCritic()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getString(
                PreferenceConstants.SOURCE_CRITIC_LOCATION);
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
        storeBoolean(PreferenceConstants.SOURCE_CRITIC_ENABLED, enabledButton.getSelection());
        storeBoolean(PreferenceConstants.SOURCE_CRITIC_DEFAULT_LOCATION,
            useDefaultButton.getSelection());

        String location = customText.getText().equals("") ? DEFAULT_LOCATION : customText.getText();
        storeString(PreferenceConstants.SOURCE_CRITIC_LOCATION, location);

        return super.performOk();
    }

    protected Control createContents(Composite parent)
    {
        Composite composite = createComposite(parent, 1);

        enabledButton = WidgetUtils.createButton(composite, "Enable Critic", SWT.CHECK | SWT.LEFT);
        enabledButton.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    validateEnabled();
                    toggleCriticEnabled(enabledButton.getSelection());
                }
            });

        Composite locations = WidgetUtils.createGroup(composite, GridData.FILL_HORIZONTAL);

        useDefaultButton =
            WidgetUtils.createButton(locations, "Default location (/usr/bin/perlcritic)", SWT.RADIO
                | SWT.LEFT);
        useDefaultButton.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    validateLocation(DEFAULT_LOCATION);
                }
            });

        useCustomButton =
            WidgetUtils.createButton(locations, "Custom location", SWT.RADIO | SWT.LEFT);
        useCustomButton.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    customText.setText(loadCustomLocation());
                    customText.setEnabled(useCustomButton.getSelection());

                    validateLocation(customText.getText());
                }
            });

        // validation job to ensure this is correct?
        customText = WidgetUtils.createText(locations, "Path to perlcritic");
        customText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    validateLocation(customText.getText());
                }
            });

        loadPreferences();
        toggleCriticEnabled(enabledButton.getSelection());

        return composite;
    }

    /*
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults()
    {
        enabledButton.setSelection(false);
        useDefaultButton.setSelection(true);
        useCustomButton.setSelection(false);

        customText.setText("");

        toggleCriticEnabled(false);

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

    private String loadCustomLocation()
    {
        String location = loadString(PreferenceConstants.SOURCE_CRITIC_LOCATION);
        return (location.equals(DEFAULT_LOCATION) ? "" : location);
    }

    private void loadPreferences()
    {
        enabledButton.setSelection(loadBoolean(PreferenceConstants.SOURCE_CRITIC_ENABLED));

        boolean useDefault = loadBoolean(PreferenceConstants.SOURCE_CRITIC_DEFAULT_LOCATION);
        useDefaultButton.setSelection(useDefault);
        useCustomButton.setSelection(! useDefault);

        customText.setText(loadCustomLocation());
    }

    private String loadString(String name)
    {
        return getPreferenceStore().getString(name);
    }

    private void storeBoolean(String name, boolean value)
    {
        getPreferenceStore().setValue(name, value);
    }

    private void storeString(String name, String value)
    {
        getPreferenceStore().setValue(name, value);
    }

    private void toggleCriticEnabled(boolean enabled)
    {
        System.setProperty(PreferenceConstants.SOURCE_CRITIC_ENABLED, Boolean.toString(enabled));

        useDefaultButton.setEnabled(enabled);
        useCustomButton.setEnabled(enabled);

        customText.setEnabled((enabled && ! useDefaultButton.getSelection()));
    }

    private void validateEnabled()
    {
        if (! enabledButton.getSelection())
        {
            removeErrorMessage("Invalid perl critic location");
            return;
        }

        String location = DEFAULT_LOCATION;
        if (! useDefaultButton.getSelection())
        {
            location = customText.getText();
        }

        validateLocation(location);
    }

    private void validateLocation(String location)
    {
        if (! enabledButton.getSelection()) { return; }

        if ("".equals(location))
        {
            if (! useDefaultButton.getSelection())
            {
                addErrorMessage("Enter path to critic executable");
            }

            return;
        }

        removeErrorMessage("Enter path to critic executable");

        if (! new File(location).isFile())
        {
            addErrorMessage("Invalid perl critic location");
        }
        else
        {
            removeErrorMessage("Invalid perl critic location");
        }
    }

}
