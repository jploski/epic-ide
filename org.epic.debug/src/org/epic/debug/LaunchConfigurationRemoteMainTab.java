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

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;

public class LaunchConfigurationRemoteMainTab extends AbstractLaunchConfigurationTab
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
	protected Label fProjLabel;
	protected Label fHostLabel;
	protected Label fPortLabel;
	protected Combo fProjText;
	protected Button fProjButton;
	protected Text fHostText;
	protected Text fPortText;

	// Main class UI widgets
	protected Label fMainLabel;
	protected Combo fFileText;
	//	protected Button fSearchButton;
	//  protected Button fSearchExternalJarsCheckButton;
	//	protected Button fStopInMainCheckButton;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String PERL_NATURE_ID =
		"org.epic.perleditor.perlnature";
	private Label fDestLabel;
	private Text fDestText;

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

		Composite projComp = new Composite(comp, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 2;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);
		projComp.setFont(font);

		fProjLabel = new Label(projComp, SWT.NONE);
		fProjLabel.setText("&Project:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fProjLabel.setLayoutData(gd);
		fProjLabel.setFont(font);

		//fParamText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		fProjText =
			new Combo(projComp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.setFont(font);
		fProjText.setItems(getPerlProjects());
		fProjText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent evt)
			{
				updateLaunchConfigurationDialog();
				fFileText.setItems(getPerlFiles());
			}
		});

	

		createVerticalSpacer(comp, 1);

		Composite mainComp = new Composite(comp, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		mainComp.setLayoutData(gd);
		mainComp.setFont(font);

		fMainLabel = new Label(mainComp, SWT.NONE);
		fMainLabel.setText("File to execute"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fMainLabel.setLayoutData(gd);
		fMainLabel.setFont(font);

		//fMainText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		fFileText =
			new Combo(mainComp, SWT.READ_ONLY | SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fFileText.setLayoutData(gd);
		fFileText.setFont(font);
		fFileText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent evt)
			{
				updateLaunchConfigurationDialog();
			}
		});

		createVerticalSpacer(comp, 2);
		Composite hostComp = new Composite(comp, SWT.NONE);
		GridLayout hostLayout = new GridLayout();
		hostLayout.numColumns = 2;
		hostLayout.marginHeight = 0;
		hostLayout.marginWidth = 0;
		hostComp.setLayout(hostLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		hostComp.setLayoutData(gd);
		hostComp.setFont(font);

		fHostLabel = new Label(hostComp, SWT.NONE);
		fHostLabel.setText("Host:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fHostLabel.setLayoutData(gd);
		fHostLabel.setFont(font);

		fHostText = new Text(hostComp, SWT.SINGLE | SWT.BORDER);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		fHostText.setLayoutData(gd);
		fHostText.setFont(font);
		fHostText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent evt)
			{
				updateLaunchConfigurationDialog();
			}
		});

		createVerticalSpacer(comp, 2);
		Composite destComp = new Composite(comp, SWT.NONE);
		GridLayout destLayout = new GridLayout();
		destLayout.numColumns = 2;
		destLayout.marginHeight = 0;
		destLayout.marginWidth = 0;
		destComp.setLayout(destLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		destComp.setLayoutData(gd);
		destComp.setFont(font);

		fDestLabel = new Label(destComp, SWT.NONE);
		fDestLabel.setText("Dest:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fDestLabel.setLayoutData(gd);
		fDestLabel.setFont(font);

		fDestText = new Text(destComp, SWT.SINGLE | SWT.BORDER);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		fDestText.setLayoutData(gd);
		fDestText.setFont(font);
		fDestText.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent evt)
		{
			updateLaunchConfigurationDialog();
		}
	});
	
		createVerticalSpacer(comp, 2);
		Composite portComp = new Composite(comp, SWT.NONE);
		GridLayout portLayout = new GridLayout();
		portLayout.numColumns = 2;
		portLayout.marginHeight = 0;
		portLayout.marginWidth = 0;
		portComp.setLayout(portLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		portComp.setLayoutData(gd);
		portComp.setFont(font);

		fPortLabel = new Label(portComp, SWT.NONE);
		fPortLabel.setText("Port:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fPortLabel.setLayoutData(gd);
		fPortLabel.setFont(font);

		fPortText = new Text(portComp, SWT.SINGLE | SWT.BORDER);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		fPortText.setLayoutData(gd);
		fPortText.setFont(font);
		fPortText.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent evt)
		{
			updateLaunchConfigurationDialog();
		}
	});

	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config)
	{
		updateProjectFromConfig(config);
		updateFileFromConfig(config);
		updateParamsFromConfig(config);
		
		
		String val = ""; //$NON-NLS-1$
		try
		{
			val =
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT,
					EMPTY_STRING);
		} catch (CoreException ce)
		{
			PerlDebugPlugin.log(ce);
		}
		fPortText.setText(val);
		
	 val = ""; //$NON-NLS-1$
		try
		{
			val =
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST,
					EMPTY_STRING);
		} catch (CoreException ce)
		{
			PerlDebugPlugin.log(ce);
		}
		fHostText.setText(val);
		
		 val = ""; //$NON-NLS-1$
			try
			{
				val =
					config.getAttribute(
						PerlLaunchConfigurationConstants.ATTR_REMOTE_DEST,
						EMPTY_STRING);
			} catch (CoreException ce)
			{
				PerlDebugPlugin.log(ce);
			}
			fDestText.setText(val);
	}

	protected void updateProjectFromConfig(ILaunchConfiguration config)
	{
		String projectName = ""; //$NON-NLS-1$
		try
		{
			projectName =
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					EMPTY_STRING);
		} catch (CoreException ce)
		{
			PerlDebugPlugin.log(ce);
		}
		fProjText.setText(projectName);
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
		fHostText.setText(params);
	}

	protected void updateFileFromConfig(ILaunchConfiguration config)
	{
		String fileName = ""; //$NON-NLS-1$
		try
		{
			fileName =
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
					EMPTY_STRING);
		} catch (CoreException ce)
		{
			PerlDebugPlugin.log(ce);
		}
		fFileText.setText(fileName);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
			(String) fProjText.getText());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
			(String) fFileText.getText());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST,
			(String) fHostText.getText());
		config.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE_DEST,
				(String) fDestText.getText());
		config.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT,
				(String) fPortText.getText());
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose()
	{
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

		setErrorMessage(null);
		setMessage(null);

		String name = fProjText.getText().trim();
		if (name.length() > 0)
		{
			if (!ResourcesPlugin
				.getWorkspace()
				.getRoot()
				.getProject(name)
				.exists())
			{
				setErrorMessage("Project does not exist"); //$NON-NLS-1$
				return false;
			}
		} else
		{
			setErrorMessage("Specify Project"); //$NON-NLS-1$
			return false;
		}

		name = fFileText.getText().trim();
		if (name.length() == 0)
		{
			setErrorMessage("Startup File is not specified"); //$NON-NLS-1$
			return false;
		}

		name = fPortText.getText().trim();
		if (name.length() == 0)
		{
			setErrorMessage("Port is not specified"); //$NON-NLS-1$
			return false;
		}
		name = fDestText.getText().trim();
		if (name.length() == 0)
		{
			setErrorMessage("Destination is not specified"); //$NON-NLS-1$
			return false;
		}
		
		name = fHostText.getText().trim();
		if (name.length() == 0)
		{
			setErrorMessage("Host is not specified"); //$NON-NLS-1$
			return false;
		}
		return true;
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
		String host=null;
		try {
			 host = InetAddress.getLocalHost().getHostAddress().toString();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		//	}
		//	initializeMainTypeAndName(javaElement, config);

		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
			"");
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
			"");
		config.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST,
				host);
		config.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT,
				"5000");
		config.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_REMOTE,
				"REMOTE");
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

	private String[] getPerlFiles()
	{
		String projectName = fProjText.getText();

		if (projectName == null || projectName.length() == 0)
		{
			return (new String[] {
			});
		}

		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(projectName);
		IResourceVisitor visitor = new PerlProjectVisitor();
		try
		{
			project.accept(visitor);
		} catch (CoreException e)
		{
			e.printStackTrace();
		}
		return ((PerlProjectVisitor) visitor).getList();
	}

	class PerlProjectVisitor implements IResourceVisitor
	{
		private static final String PERL_EDITOR_ID =
			"org.epic.perleditor.editors.PerlEditor";
		private static final String EMB_PERL_FILE_EXTENSION = "epl";

		private List fileList = new ArrayList();
		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
		public boolean visit(IResource resource) throws CoreException
		{
			IEditorDescriptor defaultEditorDescriptor =
				PerlDebugPlugin
					.getDefault()
					.getWorkbench()
					.getEditorRegistry()
					.getDefaultEditor(resource.getFullPath().toString());

			if (defaultEditorDescriptor == null)
			{
				return true;
			}

			if (defaultEditorDescriptor.getId().equals(PERL_EDITOR_ID)
				&& !resource.getFileExtension().equals(EMB_PERL_FILE_EXTENSION))
			{
				fileList.add(
					resource.getFullPath().removeFirstSegments(1).toString());
			}

			return true;
		}

		public String[] getList()
		{
			return (String[]) fileList.toArray(new String[fileList.size()]);
		}

	}
}