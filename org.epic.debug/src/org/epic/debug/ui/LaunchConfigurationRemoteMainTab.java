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

import java.io.File;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.epic.debug.*;
import org.epic.debug.util.RemotePort;

public class LaunchConfigurationRemoteMainTab
		extends
			AbstractLaunchConfigurationTab implements IPropertyChangeListener {

	/**
	 * A launch configuration tab that displays and edits project and main type
	 * name launch configuration attributes.
	 * <p>
	 * This class may be instantiated. This class is not intended to be
	 * subclassed.
	 * </p>
	 * 
	 * @since 2.0
	 */

	// Project UI widgets
	//protected Label fProjLabel;
	protected Label fHostLabel;
	protected Label fPortLabel;
	//protected Text fProjText;
	//protected Button fProjButton;
	protected Text fHostText;
	protected Text fPortText;

	// Main class UI widgets
	//protected Label fMainLabel;
	//protected Text fFileText;
	//	protected Button fSearchButton;
	//  protected Button fSearchExternalJarsCheckButton;
	//	protected Button fStopInMainCheckButton;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String PERL_NATURE_ID = "org.epic.perleditor.perlnature";
	private Label fDestLabel;
	private Text fDestText;
	private GridLayout layout;
	private FileFieldEditor mDebugPackageFilePath;
	private Object checkComp;
	private Label fCheckLabel;
	private Button fCheckBox;
	Composite mDebugPackageComp;
	
	private ProjectAndFileBlock fProjectAndFileBlock  = new ProjectAndFileBlock();;

	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();

		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);
		GridData gd;
//
//		createVerticalSpacer(comp, 1);
//
//		Composite projComp = new Composite(comp, SWT.NONE);
//		GridLayout projLayout = new GridLayout();
//		projLayout.numColumns = 3;
//		projLayout.marginHeight = 0;
//		projLayout.marginWidth = 0;
//		projLayout.makeColumnsEqualWidth = true;
//		projComp.setLayout(projLayout);
//		gd = new GridData(GridData.FILL_HORIZONTAL);
//		projComp.setLayoutData(gd);
//		projComp.setFont(font);
//
//		fProjLabel = new Label(projComp, SWT.NONE);
//		fProjLabel.setText("&Project:"); //$NON-NLS-1$
//		gd = new GridData();
//		gd.horizontalSpan = 1;
//		fProjLabel.setLayoutData(gd);
//		fProjLabel.setFont(font);
//
//		//fParamText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
//		fProjText = new Text(projComp, SWT.SINGLE | SWT.BORDER );
//		gd = new GridData(GridData.FILL_HORIZONTAL);
//		gd.horizontalSpan = 2;
//		fProjText.setLayoutData(gd);
//		fProjText.setFont(font);
//		fProjText.addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent evt) {
//				updateLaunchConfigurationDialog();
//			}
//		});
//
//		createVerticalSpacer(comp, 1);
//
//		Composite mainComp = new Composite(comp, SWT.NONE);
//		GridLayout mainLayout = new GridLayout();
//		mainLayout.numColumns = 3;
//		mainLayout.marginHeight = 0;
//		mainLayout.marginWidth = 0;
//		mainLayout.makeColumnsEqualWidth = true;
//		mainComp.setLayout(mainLayout);
//		gd = new GridData(GridData.FILL_HORIZONTAL);
//		mainComp.setLayoutData(gd);
//		mainComp.setFont(font);
//
//		fMainLabel = new Label(mainComp, SWT.NONE);
//		fMainLabel.setText("File to execute:"); //$NON-NLS-1$
//		gd = new GridData();
//		gd.horizontalSpan = 1;
//		fMainLabel.setLayoutData(gd);
//		fMainLabel.setFont(font);
//
//		//fMainText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
//		fFileText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
//		gd = new GridData(GridData.FILL_HORIZONTAL);
//		gd.horizontalSpan = 2;
//		fFileText.setLayoutData(gd);
//		fFileText.setFont(font);
//		fFileText.addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent evt) {
//				updateLaunchConfigurationDialog();
//			}
//		});

		fProjectAndFileBlock.createControl(comp);
		
		createVerticalSpacer(comp, 2);
		Composite hostComp = new Composite(comp, SWT.NONE);
		GridLayout hostLayout = new GridLayout();
		hostLayout.numColumns = 3;
		hostLayout.marginHeight = 0;
		hostLayout.marginWidth = 0;
		hostLayout.makeColumnsEqualWidth = true;
		hostComp.setLayout(hostLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		hostComp.setLayoutData(gd);
		hostComp.setFont(font);

		fHostLabel = new Label(hostComp, SWT.NONE);
		fHostLabel.setText("Local Host IP:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 1;
		fHostLabel.setLayoutData(gd);
		fHostLabel.setFont(font);

		fHostText = new Text(hostComp, SWT.SINGLE | SWT.BORDER);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fHostText.setLayoutData(gd);
		fHostText.setFont(font);
		fHostText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});

		createVerticalSpacer(comp, 1);
		Composite destComp = new Composite(comp, SWT.NONE);
		GridLayout destLayout = new GridLayout();
		destLayout.numColumns = 3;
		destLayout.marginHeight = 0;
		destLayout.marginWidth = 0;
		destLayout.makeColumnsEqualWidth = true;
		destComp.setLayout(destLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		destComp.setLayoutData(gd);
		destComp.setFont(font);

		fDestLabel = new Label(destComp, SWT.NONE);
		fDestLabel.setText("Target Host Project Installation Path:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 1;
		fDestLabel.setLayoutData(gd);
		fDestLabel.setFont(font);

		fDestText = new Text(destComp, SWT.SINGLE | SWT.BORDER);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fDestText.setLayoutData(gd);
		fDestText.setFont(font);
		fDestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});

		createVerticalSpacer(comp, 1);
		Composite portComp = new Composite(comp, SWT.NONE);
		GridLayout portLayout = new GridLayout();
		portLayout.numColumns = 3;
		portLayout.marginHeight = 0;
		portLayout.marginWidth = 0;
		portLayout.makeColumnsEqualWidth = true;
		portComp.setLayout(portLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		portComp.setLayoutData(gd);
		portComp.setFont(font);

		fPortLabel = new Label(portComp, SWT.NONE);
		fPortLabel.setText("Port:"); //$NON-NLS-1$
		gd = new GridData();
		gd.verticalAlignment = 1;
		fPortLabel.setLayoutData(gd);
		fPortLabel.setFont(font);

		fPortText = new Text(portComp, SWT.SINGLE | SWT.BORDER);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fPortText.setLayoutData(gd);
		fPortText.setFont(font);
		fPortText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		//********************************
		createVerticalSpacer(comp, 1);
		Composite checkComp = new Composite(comp, SWT.NONE);
		GridLayout checkLayout = new GridLayout();
		checkLayout.numColumns = 3;
		checkLayout.marginHeight = 0;
		checkLayout.marginWidth = 0;
		checkLayout.makeColumnsEqualWidth = true;
		checkComp.setLayout(portLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		checkComp.setLayoutData(gd);
		checkComp.setFont(font);

		fCheckLabel = new Label(checkComp, SWT.NONE);
		fCheckLabel.setText("Create Debug Package"); //$NON-NLS-1$
		gd = new GridData();
		gd.verticalAlignment = 1;
		fCheckLabel.setLayoutData(gd);
		fCheckLabel.setFont(font);

		//fPortText = new Text(portComp, SWT.SINGLE | SWT.BORDER);
		fCheckBox = new Button(checkComp, SWT.CHECK | SWT.CENTER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		fCheckBox.setLayoutData(gd);
		fCheckBox.setFont(font);

		fCheckBox.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				calculatePackageFilePathEnabled();
				updateLaunchConfigurationDialog();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		//********************************

		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.makeColumnsEqualWidth = true;
		gd = new GridData(GridData.FILL_HORIZONTAL);

		mDebugPackageComp = new Composite(comp, SWT.NONE);
		mDebugPackageComp.setLayout(layout);
		mDebugPackageComp.setLayoutData(gd);
		mDebugPackageComp.setFont(font);

		mDebugPackageFilePath = new FileFieldEditor("Test",
				"Debug Package File Path", mDebugPackageComp);

		String[] ext = {".zip"};
		mDebugPackageFilePath.setFileExtensions(ext);
		//fHTMLRootDir.fillIntoGrid(htmlRootDirComp, 3);
		mDebugPackageFilePath.setPropertyChangeListener(this);
		calculatePackageFilePathEnabled();
	}

	void calculatePackageFilePathEnabled() {
		mDebugPackageFilePath.setEnabled(fCheckBox.getSelection(),
				mDebugPackageComp);
	}
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		
		fProjectAndFileBlock.initializeFrom(config);
		updateParamsFromConfig(config);
		updateDebugPackageFromConfig(config);

		String val = ""; //$NON-NLS-1$
		try {
			val = config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT,
					EMPTY_STRING);
		} catch (CoreException ce) {
			PerlDebugPlugin.log(ce);
		}
		fPortText.setText(val);

		val = ""; //$NON-NLS-1$
		try {
			val = config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST,
					EMPTY_STRING);
		} catch (CoreException ce) {
			PerlDebugPlugin.log(ce);
		}
		fHostText.setText(val);

		val = ""; //$NON-NLS-1$
		try {
			val = config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_REMOTE_DEST,
					EMPTY_STRING);
		} catch (CoreException ce) {
			PerlDebugPlugin.log(ce);
		}
		fDestText.setText(val);
		calculatePackageFilePathEnabled();
	}

//	protected void updateProjectFromConfig(ILaunchConfiguration config) {
//		String projectName = ""; //$NON-NLS-1$
//		try {
//			projectName = config.getAttribute(
//					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
//					EMPTY_STRING);
//		} catch (CoreException ce) {
//			PerlDebugPlugin.log(ce);
//		}
//		fProjText.setText(projectName);
//	}

	protected void updateDebugPackageFromConfig(ILaunchConfiguration config) {
		String path = ""; //$NON-NLS-1$
		boolean create = true;

		try {
			path = config
					.getAttribute(
							PerlLaunchConfigurationConstants.ATTR_REMOTE_DEBUG_PACKAGE_PATH,
							EMPTY_STRING);
			create = config
					.getAttribute(
							PerlLaunchConfigurationConstants.ATTR_REMOTE_CREATE_DEBUG_PACKAGE,
							true);
		} catch (CoreException ce) {
			PerlDebugPlugin.log(ce);
		}

		mDebugPackageFilePath.setStringValue(path);
		this.fCheckBox.setSelection(create);
	}

	protected void updateParamsFromConfig(ILaunchConfiguration config) {
		String params = ""; //$NON-NLS-1$
		try {
			params = config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
					EMPTY_STRING);
		} catch (CoreException ce) {
			PerlDebugPlugin.log(ce);
		}
		fHostText.setText(params);
	}

//	protected void updateFileFromConfig(ILaunchConfiguration config) {
//		String fileName = ""; //$NON-NLS-1$
//		try {
//			fileName = config.getAttribute(
//					PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
//					EMPTY_STRING);
//		} catch (CoreException ce) {
//			PerlDebugPlugin.log(ce);
//		}
//		fFileText.setText(fileName);
//	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fProjectAndFileBlock.performApply(config);
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST,
				(String) fHostText.getText());
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_DEST,
				(String) fDestText.getText());
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT,
				(String) fPortText.getText());
		config
				.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_REMOTE_DEBUG_PACKAGE_PATH,
						(String) mDebugPackageFilePath.getStringValue());
		config
				.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_REMOTE_CREATE_DEBUG_PACKAGE,
						(boolean) fCheckBox.getSelection());
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
	}

	/**
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {

		setErrorMessage(null);
		setMessage(null);

		if(!fProjectAndFileBlock.isValid(config)){
			return false;
		}
		String name = fPortText.getText().trim();
		int port = -1;

		try {
			port = Integer.parseInt(name);
		} catch (Exception e) {
		}

		if (name.length() == 0 || port < 0) {
			setErrorMessage("Port is not valid"); //$NON-NLS-1$
			return false;
		}
		name = fDestText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage("Target Host Project Installation Path is not specified"); //$NON-NLS-1$
			return false;
		}

		name = fHostText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage("Local Host is not specified"); //$NON-NLS-1$
			return false;
		}

		if (this.fCheckBox.getSelection()) {

			name = mDebugPackageFilePath.getStringValue();

			if (name == null)
				setErrorMessage("Debug Package File Path missing");
			else {
				Path path = new Path(name);
				File file = path.removeLastSegments(1).toFile();

				if (!path.isValidPath(name) || path.segmentCount() == 0
						|| !file.exists())
					setErrorMessage("Debug Package File Path invalid");
			}

		}
		return true;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {

		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostAddress().toString();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		fProjectAndFileBlock.setDefaults(config);
		config.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS, "");
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_HOST,
				host);
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE_PORT,
				Integer.toString(RemotePort.findFreePort()));
		config
				.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_REMOTE_CREATE_DEBUG_PACKAGE,
						true);
		config
				.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_REMOTE_DEBUG_PACKAGE_PATH,
						"");
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_REMOTE,
				"REMOTE");
	}

	/**
	 * Set the main type & name attributes on the working copy based on the
	 * IJavaElement
	 */
	/*
	 * protected void initializeMainTypeAndName(IJavaElement javaElement,
	 * ILaunchConfigurationWorkingCopy config) { String name= null; if
	 * (javaElement instanceof IMember) { IMember member = (IMember)javaElement;
	 * if (member.isBinary()) { javaElement = member.getClassFile(); } else {
	 * javaElement = member.getCompilationUnit(); } } if (javaElement instanceof
	 * ICompilationUnit || javaElement instanceof IClassFile) { try { IType[]
	 * types = MainMethodFinder.findTargets(new BusyIndicatorRunnableContext(),
	 * new Object[] {javaElement}); if (types != null && (types.length > 0)) { //
	 * Simply grab the first main type found in the searched element name =
	 * types[0].getFullyQualifiedName(); } } catch (InterruptedException ie) { }
	 * catch (InvocationTargetException ite) { } } if (name == null) { name= "";
	 * //$NON-NLS-1$ }
	 * config.setAttribute(PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
	 * name); if (name.length() > 0) { int index = name.lastIndexOf('.'); if
	 * (index > 0) { name = name.substring(index + 1); } name =
	 * getLaunchConfigurationDialog().generateName(name); config.rename(name); } }
	 */
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Configuration"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return (PerlDebugPlugin.getDefaultDesciptorImageRegistry()
				.get(PerlDebugImages.DESC_OBJS_LaunchTabRemote));
	}

	/**
	 * Returns a String array whith all Perl projects
	 * 
	 * @return Stiring[] List of Perl projects
	 */
	private String[] getPerlProjects() {
		List projectList = new ArrayList();
		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			try {
				if (project.isAccessible() && project.hasNature(PERL_NATURE_ID)) {
					//System.out.println("Perl Project: " + project.getName());
					projectList.add(project.getName());
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}

		}

		return (String[]) projectList.toArray(new String[projectList.size()]);
	}

//	private String[] getPerlFiles() {
//		String projectName = fProjText.getText();
//
//		if (projectName == null || projectName.length() == 0) {
//			return (new String[]{});
//		}
//
//		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
//		IProject project = workspaceRoot.getProject(projectName);
//		IResourceVisitor visitor = new PerlProjectVisitor();
//		try {
//			project.accept(visitor);
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//		return ((PerlProjectVisitor) visitor).getList();
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String path = this.mDebugPackageFilePath.getStringValue();
		if (path.length() >= 1 && !path.endsWith(".zip"))
				mDebugPackageFilePath.setStringValue(path + ".zip");
			
			updateLaunchConfigurationDialog();
		
			
	}

	class PerlProjectVisitor implements IResourceVisitor

	{
		private static final String PERL_EDITOR_ID = "org.epic.perleditor.editors.PerlEditor";
		private static final String EMB_PERL_FILE_EXTENSION = "epl";

		private List fileList = new ArrayList();
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
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
}