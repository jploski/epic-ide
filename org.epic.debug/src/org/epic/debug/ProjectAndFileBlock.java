package org.epic.debug;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * A control for setting the project and file associated with a launch
 * configuration.
 * 
 * @author Katrin Dust
 * 
 */
public class ProjectAndFileBlock extends AbstractLaunchConfigurationTab {

	/**
	 * the actual project
	 */
	protected Text fProjText;

	/**
	 * the actual file to execute
	 */
	protected Text fMainText;

	/**
	 * Button to search a file to execute
	 */
	protected Button fileButton;

	/**
	 * button to browse a project
	 */
	protected Button projectButton;

	/**
	 * perl nature id
	 */
	private static final String PERL_NATURE_ID = "org.epic.perleditor.perlnature";

	/**
	 * listener for file- and projectButton
	 */
	private ButtonListener buttonListener = new ButtonListener();

	/**
	 * The last launch config this tab was initialized from
	 */
	protected ILaunchConfiguration fLaunchConfiguration;

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

		// creates a project text field
		Font font = parent.getFont();
		Composite projComp = new Composite(parent, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 2;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);
		projComp.setFont(font);
		Label fProjLabel = new Label(projComp, SWT.NONE);
		fProjLabel.setText("&Project:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fProjLabel.setLayoutData(gd);
		fProjLabel.setFont(font);
		fProjText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.setFont(font);
		// update the configuartion by modification
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		// creates a browse button
		projectButton = createPushButton(projComp, "Browse...", null);
		projectButton.addSelectionListener(buttonListener);

		createVerticalSpacer(projComp, 1);

		// creates a file text field
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		projComp.setLayout(mainLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);
		projComp.setFont(font);
		Label fMainLabel = new Label(projComp, SWT.NONE);
		fMainLabel.setText("File to execute:");
		gd = new GridData();
		gd.horizontalSpan = 2;
		fMainLabel.setLayoutData(gd);
		fMainLabel.setFont(font);
		fMainText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fMainText.setLayoutData(gd);
		fMainText.setFont(font);
		// update the configuartion by modification
		fMainText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		fileButton = createPushButton(projComp, "Search...", null);
		fileButton.addSelectionListener(buttonListener);

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
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				""); //$NON-NLS-1$

		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
				"");

	}

	/**
	 * Initialize the file and project names from a given configuration.
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
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				(String) fProjText.getText());
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

		name = fMainText.getText().trim();
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

	/**
	 * Returns a String array with all Perl projects in the current workspace
	 * 
	 * @return String[] List of Perl projects
	 */
	private String[] getPerlProjects() {
		List projectList = new ArrayList();
		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			try {
				if (project.isAccessible() && project.hasNature(PERL_NATURE_ID)) {
					projectList.add(project.getName());
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return (String[]) projectList.toArray(new String[projectList.size()]);
	}

	/**
	 * Returns all perl files, which belongs to the current project.
	 * 
	 * @return String [] List of files matchign to the current project
	 */
	private String[] getPerlFiles() {
		String projectName = fProjText.getText();

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

	/**
	 * Used to get the perl files to a current project.
	 * 
	 */
	class PerlProjectVisitor implements IResourceVisitor {
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
			Object source = e.getSource();
			// dialog to browse a project
			if (source == projectButton) {
				String[] projects;
				projects = getPerlProjects();
				ILabelProvider labelProvider = new ProjectLabelProvider();
				ElementListSelectionDialog dialog = new ElementListSelectionDialog(
						getShell(), labelProvider);
				dialog.setTitle("Project Selection");
				dialog.setMessage("Choose a project");
				dialog.setElements(projects);
				if (dialog.open() == ElementListSelectionDialog.OK) {
					if (!fProjText.getText().equals(
							(String) dialog.getFirstResult())) {
						fProjText.setText((String) dialog.getFirstResult());
						fMainText.setText("");
					}
				}
				// dialog to search a file
			} else if (source == fileButton) {
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
	}

	/**
	 * A label provider for projects.
	 * @author Katrin
	 * 
	 */
	private class ProjectLabelProvider extends LabelProvider {

		public Image getImage(Object element) {
			return AbstractUIPlugin.imageDescriptorFromPlugin(
					PerlDebugPlugin.getDefault().toString(),
					"icons/project_folder.gif").createImage();
		}
	}

	/**
	 * a label provider for perl files
	 * @author Katrin
	 * 
	 */
	private class FileLabelProvider extends LabelProvider {

		public Image getImage(Object element) {
			return AbstractUIPlugin.imageDescriptorFromPlugin(
					PerlDebugPlugin.getDefault().toString(), "icons/epic.gif")
					.createImage();
		}
	}

	/**
	 * Sets the Perl project currently specified by the given launch config, if
	 * any.
	 */
	protected void setLaunchConfiguration(ILaunchConfiguration config) {
		fLaunchConfiguration = config;
	}

	/**
	 * Returns the current java project context
	 */
	protected ILaunchConfiguration getLaunchConfiguration() {
		return fLaunchConfiguration;
	}

}
