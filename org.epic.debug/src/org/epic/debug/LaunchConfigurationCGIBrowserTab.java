/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package org.epic.debug;

//import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
//import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
//import org.eclipse.swt.events.SelectionAdapter;
//import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.swt.widgets.Text;
//import org.eclipse.ui.dialogs.ElementListSelectionDialog;
//import org.eclipse.ui.dialogs.SelectionDialog;
//import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.browser.BrowserDescriptor;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.core.resources.IProject;

public class LaunchConfigurationCGIBrowserTab
	extends AbstractLaunchConfigurationTab
{

	/**
	 * A launch configuration tab that displays and edits project and
	 * main type name launch configuration attributes.
	 * <p>
	 * This class may be instantiated. This class is not intended to be subclassed.
	 * </p>
	 * @since 2.0
	 */

	private Table browsersTable;
	private Label customBrowserPathLabel;
	private Text customBrowserPath;
	private Button customBrowserBrowse;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String PERL_NATURE_ID =
		"org.epic.perleditor.perlnature";

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent)
	{
		Font font = parent.getFont();

		//noDefaultAndApplyButton();
		Composite mainComposite = new Composite(parent, SWT.NULL);
		setControl(mainComposite);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		//data.grabExcessHorizontalSpace = true;
		mainComposite.setLayoutData(data);
		mainComposite.setFont(font);

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComposite.setLayout(layout);

		Label description = new Label(mainComposite, SWT.NULL);
		description.setFont(font);
		description.setText("select_browser");
		createSpacer(mainComposite);

		Label tableDescription = new Label(mainComposite, SWT.NULL);
		tableDescription.setFont(font);
		tableDescription.setText("current_browser");
		//data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		//description.setLayoutData(data);
		browsersTable = new Table(mainComposite, SWT.CHECK | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		//.heightHint = convertHeightInCharsToPixels(6);
		browsersTable.setLayoutData(gd);
		browsersTable.setFont(font);
		browsersTable.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent selEvent)
			{
				if (selEvent.detail == SWT.CHECK)
				{
					TableItem item = (TableItem) selEvent.item;
					if (item.getChecked())
					{
						// Deselect others
						TableItem[] items = browsersTable.getItems();
						for (int i = 0; i < items.length; i++)
						{
							if (items[i] == item)
								continue;
							else
								items[i].setChecked(false);
						}
					} else
					{
						// Do not allow deselection
						item.setChecked(true);
					}
					setEnabledCustomBrowserPath();
				}
			}
			public void widgetDefaultSelected(SelectionEvent selEvent)
			{
			}
		});
		// populate table with browsers
		BrowserDescriptor[] aDescs =
			BrowserManager.getInstance().getBrowserDescriptors();
		for (int i = 0; i < aDescs.length; i++)
		{
			TableItem item = new TableItem(browsersTable, SWT.NONE);
			item.setText(aDescs[i].getLabel());
			if (BrowserManager
				.getInstance()
				.getDefaultBrowserID()
				.equals(aDescs[i].getID()))
				item.setChecked(true);
			else
				item.setChecked(false);
			item.setGrayed(aDescs.length == 1);
		}

		createCustomBrowserPathPart(mainComposite);

		
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config)
	{
//		updateProjectFromConfig(config);
//		fMainText.setItems(getPerlFiles());
//		updateMainTypeFromConfig(config);
//		updateParamsFromConfig(config);
	}



	protected void updateParamsFromConfig(ILaunchConfiguration config)
	{
//		String params = ""; //$NON-NLS-1$
//		try
//		{
//			params =
//				config.getAttribute(
//					PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
//					EMPTY_STRING);
//		} catch (CoreException ce)
//		{
//			PerlDebugPlugin.log(ce);
//		}
//		fParamText.setText(params);
	}

	

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
//		config.setAttribute(
//			PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
//			(String) fProjText.getText());
//		config.setAttribute(
//			PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
//			(String) fMainText.getText());
//		config.setAttribute(
//			PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
//			(String) fParamText.getText());
//		config.setAttribute(
//			PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
//			"CGI");
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose()
	{
	}

	/**
	 * Show a dialog that lists all main types
	 */
	protected void handleSearchButtonSelected()
	{

		/*	IJavaProject javaProject = getJavaProject();
			IJavaSearchScope searchScope = null;
			if ((javaProject == null) || !javaProject.exists()) {
				searchScope = SearchEngine.createWorkspaceScope();
			} else {
				searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject}, false);
			}
		
			int constraints = IJavaElementSearchConstants.CONSIDER_BINARIES;
			if (fSearchExternalJarsCheckButton.getSelection()) {
				constraints |= IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS;
			}
		
			Shell shell = getShell();
			SelectionDialog dialog = JavaUI.createMainTypeDialog(shell,
																 getLaunchConfigurationDialog(),
																 searchScope,
																 constraints,
																 false,
																 fMainText.getText());
			dialog.setTitle(LauncherMessages.getString("JavaMainTab.Choose_Main_Type_11")); //$NON-NLS-1$
			dialog.setMessage(LauncherMessages.getString("JavaMainTab.Choose_a_main_&type_to_launch__12")); //$NON-NLS-1$
			if (dialog.open() == SelectionDialog.CANCEL) {
				return;
			}
		
			Object[] results = dialog.getResult();
			if ((results == null) || (results.length < 1)) {
				return;
			}
			IType type = (IType)results[0];
			if (type != null) {
				fMainText.setText(type.getFullyQualifiedName());
				javaProject = type.getJavaProject();
				fProjText.setText(javaProject.getElementName());
			}*/
	}

	/**
	 * Show a dialog that lets the user select a project.  This in turn provides
	 * context for the main type, allowing the user to key a main type name, or
	 * constraining the search for main types to the specified project.
	 */
	protected void handleProjectButtonSelected()
	{
		/*	IJavaProject project = chooseJavaProject();
			if (project == null) {
				return;
			}
		
			String projectName = project.getElementName();
			fProjText.setText(projectName);*/
	}

	/**
	 * Realize a Java Project selection dialog and return the first selected project,
	 * or null if there was none.
	 */
	protected IProject choosePerlProject()
	{
		IProject[] projects;
		try
		{
			//	projects= JavaCore.create(getWorkspaceRoot()).getJavaProjects();
		} catch (Exception e)
		{
			PerlDebugPlugin.log(e);
			//	projects= new IJavaProject[0];
		}
		/*
				ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
				ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
				dialog.setTitle(LauncherMessages.getString("JavaMainTab.Project_Selection_13")); //$NON-NLS-1$
				dialog.setMessage(LauncherMessages.getString("JavaMainTab.Choose_a_&project_to_constrain_the_search_for_main_types__14")); //$NON-NLS-1$
				dialog.setElements(projects);
		
				IJavaProject javaProject = getJavaProject();
				if (javaProject != null) {
					dialog.setInitialSelections(new Object[] { javaProject });
				}
				if (dialog.open() == ElementListSelectionDialog.OK) {
					return (IJavaProject) dialog.getFirstResult();
				}*/
		return null;
	}

	/**
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot()
	{
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config)
	{
		return(true);
//		setErrorMessage(null);
//		setMessage(null);
//
//		String name = fProjText.getText().trim();
//		if (name.length() > 0)
//		{
//			if (!ResourcesPlugin
//				.getWorkspace()
//				.getRoot()
//				.getProject(name)
//				.exists())
//			{
//				setErrorMessage("Project does not exist"); //$NON-NLS-1$
//				return false;
//			}
//		} else
//		{
//			setErrorMessage("Specify Project"); //$NON-NLS-1$
//			return false;
//		}
//
//		name = fMainText.getText().trim();
//		if (name.length() == 0)
//		{
//			setErrorMessage("Startup File is not specified"); //$NON-NLS-1$
//			return false;
//		}
//
//		return true;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config)
	{
		/*	IJavaElement javaElement = getContext();
			if (javaElement != null) {
				initializeJavaProject(javaElement, config);
			} else {*/
		// We set empty attributes for project & main type so that when one config is
		// compared to another, the existence of empty attributes doesn't cause an
		// incorrect result (the performApply() method can result in empty values
		// for these attributes being set on a config if there is nothing in the
		// corresponding text boxes)
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		//	}
		//	initializeMainTypeAndName(javaElement, config);

		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
			"");
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
			"");

	}

	/**
	 * Set the main type & name attributes on the working copy based on the IJavaElement
	 */
	/*	protected void initializeMainTypeAndName(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
			String name= null;
			if (javaElement instanceof IMember) {
				IMember member = (IMember)javaElement;
				if (member.isBinary()) {
					javaElement = member.getClassFile();
				} else {
					javaElement = member.getCompilationUnit();
				}
			}
			if (javaElement instanceof ICompilationUnit || javaElement instanceof IClassFile) {
				try {
					IType[] types = MainMethodFinder.findTargets(new BusyIndicatorRunnableContext(), new Object[] {javaElement});
					if (types != null && (types.length > 0)) {
						// Simply grab the first main type found in the searched element
						name = types[0].getFullyQualifiedName();
					}
				} catch (InterruptedException ie) {
				} catch (InvocationTargetException ite) {
				}
			}
			if (name == null) {
				name= ""; //$NON-NLS-1$
			}
			config.setAttribute(PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE, name);
			if (name.length() > 0) {
				int index = name.lastIndexOf('.');
				if (index > 0) {
					name = name.substring(index + 1);
				}
				name = getLaunchConfigurationDialog().generateName(name);
				config.rename(name);
			}
		}
	*/
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName()
	{
		return "Browser Configuration"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage()
	{
		return (
			PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				PerlDebugImages.DESC_OBJS_LaunchTabMain));
	}

	/**
	 * Returns a String array whith all Perl projects
	 * @return Stiring[] List of Perl projects
	 */
	private String[] getPerlProjects()
	{
		List projectList = new ArrayList();
		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		for (int i = 0; i < projects.length; i++)
		{
			IProject project = projects[i];
			try
			{
				if (project.exists() && project.hasNature(PERL_NATURE_ID))
				{
					//System.out.println("Perl Project: " + project.getName());
					projectList.add(project.getName());
				}
			} catch (CoreException e)
			{
				e.printStackTrace();
			}

		}

		return (String[]) projectList.toArray(new String[projectList.size()]);
	}


//**********************************************************
  private void createSpacer(Composite parent) {
		  Label spacer = new Label(parent, SWT.NONE);
		  GridData data = new GridData();
		  data.horizontalAlignment = GridData.FILL;
		  data.verticalAlignment = GridData.BEGINNING;
		  spacer.setLayoutData(data);
	  }
	  
	  private void setEnabledCustomBrowserPath() {
			  TableItem[] items = browsersTable.getItems();
			  for (int i = 0; i < items.length; i++) {
				  if (items[i].getChecked()) {
					  boolean enabled =
						  (HelpPlugin.PLUGIN_ID + ".custombrowser").equals(
							  BrowserManager
								  .getInstance()
								  .getBrowserDescriptors()[i]
								  .getID());
					  customBrowserPathLabel.setEnabled(enabled);
					  customBrowserPath.setEnabled(enabled);
					  customBrowserBrowse.setEnabled(enabled);
					  break;
				  }
			  }

		  }
		  
		protected void createCustomBrowserPathPart(Composite mainComposite) {
				Font font = mainComposite.getFont();

				// vertical space
				new Label(mainComposite, SWT.NULL);

				Composite bPathComposite = new Composite(mainComposite, SWT.NULL);
				GridLayout layout = new GridLayout();
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				layout.numColumns = 3;
				bPathComposite.setLayout(layout);
				bPathComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				customBrowserPathLabel = new Label(bPathComposite, SWT.LEFT);
				customBrowserPathLabel.setFont(font);
			//customBrowserPathLabel.setText(WorkbenchResources.getString("CustomBrowserPreferencePage.Program")); //$NON-NLS-1$

				customBrowserPath = new Text(bPathComposite, SWT.BORDER);
				customBrowserPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				customBrowserPath.setFont(font);
				//customBrowserPath.setText(
				//	HelpPlugin.getDefault().getPluginPreferences().getString(
				//		CustomBrowser.CUSTOM_BROWSER_PATH_KEY));

				customBrowserBrowse = new Button(bPathComposite, SWT.NONE);
				customBrowserBrowse.setFont(font);
				customBrowserBrowse.setText("CustomBrowserPreferencePage.Browse"); //$NON-NLS-1$
				GridData data = new GridData();
				data.horizontalAlignment = GridData.FILL;
				customBrowserBrowse.setLayoutData(data);
				customBrowserBrowse.addSelectionListener(new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent event) {
					}
					public void widgetSelected(SelectionEvent event) {
						FileDialog d = new FileDialog(getShell());
						d.setText("CustomBrowserPreferencePage.Details"); //$NON-NLS-1$
						String file = d.open();
						if (file != null) {
							customBrowserPath.setText("\""+file+"\" %1");
						}
					}
				});
				setEnabledCustomBrowserPath();
			}
}