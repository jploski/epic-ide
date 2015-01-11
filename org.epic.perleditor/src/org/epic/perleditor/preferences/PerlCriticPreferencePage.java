package org.epic.perleditor.preferences;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.epic.core.builders.PerlCriticBuilderHelper;
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

    private Button jobEnabledButton;

    private List<String> errors = new ArrayList<String>(5);
    private Button useCustomButton;

    private Button useDefaultButton;

	private Combo severityOptions;

	private Text otherOptions;

    //~ Methods

    public static int getMarkerSeverity(int severity)
    {
        Assert.isTrue((severity > 0) && (severity <= 5));

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
    
    public static boolean isPerlCriticEnabled()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
            PreferenceConstants.SOURCE_CRITIC_ENABLED);
    }
    
    public static boolean isPerlCriticJobEnabled()
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
            PreferenceConstants.SOURCE_CRITIC_JOB_ENABLED);
    }
    
    public static String getSeverity()
    {
    	 return PerlEditorPlugin.getDefault().getPreferenceStore().getString(
                 PreferenceConstants.SOURCE_CRITIC_SEVERITY);
    }
    
    public static String getOtherOptions() {
    	 return PerlEditorPlugin.getDefault().getPreferenceStore().getString(
                 PreferenceConstants.SOURCE_CRITIC_OTHEROPTIONS);
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
        storeBoolean(PreferenceConstants.SOURCE_CRITIC_JOB_ENABLED, jobEnabledButton.getSelection());
        storeBoolean(PreferenceConstants.SOURCE_CRITIC_DEFAULT_LOCATION,
            useDefaultButton.getSelection());

        String location = customText.getText().equals("") ? DEFAULT_LOCATION : customText.getText();
        storeString(PreferenceConstants.SOURCE_CRITIC_LOCATION, location);

        for (int i = 0; i < comboButtons.length; i++)
        {
            String prefName = PreferenceConstants.SOURCE_CRITIC_SEVERITY_LEVEL + (i + 1);
            storeString(prefName, comboButtons[i].getText());
        }
        
        storeString(PreferenceConstants.SOURCE_CRITIC_SEVERITY, severityOptions.getText());
        storeString(PreferenceConstants.SOURCE_CRITIC_OTHEROPTIONS, otherOptions.getText());

        // invalidate PerlCriticBuilderHelper to force update of settings
        PerlCriticBuilderHelper.destroy();
        
        return super.performOk();
    }

    protected Control createContents(final Composite parent)
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

        jobEnabledButton = WidgetUtils.createButton(composite, "Run Critic automatically", SWT.CHECK | SWT.LEFT);
        jobEnabledButton.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    toggleCriticJobEnabled(jobEnabledButton.getSelection());
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

        Composite buttonComposite = new Composite(locations, SWT.NULL);

        GridLayout buttonLayout = new GridLayout(2, false);
        buttonComposite.setLayout(buttonLayout);

        // TODO: validation job to ensure this is correct?
        customText = WidgetUtils.createText(buttonComposite, "Path to perlcritic");
        customText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    validateLocation(customText.getText());
                }
            });

        Button browseButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

        browseButton.setText("..."); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                FileDialog fileBrowser = new FileDialog(parent.getShell());
                String dir = fileBrowser.open();
                if (dir != null) {
                    customText.setText(dir);
                }
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
        
        Composite runOptionGroup = WidgetUtils.createGroup(composite, GridData.FILL_HORIZONTAL); 
        ((Group)runOptionGroup).setText("Run Options");
        
        Composite runOptionContent = WidgetUtils.createComposite(runOptionGroup, 2);
        
        WidgetUtils.createLabel(runOptionContent, "Severity");
        severityOptions = new Combo(runOptionContent, SWT.READ_ONLY);
        severityOptions.setItems(new String[] {"default", "gentle", "stern", "harsh", "cruel", "brutal"});
        
        WidgetUtils.createLabel(runOptionContent, "Other options");
        otherOptions = WidgetUtils.createText(runOptionContent, "");

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
        jobEnabledButton.setSelection(false);
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
            addErrorMessage(errors.get(errors.size() - 1));
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
        jobEnabledButton.setSelection(loadBoolean(PreferenceConstants.SOURCE_CRITIC_JOB_ENABLED));

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
        
        severityOptions.setText(loadString(PreferenceConstants.SOURCE_CRITIC_SEVERITY, "default"));
        otherOptions.setText(loadString(PreferenceConstants.SOURCE_CRITIC_OTHEROPTIONS, ""));
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
        otherOptions.setEnabled(enabled);
        severityOptions.setEnabled(enabled);
        jobEnabledButton.setEnabled(enabled);
        
        customText.setEnabled((enabled && ! useDefaultButton.getSelection()));

        for (int i = 0; i < comboButtons.length; i++)
        {
            comboButtons[i].setEnabled(enabled);
        }
    }

    private void toggleCriticJobEnabled(boolean enabled)
    {
        System.setProperty(PreferenceConstants.SOURCE_CRITIC_JOB_ENABLED, Boolean.toString(enabled));
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
