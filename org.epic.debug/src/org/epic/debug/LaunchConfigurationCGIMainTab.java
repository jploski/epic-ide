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
//import org.eclipse.swt.events.SelectionAdapter;
//import org.eclipse.swt.events.SelectionEvent;
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
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.swt.widgets.Text;
//import org.eclipse.ui.dialogs.ElementListSelectionDialog;
//import org.eclipse.ui.dialogs.SelectionDialog;
//import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.core.resources.IProject;

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

	private IntegerFieldEditor fDebugPort;
	private DirectoryFieldEditor fCGIRootDir;
	private DirectoryFieldEditor fHTMLRootDir;
	private FileFieldEditor fHTMLRootFile;
	// Project UI widgets
	//	protected Label fProjLabel;
	//protected Label fParamLabel;
	//	protected Combo fProjText;
	//	protected Button fProjButton;
	//	protected Text fParamText;

	// Main class UI widgets
	protected Label fMainLabel;
	protected Combo fMainText;
	//	protected Button fSearchButton;
	//  protected Button fSearchExternalJarsCheckButton;
	//	protected Button fStopInMainCheckButton;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String PERL_NATURE_ID =
		"org.epic.perleditor.perlnature";

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

		Composite htmlRootDirComp = new Composite(comp, SWT.NONE);
		htmlRootDirComp.setLayout(layout);
		htmlRootDirComp.setLayoutData(gd);
		htmlRootDirComp.setFont(font);

		fHTMLRootDir =
			new DirectoryFieldEditor(
				"Test",
				"HTML Root Directory",
				htmlRootDirComp);
		fHTMLRootDir.fillIntoGrid(htmlRootDirComp, 3);
		fHTMLRootDir.setPropertyChangeListener(this);

		createVerticalSpacer(comp, 1);

		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		gd = new GridData(GridData.FILL_HORIZONTAL);

		Composite htmlRootFileComp = new Composite(comp, SWT.NONE);
		htmlRootFileComp.setLayout(layout);
		htmlRootFileComp.setLayoutData(gd);
		htmlRootFileComp.setFont(font);

		fHTMLRootFile =
			new FileFieldEditor(
				"Test",
				"HTML Root File",
				true,
				htmlRootFileComp);
		fHTMLRootFile.fillIntoGrid(htmlRootFileComp, 3);
		fHTMLRootFile.setPropertyChangeListener(this);

		createVerticalSpacer(comp, 1);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		gd = new GridData(GridData.FILL_HORIZONTAL);

		Composite cgiRootDirComp = new Composite(comp, SWT.NONE);
		cgiRootDirComp.setLayout(layout);
		cgiRootDirComp.setLayoutData(gd);
		cgiRootDirComp.setFont(font);

		fCGIRootDir =
			new DirectoryFieldEditor(
				"Test",
				"CGI Root Directory",
				cgiRootDirComp);
		fCGIRootDir.fillIntoGrid(cgiRootDirComp, 3);

		fCGIRootDir.setPropertyChangeListener(this);

		createVerticalSpacer(comp, 1);

		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		gd = new GridData(GridData.FILL_HORIZONTAL);

		Composite debugPortComp = new Composite(comp, SWT.NONE);
		debugPortComp.setLayout(layout);
		debugPortComp.setLayoutData(gd);
		debugPortComp.setFont(font);

		fDebugPort =
			new IntegerFieldEditor("Test", "Debugger Port", debugPortComp);
		fDebugPort.fillIntoGrid(debugPortComp, 2);
		fDebugPort.setValidRange(1,Integer.MAX_VALUE);

		fDebugPort.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
		fDebugPort.setPropertyChangeListener(this);
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
			fHTMLRootDir.setStringValue(
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
					(String) null));

			fHTMLRootFile.setStringValue(
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE,
					(String) null));
			fCGIRootDir.setStringValue(
				config.getAttribute(
					PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
					(String) null));

			fDebugPort.setStringValue(
					config.getAttribute(
						PerlLaunchConfigurationConstants.ATTR_DEBUG_PORT,
						(String)null));
			
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
			PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
			this.fHTMLRootDir.getStringValue());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE,
			this.fHTMLRootFile.getStringValue());
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
			this.fCGIRootDir.getStringValue());
		config.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_DEBUG_PORT,
						fDebugPort.getStringValue());
		config.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
							"OK");

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

		String value = fHTMLRootDir.getStringValue();

		if (value == null)
		{
			setErrorMessage("HTML Root Directory is missing"); //$NON-NLS-1$
			return false;
		}

		File file = new File(value);
		if (!file.exists() || !file.isDirectory())
		{
			setErrorMessage("HTML Root Directory is invalid"); //$NON-NLS-1$
			return false;
		}

		value = fHTMLRootFile.getStringValue();

		if (value == null)
		{
			setErrorMessage("HTML Startup File is missing"); //$NON-NLS-1$
			return false;
		}

		file = new File(value);
		if (!file.exists() || !file.isFile())
		{
			setErrorMessage("HTML Startup File is invalid"); //$NON-NLS-1$
			return false;
		}

		if (value.indexOf(fHTMLRootDir.getStringValue()) != 0)
		{
			setErrorMessage("HTML Startup File must be located within HTMT Root Directory (or one of its subfolders)"); //$NON-NLS-1$
			return false;
		}

		value = fCGIRootDir.getStringValue();
		if (value == null)
		{
			setErrorMessage("CGI Root Directory is missing"); //$NON-NLS-1$
			return false;
		}

		file = new File(value);
		if (!file.exists() || !file.isDirectory())
		{
			setErrorMessage("CGI Root Directory is invalid"); //$NON-NLS-1$
			return false;
		}

		value = fDebugPort.getStringValue();
				if (value == null )
				{
					setErrorMessage("Debug Port is missing"); //$NON-NLS-1$
					return false;
				}
				else {
					try{
				
					 Integer.parseInt(value);
					}
					 catch(Exception e)
					 {
						setErrorMessage("Debug Port is invalid"); //$NON-NLS-1$
											return false;
					 }
				}

		return true;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config)
	{
		String root =
			ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR, root); //$NON-NLS-1$
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR, root); //$NON-NLS-1$
		config.setAttribute(PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE, root); //$NON-NLS-1$
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_DEBUG_PORT,
			PerlDebugPlugin.getDefaultDebugPort());
		config.setAttribute(
		PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
					"OK");

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
		if (event.getSource() == fHTMLRootDir)
		{
			if (fHTMLRootFile
				.getStringValue()
				.indexOf(fHTMLRootDir.getStringValue())
				!= 0)
				fHTMLRootFile.setStringValue(fHTMLRootDir.getStringValue());
		}

		updateLaunchConfigurationDialog();

	}

}