package org.epic.core.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;
import org.epic.core.util.XMLUtilities;

public class IgnoredPathsPropertyPage extends PropertyPage
{
    private List ignoredPathList;
    private Text newEntryText;
    private IProject project;

    public IgnoredPathsPropertyPage()
    {
    }

    public void createControl(Composite parent)
    {
        noDefaultAndApplyButton();

        super.createControl(parent);

        IResource resource = (IResource) getElement().getAdapter(IResource.class);
        project = resource.getProject();
    }

    protected Control createContents(Composite parent)
    {
        Composite entryTable = new Composite(parent, SWT.NULL);

        // Create a data that takes up the extra space in the dialog
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        entryTable.setLayoutData(data);

        GridLayout layout = new GridLayout();
        entryTable.setLayout(layout);

        // Add in a dummy label for spacing
        Label infoLabel = new Label(entryTable, SWT.NONE);
        infoLabel.setText("Path prefixes excluded from syntax checking (project-relative; * is wildcard)");

        ignoredPathList = new List(entryTable, SWT.BORDER);

        // Create a data that takes up the extra space in the dialog and spans
        // both columns.
        data = new GridData(GridData.FILL_BOTH);
        ignoredPathList.setLayoutData(data);

        Composite buttonComposite = new Composite(entryTable, SWT.NULL);

        GridLayout buttonLayout = new GridLayout();
        // buttonLayout.numColumns = 2;
        buttonLayout.numColumns = 3;
        buttonComposite.setLayout(buttonLayout);

        // Create a data that takes up the extra space in the dialog and spans
        // both columns.
        data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
        buttonComposite.setLayoutData(data);

        Button addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

        addButton.setText("Add to List"); //$NON-NLS-1$
        addButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                if (newEntryText.getText().trim().length() > 0)
                {
                    ignoredPathList.add(newEntryText.getText(), ignoredPathList.getItemCount());
                    newEntryText.setText("");
                }
            }
        });

        newEntryText = new Text(buttonComposite, SWT.BORDER);
        // Create a data that takes up the extra space in the dialog .
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        newEntryText.setLayoutData(data);

        Button removeButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

        removeButton.setText("Remove Selection"); //$NON-NLS-1$
        removeButton.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                if (ignoredPathList.getSelectionIndex() >= 0)
                    ignoredPathList.remove(ignoredPathList.getSelectionIndex());
            }
        });

        data = new GridData();
        data.horizontalSpan = 2;
        removeButton.setLayoutData(data);

        // Initialize list
        IResource resource = (IResource) getElement().getAdapter(IResource.class);
        XMLUtilities xmlUtil = new XMLUtilities();
        String[] listEntries = xmlUtil.getIgnoredEntries(resource.getProject());
        if (listEntries != null)
        {
            ignoredPathList.setItems(listEntries);
        }

        return entryTable;
    }

    public boolean performOk()
    {
        try
        {
            XMLUtilities xmlUtil = new XMLUtilities();
            xmlUtil.writeIgnoredEntries(project, ignoredPathList.getItems());
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}