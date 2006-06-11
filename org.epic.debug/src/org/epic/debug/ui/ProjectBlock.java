package org.epic.debug.ui;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;

/**
 * A control for setting the project associated with a launch configuration.
 * 
 * @author Katrin Dust
 */
public class ProjectBlock extends AbstractLaunchConfigurationTab
{
	private Text fProjText;
    private Button projectButton;

	/**
	 * Creates the project part of the LaunchConfigurationTab.
	 * 
	 * @param parent
	 *            a composite for the tab
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
        setControl(parent);
        createProjectEditor(parent);
	}
    
    /**
     * @return the name of the selected project (which may be invalid)
     *         or "" if none has been selected
     */
    public String getSelectedProject()
    {
        return fProjText.getText();
    }

	/**
	 * Sets an empty string as the default values of project and file names.
	 * 
	 * @param configuration
	 *            configuration copy
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
			""); //$NON-NLS-1$
	}

	/**
	 * Initialize the project name from a given configuration.
	 * 
	 * @param configuration
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {

		String projectName = "";
		try {
			projectName = config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		} catch (CoreException ce) {
			PerlDebugPlugin.log(ce);
		}
		fProjText.setText(projectName);
	}

	/**
	 * Saves the project name in the configuration
	 * 
	 * @param configuration
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				(String) fProjText.getText());
	}

	/**
	 * Returns the name
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Project";
	}

	/**
	 * Tests if the selected project is valid and sets an 
	 * error message if the test fails. The following is examined:
	 * <ul>
	 * <li> if the project field is filled out
	 * <li> if the project exists in the current workspace
	 * </ul>
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);

		String name = fProjText.getText().trim();
		if (name.length() > 0) {
			if (!ResourcesPlugin.getWorkspace().getRoot().getProject(name)
					.exists()) {
				setErrorMessage("Project does not exist");
				return false;
			}
		} else {
			setErrorMessage("Specify Project");
			return false;
		}
        return true;
	}
    
    private void createProjectEditor(Composite parent) {
        Font font = parent.getFont();
        Group group = new Group(parent, SWT.NONE);
        group.setText("&Project:");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setFont(font);

        fProjText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fProjText.setLayoutData(gd);
        fProjText.setFont(font);
        fProjText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        projectButton = createPushButton(group, "&Browse...", null);
        projectButton.addSelectionListener(new ButtonListener());
    }
    
    /**
     * Called whenever a new project has been selected through the project 
     * selection dialog. Subclasses may override this no-op method.
     */
    protected void newProjectSelected()
    {
    }

	/**
	 * A listener for the "Browse..." button.
     * Creates a project selection dialog when the button is clicked.
	 */
	private class ButtonListener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			ProjectSelectionDialog dialog =
                new ProjectSelectionDialog(getShell());

			if (dialog.open() == ElementListSelectionDialog.OK) {
				if (!fProjText.getText().equals(
						(String) dialog.getFirstResult())) {
					fProjText.setText((String) dialog.getFirstResult());
                    newProjectSelected();
				}
			}
		}
	}
}
