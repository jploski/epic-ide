package org.epic.debug.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;

/**
 * A control for setting the project and file associated with a launch
 * configuration.
 * 
 * @author Katrin Dust
 * 
 */
public class ProjectAndFileBlock extends ProjectBlock
{
    private Text fMainText;
    private Button fileButton;

	/**
	 * This method creates the file and project part of the
	 * LaunchConfigurationTab.
	 * 
	 * @param parent
	 *            a composite for the tab
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
        super.createControl(parent);

        Composite comp = (Composite) getControl();
		createVerticalSpacer(comp, 1);
        createScriptFileEditor(comp);
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
        super.setDefaults(config);

		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
				"");
	}

	/**
	 * Initializes the file and project names from a given configuration.
	 * 
	 * @param configuration
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
        super.initializeFrom(config);

		String mainTypeName = "";
		try {
			mainTypeName = config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE, "");
		} catch (CoreException ce) {
			PerlDebugPlugin.log(ce);
		}
		fMainText.setText(mainTypeName);

	}

	/**
	 * Saves the project and file values in the configuration
	 * 
	 * @param configuration
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
        super.performApply(config);

		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
				(String) fMainText.getText());
	}

	/**
	 * Returns the name
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "project and file";
	}

	/**
	 * Tests, if the project and file names are valid and sets otherwise
	 * errormessages. The following is examined:
	 * <ul>
	 * <li> if a project is inserted
	 * <li> if the project exists in the current workspace
	 * <li> if a file is inserted
	 * <li> if the file belongs to the current project
	 * </ul>
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);
        
        boolean valid = super.isValid(config);
        if (!valid) return false;

		String name = fMainText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage("Startup File is not specified");
			return false;
		}
		String[] files = getPerlFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(name)) {
				return true;
			}
		}
		setErrorMessage("File does not exist or match to the project");
		return false;
	}
    
    private void createScriptFileEditor(Composite parent) {
        Font font = parent.getFont();
        Group group = new Group(parent, SWT.NONE);
        group.setText("File to e&xecute:");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setFont(font);
        
        fMainText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fMainText.setLayoutData(gd);
        fMainText.setFont(font);
        fMainText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        fileButton = createPushButton(group, "&Search...", null);
        fileButton.addSelectionListener(new ButtonListener());
    }

	/**
	 * Returns all Perl files which belong to the current project.
	 * 
	 * @return list of files matching the current project
	 */
	private String[] getPerlFiles() {
		String projectName = getSelectedProject();

		if (projectName == null || projectName.length() == 0) {
			return (new String[] {});
		}

		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(projectName);
		IResourceVisitor visitor = new PerlProjectVisitor();
		try {
			project.accept(visitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return ((PerlProjectVisitor) visitor).getList();
	}
    
    protected void newProjectSelected()
    {
        super.newProjectSelected();
        fMainText.setText("");
    }

	/**
	 * Used to get the Perl files of a current project.
	 */
	private static class PerlProjectVisitor implements IResourceVisitor {
		private static final String PERL_EDITOR_ID = "org.epic.perleditor.editors.PerlEditor";

		private static final String EMB_PERL_FILE_EXTENSION = "epl";

		private List fileList = new ArrayList();

		public boolean visit(IResource resource) throws CoreException {
			IEditorDescriptor defaultEditorDescriptor = PerlDebugPlugin
					.getDefault().getWorkbench().getEditorRegistry()
					.getDefaultEditor(resource.getFullPath().toString());

			if (defaultEditorDescriptor == null) {
				return true;
			}

			if (defaultEditorDescriptor.getId().equals(PERL_EDITOR_ID)
					&& !resource.getFileExtension().equals(
							EMB_PERL_FILE_EXTENSION)) {
				fileList.add(resource.getFullPath().removeFirstSegments(1)
						.toString());
			}
			return true;
		}

		public String[] getList() {
			return (String[]) fileList.toArray(new String[fileList.size()]);
		}
	}

	/**
	 * A listener to update for text changes and widget selection. Creates a
	 * selection dialog for the buttons.
	 */
	private class ButtonListener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent e) {
			String[] files = getPerlFiles();

			ILabelProvider labelProvider = new FileLabelProvider();
			ElementListSelectionDialog dialog = new ElementListSelectionDialog(
					getShell(), labelProvider);
			dialog.setTitle("File Selection");
			dialog.setMessage("Matching files:");
			dialog.setElements(files);

			String perlFile = fMainText.getText();
			if (perlFile != null) {
				dialog.setInitialSelections(new Object[] { perlFile });
			}
			if (dialog.open() == ElementListSelectionDialog.OK) {
				fMainText.setText((String) dialog.getFirstResult());
			}
		}
	}

	/**
	 * A label provider for Perl files.
     * 
	 * @author Katrin Dust
	 */
	private static class FileLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return AbstractUIPlugin.imageDescriptorFromPlugin(
					PerlDebugPlugin.getDefault().toString(), "icons/epic.gif")
					.createImage();
		}
	}
}
