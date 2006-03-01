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

import gnu.regexp.RE;
import gnu.regexp.REException;

import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.epic.debug.util.ListEditor;

public class LaunchConfigurationCGIEnvTab
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

	private ListEditor fEnvVar;
	//	private IntegerFieldEditor fWebserverPort;

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

	private RE mTestEnvVar;

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String PERL_NATURE_ID =
		"org.epic.perleditor.perlnature";

	public LaunchConfigurationCGIEnvTab()
	{
		super();
		try
		{
			mTestEnvVar = new RE("^\\s*[^\\s]+\\s*=.*");
		} catch (REException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
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

		Composite envComp = new Composite(comp, SWT.NONE);
		envComp.setLayout(layout);
		envComp.setLayoutData(gd);
		envComp.setFont(font);

		fEnvVar = new ListEditor("Environment Variables", envComp, this);
		fEnvVar.fillIntoGrid(envComp, 3);
		fEnvVar.setPropertyChangeListener(this);
		createVerticalSpacer(comp, 1);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		gd = new GridData(GridData.FILL_HORIZONTAL);

		
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
		
			fEnvVar.initilizeFrom(config);

		
	}

	//	/**
	//	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	//	 */
	public void performApply(ILaunchConfigurationWorkingCopy config)
	{
	
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_DEBUG_CGI,
			"OK");

		fEnvVar.doApply(config);

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

	
		String[] items = this.fEnvVar.getItems();
		for (int i = 0; i < items.length; i++)
		{
			if( !mTestEnvVar.isMatch(items[i]))
			{
				setErrorMessage("Invalid Environment Variable Entry at Line "+(i+1) );
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
		config.setAttribute(
			PerlLaunchConfigurationConstants.ATTR_CGI_ENV,
			(Map) null);

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
		return "CGI Environment"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage()
	{
		return (
			PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				PerlDebugImages.DESC_OBJS_LaunchTabCGI));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event)
	{
		
		updateLaunchConfigurationDialog();

	}

	public void update()
	{
		updateLaunchConfigurationDialog();
	}

}