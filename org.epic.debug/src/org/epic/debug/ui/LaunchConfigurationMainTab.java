
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

package org.epic.debug.ui;

//import java.lang.reflect.InvocationTargetException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;
import org.epic.debug.ProjectAndFileBlock;
import org.epic.debug.WorkingDirectoryBlock;

public class LaunchConfigurationMainTab extends AbstractLaunchConfigurationTab implements ActionListener
{

	/**
	 * A launch configuration tab that displays and edits project and
	 * main type name launch configuration attributes.
	 * <p>
	 * This class may be instantiated. This class is not intended to be subclassed.
	 * </p>
	 * @since 2.0
	 */

	// Project UI widgets
	protected Label fParamLabel;
	protected Text fParamText;

	// Main class UI widgets
    protected WorkingDirectoryBlock fWorkingDirectoryBlock;
    protected ProjectAndFileBlock  fFileToExecuteBlock;
	//	protected Button fSearchButton;
	//  protected Button fSearchExternalJarsCheckButton;
	//	protected Button fStopInMainCheckButton;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$


    public LaunchConfigurationMainTab()
    {
        fWorkingDirectoryBlock = new WorkingDirectoryBlock();
        fFileToExecuteBlock   = new ProjectAndFileBlock();
    }

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent)
	{
		Font font = parent.getFont();

		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);
		GridData gd;

		createVerticalSpacer(comp, 1);
		
		fFileToExecuteBlock.createControl(comp);
		
		createVerticalSpacer(comp, 2);
		Composite paramComp = new Composite(comp, SWT.NONE);
		GridLayout paramLayout = new GridLayout();
		paramLayout.numColumns = 2;
		paramLayout.marginHeight = 0;
		paramLayout.marginWidth = 0;
		paramComp.setLayout(paramLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		paramComp.setLayoutData(gd);
		paramComp.setFont(font);

		fParamLabel = new Label(paramComp, SWT.NONE);
		fParamLabel.setText("Command-line parameters:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fParamLabel.setLayoutData(gd);
		fParamLabel.setFont(font);

		fParamText = new Text(paramComp, SWT.SINGLE | SWT.BORDER);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		fParamText.setLayoutData(gd);
		fParamText.setFont(font);
		fParamText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent evt)
			{
				updateLaunchConfigurationDialog();
			}
		});

        createVerticalSpacer(comp, 2);
        fWorkingDirectoryBlock.createControl(comp); 
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config)
	{
		
		fFileToExecuteBlock.initializeFrom(config);
		//updateMainTypeFromConfig(config);
	//	String file = fMainText.getText();
	//	fMainText.setItems(getPerlFiles());
		//fMainText.setText(file);
		updateParamsFromConfig(config);
        fWorkingDirectoryBlock.initializeFrom(config);
	}


	protected void updateParamsFromConfig(ILaunchConfiguration config)
	{
		String params = ""; //$NON-NLS-1$
		try
		{
			params =
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
					EMPTY_STRING);
		} catch (CoreException ce)
		{
			PerlDebugPlugin.log(ce);
		}
		fParamText.setText(params);
	}


	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
		fFileToExecuteBlock.performApply(config);

		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
			(String) fParamText.getText());
        
        fWorkingDirectoryBlock.performApply(config);
	}
    
    public String getErrorMessage()
    {
        String m = super.getErrorMessage();
        m = (m == null) ? fFileToExecuteBlock.getErrorMessage() : m;
        return m == null ? fWorkingDirectoryBlock.getErrorMessage() : m;
    }
    
    public String getMessage()
    {
        String m = super.getMessage();
        m = m == null ? fWorkingDirectoryBlock.getMessage() : m;
        return m == null ? fFileToExecuteBlock.getMessage() : m;
    }
    
    public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog)
    {
        super.setLaunchConfigurationDialog(dialog);
        fWorkingDirectoryBlock.setLaunchConfigurationDialog(dialog);
        fFileToExecuteBlock.setLaunchConfigurationDialog(dialog);
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
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config)
	{

		setErrorMessage(null);
		setMessage(null);


		if (!fFileToExecuteBlock.isValid(config)){
			return false;
		}
		
		return fWorkingDirectoryBlock.isValid(config);
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
		//	}
		//	initializeMainTypeAndName(javaElement, config);

		fFileToExecuteBlock.setDefaults(config);
		
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
			"");

        fWorkingDirectoryBlock.setDefaults(config);
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
		return "Configuration"; //$NON-NLS-1$
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


	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}