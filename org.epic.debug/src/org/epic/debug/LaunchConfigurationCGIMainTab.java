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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.epic.debug.LaunchConfigurationMainTab.PerlProjectVisitor;

public class LaunchConfigurationCGIMainTab
	extends AbstractLaunchConfigurationTab
	implements IPropertyChangeListener
{

	/**
	 * A launch configuration tab that displays and edits project and
	 * main type name launch configuration attributes.
	 * <p>
	 * This class may be instantiated. This class is not intended to be subclassed.
	 * </p>
	 * @since 2.0
	 */

	
	//	protected Label fProjLabel;
	//protected Label fParamLabel;
	//	protected Combo fProjText;
	//	protected Button fProjButton;
	//	protected Text fParamText;

	// Main class UI widgets

	//	protected Button fSearchButton;
	//  protected Button fSearchExternalJarsCheckButton;
	//	protected Button fStopInMainCheckButton;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String PERL_NATURE_ID =
		"org.epic.perleditor.perlnature";
	private Label fProjLabel;
	private Combo fProjText;

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent)
	{
		Font font = parent.getFont();
		GridLayout layout;
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		topLayout.marginHeight = 0;
		topLayout.marginWidth = 0;
		comp.setLayout(topLayout);
		GridData gd;

		createVerticalSpacer(comp, 2);

		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		gd = new GridData(GridData.FILL_HORIZONTAL);

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

		
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config)
	{
		updateParamsFromConfig(config);

	}

	protected void updateParamsFromConfig(ILaunchConfiguration config)
	{
		try
		{
			fProjText.setText(
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
					(String) null));

		
			
			} catch (CoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//	/**
	//	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	//	 */
	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
			fProjText.getText());
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
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config)
	{
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$

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

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event)
	{
//		if (event.getSource() == fHTMLRootDir)
//		{
//			if (fHTMLRootFile
//				.getStringValue()
//				.indexOf(fHTMLRootDir.getStringValue())
//				!= 0)
//				fHTMLRootFile.setStringValue(fHTMLRootDir.getStringValue());
//		}
//
//		updateLaunchConfigurationDialog();

	}

	private String[] getDirs()
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
		return ((PerlProjectVisitor) visitor).getDirList();
	}

	class PerlProjectVisitor implements IResourceVisitor
	{
		private static final String PERL_EDITOR_ID =
			"org.epic.perleditor.editors.PerlEditor";
		private static final String EMB_PERL_FILE_EXTENSION = "epl";

		private List mDirList = new ArrayList();
		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
		public boolean visit(IResource resource) throws CoreException
		{
			if(resource instanceof IFolder)

			
			{
				mDirList.add(
					resource.getFullPath().removeFirstSegments(1).toString());
			}

			return true;
		}

		public String[] getDirList()
		{
			return (String[]) mDirList.toArray(new String[mDirList.size()]);
		}

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

}
