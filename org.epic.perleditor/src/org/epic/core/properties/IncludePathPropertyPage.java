package org.epic.core.properties;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import java.io.File;

import org.epic.core.util.XMLUtilities;

public class IncludePathPropertyPage extends PropertyPage {
	//plements IWorkbenchPropertyPage {

	//The list that displays the current bad words
	private List incPathList;

	private Composite fParent;

	private String filterPath = null;

	//The newEntryText is the text where new bad words are specified
	private Text newEntryText;

	private IProject project;

	public IncludePathPropertyPage() {
		super();
	}

	public void createControl(Composite parent) {
		noDefaultAndApplyButton();
		fParent = parent;
		super.createControl(parent);

		IResource resource = (IResource) getElement().getAdapter(
				IResource.class);
		project = resource.getProject();
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {

		Composite entryTable = new Composite(parent, SWT.NULL);

		//Create a data that takes up the extra space in the dialog .
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		entryTable.setLayoutData(data);

		GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);

		//Add in a dummy label for spacing
		new Label(entryTable, SWT.NONE);

		incPathList = new List(entryTable, SWT.BORDER);
		//TODO
		// incPathList.setItems(BadWordCheckerPlugin.getDefault().getBadWordsPreference());

		//Create a data that takes up the extra space in the dialog and spans
		// both columns.
		data = new GridData(GridData.FILL_BOTH);
		incPathList.setLayoutData(data);

		Composite buttonComposite = new Composite(entryTable, SWT.NULL);

		GridLayout buttonLayout = new GridLayout();
		//buttonLayout.numColumns = 2;
		buttonLayout.numColumns = 3;
		buttonComposite.setLayout(buttonLayout);

		//Create a data that takes up the extra space in the dialog and spans
		// both columns.
		data = new GridData(GridData.FILL_BOTH
				| GridData.VERTICAL_ALIGN_BEGINNING);
		buttonComposite.setLayoutData(data);

		Button addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		addButton.setText("Add to List"); //$NON-NLS-1$
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (newEntryText.getText().trim().length() > 0) {
					incPathList.add(newEntryText.getText(), incPathList
							.getItemCount());
					newEntryText.setText("");
				}
			}
		});

		newEntryText = new Text(buttonComposite, SWT.BORDER);
		//Create a data that takes up the extra space in the dialog .
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		newEntryText.setLayoutData(data);

		//TODO
		Button browseButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		browseButton.setText("..."); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dirBrowser = new DirectoryDialog(fParent
						.getShell());
				dirBrowser.setFilterPath(filterPath);
				String dir = dirBrowser.open();
				if (dir != null) {
					String projectDir = project.getLocation().toString();
					if (dir.equals(projectDir)) {
						dir = "${project_loc}";
					} else if (dir.startsWith(projectDir)) {
						dir = "${resource_loc:" + project.getFullPath().toString() + dir.substring(projectDir.length()) + "}";
					}
					newEntryText.setText(dir);
					filterPath = dir.substring(0, dir
							.lastIndexOf(File.separator));

				}
			}
		});

		Button removeButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		removeButton.setText("Remove Selection"); //$NON-NLS-1$
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				incPathList.remove(incPathList.getSelectionIndex());
			}
		});

		data = new GridData();
		data.horizontalSpan = 2;
		removeButton.setLayoutData(data);

		// Initialize list
		IResource resource = (IResource) getElement().getAdapter(
				IResource.class);
		XMLUtilities xmlUtil = new XMLUtilities();
		String[] listEntries = xmlUtil.getIncludeEntries(resource.getProject());
		if (listEntries != null) {
			incPathList.setItems(listEntries);
		}

		return entryTable;
	}

	public boolean performOk() {
		try {
			XMLUtilities xmlUtil = new XMLUtilities();
			xmlUtil.writeIncludeEntries(project, incPathList.getItems());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
