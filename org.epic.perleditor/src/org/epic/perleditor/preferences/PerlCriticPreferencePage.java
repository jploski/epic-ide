package org.epic.perleditor.preferences;

import org.eclipse.core.resources.IMarker;

import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.epic.core.util.WidgetUtils;

import org.epic.perleditor.PerlEditorPlugin;

import java.io.File;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class PerlCriticPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
    //~ Static fields/initializers

    private static final String DEFAULT_LOCATION = "/usr/bin/perlcritic";

    private static final String[] LEVELS = { "Info", "Warning", "Error" };

    //~ Instance fields

    private Combo[] comboButtons = new Combo[5];

    private Text customText;

    private Button enabledButton;

    private List errors = new ArrayList(5);
    private Button useCustomButton;

    private Button useDefaultButton;

    //~ Methods

    public static int getMarkerSeverity(int severity)
    {
        assert (severity > 0) && (severity <= 5) : "unknown severity " + severity;

        String prefName = PreferenceConstants.SOURCE_CRITIC_SEVERITY_LEVEL + severity;
        String level = PerlEditorPlugin.getDefault().getPreferenceStore().getString(prefName);

        int markerLevel;
        if ("error".equalsIgnoreCase(level))
        {
            markerLevel = IMarker.SEVERITY_ERROR;
        }
        else if ("warning".equalsIgnoreCase(level) || "".equals(level))
        {
            markerLevel = IMarker.SEVERITY_WARNING;
        }
        else
        {
            markerLevel = IMarker.SEVERITY_INFO;
        }

        return markerLevel;
    }

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

        for (int i = 0; i < comboButtons.length; i++)
        {
            String prefName = PreferenceConstants.SOURCE_CRITIC_SEVERITY_LEVEL + (i + 1);
            storeString(prefName, comboButtons[i].getText());
        }

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
                    customText.setText(loadString(PreferenceConstants.SOURCE_CRITIC_LOCATION,
                            DEFAULT_LOCATION));
                    customText.setEnabled(useCustomButton.getSelection());

                    validateLocation(customText.getText());
                }
            });

        // TODO: validation job to ensure this is correct?
        customText = WidgetUtils.createText(locations, "Path to perlcritic");
        customText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    validateLocation(customText.getText());
                }
            });

        WidgetUtils.createLabel(composite, "Severity Marker Settings");

        Composite markerStyles = WidgetUtils.createGroup(composite, GridData.FILL_HORIZONTAL);

        for (int i = 0; i < comboButtons.length; i++)
        {
            Composite c = WidgetUtils.createComposite(markerStyles, 2);

            WidgetUtils.createLabel(c, "Level " + (i + 1));
            comboButtons[i] = new Combo(c, SWT.READ_ONLY);
            comboButtons[i].setItems(LEVELS);
        }

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

        for (int i = 0; i < comboButtons.length; i++)
        {
            comboButtons[i].setText("Warning");
        }

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

    private void loadPreferences()
    {
        enabledButton.setSelection(loadBoolean(PreferenceConstants.SOURCE_CRITIC_ENABLED));

        boolean useDefault = loadBoolean(PreferenceConstants.SOURCE_CRITIC_DEFAULT_LOCATION);
        useDefaultButton.setSelection(useDefault);
        useCustomButton.setSelection(! useDefault);

        customText.setText(loadString(PreferenceConstants.SOURCE_CRITIC_LOCATION,
                DEFAULT_LOCATION));

        for (int i = 0; i < comboButtons.length; i++)
        {
            String text =
                loadString(PreferenceConstants.SOURCE_CRITIC_SEVERITY_LEVEL + (i + 1), "Warning");
            comboButtons[i].setText(text);
        }
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

    private void toggleCriticEnabled(boolean enabled)
    {
        System.setProperty(PreferenceConstants.SOURCE_CRITIC_ENABLED, Boolean.toString(enabled));

        useDefaultButton.setEnabled(enabled);
        useCustomButton.setEnabled(enabled);

        customText.setEnabled((enabled && ! useDefaultButton.getSelection()));

        for (int i = 0; i < comboButtons.length; i++)
        {
            comboButtons[i].setEnabled(enabled);
        }
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
