package org.epic.core.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PerlMainPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private Text executableText;
	private Text browserLabelText;
	private Text debugPreviewKeysText;
	private Button warningsCheckBox;
	private Button methodsCheckBox;
	private Button taintCheckBox;
    private Button debugConsoleCheckBox;
    private Button suspendAtFirstCheckBox;

	private Button validateCheckBox;
	private Scale syntaxCheckInterval;
	private Combo interpreterTypeCombo;
	private Label syntaxIntervalSecondsLabel;
	private Composite fParent;
	private String[] intepreterTypes = {
        PreferenceConstants.DEBUG_INTERPRETER_TYPE_STANDARD,
        PreferenceConstants.DEBUG_INTERPRETER_TYPE_CYGWIN
        };

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {

		fParent = parent;

		Composite top = new Composite(parent, SWT.NULL);

		//Create a data that takes up the extra space in the dialog .
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		top.setLayoutData(data);

		GridLayout layout = new GridLayout();
		top.setLayout(layout);

		Composite buttonComposite = new Composite(top, SWT.NULL);

		GridLayout buttonLayout = new GridLayout();
		//buttonLayout.numColumns = 2;
		buttonLayout.numColumns = 3;
		buttonComposite.setLayout(buttonLayout);

		//Create a data that takes up the extra space in the dialog and spans both columns.
		data =
			new GridData(
				GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
		buttonComposite.setLayoutData(data);

		Label executableLabel = new Label(buttonComposite, SWT.NONE);
		executableLabel.setText("Perl executable:");

		executableText = new Text(buttonComposite, SWT.BORDER);

		Button browseButton =
			new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		browseButton.setText("..."); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				FileDialog fileBrowser = new FileDialog(fParent.getShell());
				String dir = fileBrowser.open();
				if (dir != null) {
					// Surround interpreter name by ""
					executableText.setText("\"" + dir + "\"");
				}
			}
		});

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		executableText.setLayoutData(data);

		executableText.setText(
			PerlEditorPlugin.getDefault().getPerlExecutable());

/*
		Label executableInfoLabel = new Label(top, SWT.NONE);
		executableInfoLabel.setText(
			"(Windows users, please specify path with forward slashes '/')");

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		Label dummy = new Label(top, SWT.CHECK);
		dummy.setLayoutData(data);
		*/

//		data = new GridData(GridData.FILL_HORIZONTAL);
//		data.grabExcessHorizontalSpace = true;
		new Label(buttonComposite, SWT.NONE).setText("Interpreter type:");
		interpreterTypeCombo = new Combo(buttonComposite, SWT.READ_ONLY);
	    interpreterTypeCombo.setItems(intepreterTypes);
		interpreterTypeCombo.setText(
            PerlEditorPlugin.getDefault().getPreferenceStore().getString(
                PreferenceConstants.DEBUG_INTERPRETER_TYPE));

		// Warning preference
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		warningsCheckBox = new Button(top, SWT.CHECK);
		warningsCheckBox.setText("Enable warnings");
		warningsCheckBox.setSelection(
			PerlEditorPlugin.getDefault().getBooleanPreference(
                PreferenceConstants.DEBUG_SHOW_WARNINGS));
		warningsCheckBox.setLayoutData(data);
		
		// Warning preference
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		methodsCheckBox = new Button(top, SWT.CHECK);
		methodsCheckBox.setText("Enable Method::Signature keywords");
		methodsCheckBox.setSelection(
			PerlEditorPlugin.getDefault().getBooleanPreference(
                PreferenceConstants.DEBUG_METHOD_SIGNATURES));
		methodsCheckBox.setLayoutData(data);

		// Taint check preference
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		taintCheckBox = new Button(top, SWT.CHECK);
		taintCheckBox.setText("Enable taint mode");
		taintCheckBox.setSelection(
            PerlEditorPlugin.getDefault().getBooleanPreference(
			    PreferenceConstants.DEBUG_TAINT_MODE));
		taintCheckBox.setLayoutData(data);

        // Debugger console (experimental)
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        debugConsoleCheckBox = new Button(top, SWT.CHECK);
        debugConsoleCheckBox.setText("Enable debugger console (experimental)");
        debugConsoleCheckBox.setSelection(
            PerlEditorPlugin.getDefault().getBooleanPreference(
                PreferenceConstants.DEBUG_DEBUG_CONSOLE));
        debugConsoleCheckBox.setLayoutData(data);        

        // Stop debugger at first line
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        suspendAtFirstCheckBox = new Button(top, SWT.CHECK);
        suspendAtFirstCheckBox.setText("Suspend debugger at first statement");
        suspendAtFirstCheckBox.setSelection(
            PerlEditorPlugin.getDefault().getBooleanPreference(
                PreferenceConstants.DEBUG_SUSPEND_AT_FIRST));
        suspendAtFirstCheckBox.setLayoutData(data);
        
		//WebBrowser preferences
		Composite browserComposite = new Composite(top, SWT.NULL);
		GridLayout browserLayout = new GridLayout();
		browserLayout.numColumns = 2;
		browserComposite.setLayout(browserLayout);
		data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
		browserComposite.setLayoutData(data);
		
		Label browserLabel=new Label(browserComposite, SWT.NONE);
		browserLabel.setText("Default Web-Start page:");
		
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		browserLabelText = new Text(browserComposite, SWT.BORDER);
		browserLabelText.setLayoutData(data);
		browserLabelText.setText(
			PerlEditorPlugin.getDefault().getPreferenceStore().getString(
                PreferenceConstants.BROWSER_START_URL));

	    // "Object identifying" hash keys for previewing hashes in debugger
        Composite idKeysComposite = new Composite(top, SWT.NULL);
        GridLayout idKeysLayout = new GridLayout();
        idKeysLayout.numColumns = 2;
        idKeysComposite.setLayout(idKeysLayout);
        data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
        idKeysComposite.setLayoutData(data);
        
        Label debugPreviewKeysLabel = new Label(browserComposite, SWT.NONE);
        debugPreviewKeysLabel.setText("Debugger preview keys:");
        
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        debugPreviewKeysText = new Text(browserComposite, SWT.BORDER);
        debugPreviewKeysText.setLayoutData(data);
        debugPreviewKeysText.setText(
            PerlEditorPlugin.getDefault().getPreferenceStore().getString(
                PreferenceConstants.DEBUG_PREVIEW_KEYS));
		
		Composite syntaxIntervalComposite = new Composite(top, SWT.NULL);

		GridLayout syncIntervalLayout = new GridLayout();
		syncIntervalLayout.numColumns = 3;
		syntaxIntervalComposite.setLayout(syncIntervalLayout);
		data =
			new GridData(
				GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
		syntaxIntervalComposite.setLayoutData(data);

		validateCheckBox = new Button(syntaxIntervalComposite, SWT.CHECK);
		validateCheckBox.setText("Validate source when idle for ");
		validateCheckBox.setSelection(
            PerlEditorPlugin.getDefault().getBooleanPreference(
                PreferenceConstants.EDITOR_SYNTAX_VALIDATION));	
		syntaxCheckInterval = new Scale(syntaxIntervalComposite, SWT.HORIZONTAL);
		syntaxCheckInterval.setMinimum(1);
		syntaxCheckInterval.setMaximum(10000);
		syntaxCheckInterval.setIncrement(100);
		
		syntaxIntervalSecondsLabel = new Label(syntaxIntervalComposite, SWT.NONE);
        displayInterval(PerlEditorPlugin.getDefault().getPreferenceStore().getInt(
            PreferenceConstants.EDITOR_SYNTAX_VALIDATION_INTERVAL));
		
		syntaxCheckInterval.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event event)
            {
                displayInterval(syntaxCheckInterval.getSelection());
            } });
			
		
		syntaxIntervalComposite.setLayoutData(data);

		return new Composite(parent, SWT.NULL);
	}

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		//Initialize the preference store we wish to use
		setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Performs special processing when this page's Restore Defaults button has 
	 * been pressed.
	 * Sets the contents of the color field to the default value in the preference
	 * store.
	 */
	protected void performDefaults() {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        
		executableText.setText(
			prefs.getDefaultString(PreferenceConstants.DEBUG_PERL_EXECUTABLE));
		warningsCheckBox.setSelection(
			prefs.getDefaultBoolean(PreferenceConstants.DEBUG_SHOW_WARNINGS));
		methodsCheckBox.setSelection(
			prefs.getDefaultBoolean(PreferenceConstants.DEBUG_METHOD_SIGNATURES));
		taintCheckBox.setSelection(
            prefs.getDefaultBoolean(PreferenceConstants.DEBUG_TAINT_MODE));
        debugConsoleCheckBox.setSelection(
            prefs.getDefaultBoolean(PreferenceConstants.DEBUG_DEBUG_CONSOLE));
        suspendAtFirstCheckBox.setSelection(
            prefs.getDefaultBoolean(PreferenceConstants.DEBUG_SUSPEND_AT_FIRST));
		interpreterTypeCombo.setText(
            prefs.getDefaultString(PreferenceConstants.DEBUG_INTERPRETER_TYPE));
		browserLabelText.setText(
			prefs.getDefaultString(PreferenceConstants.BROWSER_START_URL));
        debugPreviewKeysText.setText(
            prefs.getDefaultString(PreferenceConstants.DEBUG_PREVIEW_KEYS));
		validateCheckBox.setSelection(
		    prefs.getDefaultBoolean(PreferenceConstants.EDITOR_SYNTAX_VALIDATION));
        int defaultInterval = prefs.getDefaultInt(
            PreferenceConstants.EDITOR_SYNTAX_VALIDATION_INTERVAL);
        displayInterval(defaultInterval);
	}
	/** 
	 * Method declared on IPreferencePage. Save the
	 * color preference to the preference store.
	 */
	public boolean performOk() {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        
		PerlEditorPlugin.getDefault().setPerlExecutable(
			executableText.getText());
        prefs.setValue(
            PreferenceConstants.DEBUG_SHOW_WARNINGS,
            warningsCheckBox.getSelection());
        prefs.setValue(
            PreferenceConstants.DEBUG_METHOD_SIGNATURES,
            methodsCheckBox.getSelection());
		prefs.setValue(
            PreferenceConstants.DEBUG_TAINT_MODE,
			taintCheckBox.getSelection());
        prefs.setValue(
            PreferenceConstants.DEBUG_DEBUG_CONSOLE,
            debugConsoleCheckBox.getSelection());
        prefs.setValue(
            PreferenceConstants.DEBUG_SUSPEND_AT_FIRST,
            suspendAtFirstCheckBox.getSelection());
		prefs.setValue(
            PreferenceConstants.EDITOR_SYNTAX_VALIDATION,
            validateCheckBox.getSelection());
		prefs.setValue(
            PreferenceConstants.DEBUG_INTERPRETER_TYPE,
            interpreterTypeCombo.getText());
		prefs.setValue(
            PreferenceConstants.EDITOR_SYNTAX_VALIDATION_INTERVAL,
            syntaxCheckInterval.getSelection());
		prefs.setValue(
            PreferenceConstants.BROWSER_START_URL,
            browserLabelText.getText());
        prefs.setValue(
            PreferenceConstants.DEBUG_PREVIEW_KEYS,
            debugPreviewKeysText.getText());
		
		return super.performOk();
	}
    
    private void displayInterval(int interval)
    {
        float intervalDisplay = Math.round(interval/10f)/100f;
        syntaxIntervalSecondsLabel.setText(intervalDisplay + " seconds ");
        syntaxCheckInterval.setSelection(interval);
    }
}