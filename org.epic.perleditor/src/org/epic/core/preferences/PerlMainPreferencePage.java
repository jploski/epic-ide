package org.epic.core.preferences;

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

public class PerlMainPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private Text executableText;
	private Button warningsCheckBox;
	private Button taintCheckBox;
	private Scale syntaxCheckInterval;
	private Combo interpreterTypeCombo;
	private Label syntaxIntervalSecondsLabel;
	private Composite fParent;
	private String[] intepreterTypes = {PerlEditorPlugin.INTERPRETER_TYPE_STANDARD,PerlEditorPlugin.INTERPRETER_TYPE_CYGWIN};

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
					executableText.setText(dir);
				}
			}
		});

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		executableText.setLayoutData(data);

		executableText.setText(
			PerlEditorPlugin.getDefault().getExecutablePreference());

/*
		Label executableInfoLabel = new Label(top, SWT.NONE);
		executableInfoLabel.setText(
			"(Windows users, please specify path with forward slashes '/')");

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		Label dummy = new Label(top, SWT.CHECK);
		dummy.setLayoutData(data);
		*/

		//data = new GridData(GridData.FILL_HORIZONTAL);
		//data.grabExcessHorizontalSpace = true;
		new Label(buttonComposite, SWT.NONE).setText("Interpreter type:");
		interpreterTypeCombo = new Combo(buttonComposite, SWT.READ_ONLY);
	    interpreterTypeCombo.setItems(intepreterTypes);
		interpreterTypeCombo.setText(PerlEditorPlugin.getDefault().getPreferenceStore().getString(PerlEditorPlugin.INTERPRETER_TYPE_PREFERENCE));
					

		
		// Warning preference
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		warningsCheckBox = new Button(top, SWT.CHECK);
		warningsCheckBox.setText("Enable warnings");
		warningsCheckBox.setSelection(
			PerlEditorPlugin.getDefault().getWarningsPreference());
		warningsCheckBox.setLayoutData(data);
		
		// Taint check preference
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		taintCheckBox = new Button(top, SWT.CHECK);
		taintCheckBox.setText("Enable taint mode");
		taintCheckBox.setSelection(
			PerlEditorPlugin.getDefault().getTaintPreference());
		taintCheckBox.setLayoutData(data);
		
		
		
		Composite syntaxIntervalComposite = new Composite(top, SWT.NULL);

		GridLayout syncIntervalLayout = new GridLayout();
		syncIntervalLayout.numColumns = 3;
		syntaxIntervalComposite.setLayout(syncIntervalLayout);
		data =
			new GridData(
				GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
		syntaxIntervalComposite.setLayoutData(data);

	    

		Label syncIntevalLabel = new Label(syntaxIntervalComposite, SWT.NONE);
		syncIntevalLabel.setText("Validate source when idle for ");
	
		syntaxCheckInterval = new Scale(syntaxIntervalComposite, SWT.HORIZONTAL);
		syntaxCheckInterval.setMinimum(1);
		syntaxCheckInterval.setMaximum(3000);
		syntaxCheckInterval.setIncrement(100);
		
		syntaxIntervalSecondsLabel = new Label(syntaxIntervalComposite, SWT.NONE);
		int interval = PerlEditorPlugin.getDefault().getPreferenceStore().getInt(PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_PREFERENCE) ;
		syntaxIntervalSecondsLabel.setText(interval + " milliseconds" + "   ");
		syntaxCheckInterval.setSelection(interval);
		
		syntaxCheckInterval.addListener (SWT.Selection, new Listener () {
						public void handleEvent (Event event) {
							syntaxIntervalSecondsLabel.setText(syntaxCheckInterval.getSelection() + " milliseconds" + "   ");
						}
				});
			
		
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
		executableText.setText(
			PerlEditorPlugin.getDefault().getDefaultExecutablePreference());
			
		warningsCheckBox.setSelection(
			PerlEditorPlugin.getDefault().getDefaultWarningsPreference());
		taintCheckBox.setSelection(
					PerlEditorPlugin.getDefault().getDefaultTaintPreference());
		interpreterTypeCombo.setText(PerlEditorPlugin.INTERPRETER_TYPE_STANDARD);

		syntaxIntervalSecondsLabel.setText(PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_DEFAULT + " milliseconds");
		syntaxCheckInterval.setSelection(PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_DEFAULT);
	    
		//colorEditor.loadDefault();
	}
	/** 
	 * Method declared on IPreferencePage. Save the
	 * color preference to the preference store.
	 */
	public boolean performOk() {
		PerlEditorPlugin.getDefault().setExecutablePreference(
			executableText.getText());
		PerlEditorPlugin.getDefault().setWarningsPreference(
			warningsCheckBox.getSelection());
		PerlEditorPlugin.getDefault().setTaintPreference(
					taintCheckBox.getSelection());
			
		PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PerlEditorPlugin.INTERPRETER_TYPE_PREFERENCE, interpreterTypeCombo.getText());
		
		PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_PREFERENCE, syntaxCheckInterval.getSelection());
		return super.performOk();
	}
}