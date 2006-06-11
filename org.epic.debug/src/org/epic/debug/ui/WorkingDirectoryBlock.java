/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     adapted for EPIC by jploski
 *******************************************************************************/
package org.epic.debug.ui;

import java.io.File;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;

/**
 * A control for setting the working directory associated with a launch
 * configuration.
 */
public class WorkingDirectoryBlock extends AbstractLaunchConfigurationTab
{
    private Text fWorkingDirText;
    private Button fWorkspaceButton;
    private Button fFileSystemButton;
    private Button fVariablesButton;
    private Button fUseDefaultWorkingDirButton;

    /**
     * The last launch config this tab was initialized from
     */
    protected ILaunchConfiguration fLaunchConfiguration;

    /**
     * A listener to update for text changes and widget selection
     */
    private class WidgetListener extends SelectionAdapter implements
        ModifyListener
    {
        public void modifyText(ModifyEvent e)
        {
            updateLaunchConfigurationDialog();
        }

        public void widgetSelected(SelectionEvent e)
        {
            Object source = e.getSource();
            if (source == fWorkspaceButton)
            {
                handleWorkspaceDirBrowseButtonSelected();
            }
            else if (source == fFileSystemButton)
            {
                handleWorkingDirBrowseButtonSelected();
            }
            else if (source == fUseDefaultWorkingDirButton)
            {
                handleUseDefaultWorkingDirButtonSelected();
            }
            else if (source == fVariablesButton)
            {
                handleWorkingDirVariablesButtonSelected();
            }
        }
    }

    private WidgetListener fListener = new WidgetListener();

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent)
    {
        Font font = parent.getFont();

        //Composite group = new Composite(parent, SWT.NONE);
        Group group = new Group(parent, SWT.NONE);
        
        GridLayout workingDirLayout = new GridLayout();
        workingDirLayout.numColumns = 2;
        workingDirLayout.makeColumnsEqualWidth = false;
        //workingDirLayout.marginHeight = 0;
        //workingDirLayout.marginWidth = 0;
        group.setLayout(workingDirLayout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        group.setFont(font);
        group.setText("Working directory:");
        setControl(group);

        fWorkingDirText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        fWorkingDirText.setLayoutData(gd);
        fWorkingDirText.setFont(font);
        fWorkingDirText.addModifyListener(fListener);

        fUseDefaultWorkingDirButton = new Button(group, SWT.CHECK);
        fUseDefaultWorkingDirButton
            .setText("Use default wor&king directory"); //$NON-NLS-1$
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fUseDefaultWorkingDirButton.setLayoutData(gd);
        fUseDefaultWorkingDirButton.setFont(font);
        fUseDefaultWorkingDirButton.addSelectionListener(fListener);

        Composite buttonComp = new Composite(group, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonComp.setLayout(layout);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        buttonComp.setLayoutData(gd);
        buttonComp.setFont(font);
        fWorkspaceButton = createPushButton(buttonComp,
            "W&orkspace...", null); //$NON-NLS-1$
        fWorkspaceButton.addSelectionListener(fListener);

        fFileSystemButton = createPushButton(buttonComp,
            "File S&ystem...", null); //$NON-NLS-1$
        fFileSystemButton.addSelectionListener(fListener);

        fVariablesButton = createPushButton(buttonComp,
            "Variabl&es...", null); //$NON-NLS-1$
        fVariablesButton.addSelectionListener(fListener);
    }

    public void dispose()
    {
    }

    /**
     * Show a dialog that lets the user select a working directory
     */
    protected void handleWorkingDirBrowseButtonSelected()
    {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setMessage("Select a working directory for the launch configuration:"); //$NON-NLS-1$
        String currentWorkingDir = fWorkingDirText.getText();
        if (!currentWorkingDir.trim().equals("")) { //$NON-NLS-1$
            File path = new File(currentWorkingDir);
            if (path.exists())
            {
                dialog.setFilterPath(currentWorkingDir);
            }
        }

        String selectedDirectory = dialog.open();
        if (selectedDirectory != null)
        {
            fWorkingDirText.setText(selectedDirectory);
        }
    }

    /**
     * Show a dialog that lets the user select a working directory from the
     * workspace
     */
    protected void handleWorkspaceDirBrowseButtonSelected()
    {
        IContainer currentContainer = getContainer();
        if (currentContainer == null)
        {
            currentContainer = ResourcesPlugin.getWorkspace().getRoot();
        }
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(
            getShell(), currentContainer, false,
            "Select a &workspace relative working directory:"); //$NON-NLS-1$
        dialog.showClosedProjects(false);
        dialog.open();
        Object[] results = dialog.getResult();
        if ((results != null) && (results.length > 0)
            && (results[0] instanceof IPath))
        {
            IPath path = (IPath) results[0];
            String containerName = path.makeRelative().toString();
            fWorkingDirText.setText("${workspace_loc:" + containerName + "}"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Returns the selected workspace container,or <code>null</code>
     */
    protected IContainer getContainer()
    {
        String path = fWorkingDirText.getText().trim();
        if (path.length() > 0)
        {
            IResource res = null;
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            if (path.startsWith("${workspace_loc:")) { //$NON-NLS-1$
                IStringVariableManager manager = VariablesPlugin.getDefault()
                    .getStringVariableManager();
                try
                {
                    path = manager.performStringSubstitution(path, false);
                    IContainer[] containers = root
                        .findContainersForLocation(new Path(path));
                    if (containers.length > 0)
                    {
                        res = containers[0];
                    }
                }
                catch (CoreException e)
                {
                }
            }
            else
            {
                res = root.findMember(path);
            }
            if (res instanceof IContainer)
            {
                return (IContainer) res;
            }
        }
        return null;
    }

    /**
     * The default working dir check box has been toggled.
     */
    protected void handleUseDefaultWorkingDirButtonSelected()
    {
        boolean def = isDefaultWorkingDirectory();
        if (def)
        {
            setDefaultWorkingDir();
        }
        fWorkingDirText.setEnabled(!def);
        fWorkspaceButton.setEnabled(!def);
        fVariablesButton.setEnabled(!def);
        fFileSystemButton.setEnabled(!def);
    }

    protected void handleWorkingDirVariablesButtonSelected()
    {
        String variableText = getVariable();
        if (variableText != null)
        {
            fWorkingDirText.insert(variableText);
        }
    }

    private String getVariable()
    {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(
            getShell());
        dialog.open();
        return dialog.getVariableExpression();
    }

    protected void setDefaultWorkingDir()
    {
        fWorkingDirText.setText("${resource_loc}");
    }

    public boolean isValid(ILaunchConfiguration config)
    {
        setErrorMessage(null);
        setMessage(null);

        // if variables are present, we cannot resolve the directory
        String workingDirPath = fWorkingDirText.getText().trim();
        if (workingDirPath.indexOf("${") >= 0) { //$NON-NLS-1$
            IStringVariableManager manager = VariablesPlugin.getDefault()
                .getStringVariableManager();
            try
            {
                manager.validateStringVariables(workingDirPath);
            }
            catch (CoreException e)
            {
                setErrorMessage(e.getMessage());
                return false;
            }
        }
        else if (workingDirPath.length() > 0)
        {
            IContainer container = getContainer();
            if (container == null)
            {
                File dir = new File(workingDirPath);
                if (dir.isDirectory())
                {
                    return true;
                }
                setErrorMessage("Working directory does not exist"); //$NON-NLS-1$
                return false;
            }
        }
        return true;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy config)
    {
        config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
            (String) null);
    }

    public void initializeFrom(ILaunchConfiguration configuration)
    {
        setLaunchConfiguration(configuration);
        try
        {
            String wd = configuration.getAttribute(
                PerlLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
                (String) null);
            fWorkingDirText.setText(""); //$NON-NLS-1$
            if (wd == null)
            {
                fUseDefaultWorkingDirButton.setSelection(true);
            }
            else
            {
                fWorkingDirText.setText(wd);
                fUseDefaultWorkingDirButton.setSelection(false);
            }
            handleUseDefaultWorkingDirButtonSelected();
        }
        catch (CoreException e)
        {
            setErrorMessage(
                "Exception occurred reading configuration: "
                + e.getStatus().getMessage()); //$NON-NLS-1$
            PerlDebugPlugin.log(e);
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration)
    {
        String wd = null;
        if (!isDefaultWorkingDirectory())
        {
            wd = getAttributeValueFrom(fWorkingDirText);
        }
        configuration.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, wd);
    }

    /**
     * Returns the string in the text widget, or <code>null</code> if empty.
     * 
     * @return text or <code>null</code>
     */
    protected String getAttributeValueFrom(Text text)
    {
        String content = text.getText().trim();
        return content.length() > 0 ? content : null;
    }

    public String getName()
    {
        return "Working Directory"; //$NON-NLS-1$
    }

    /**
     * Returns whether the default working directory is to be used
     */
    protected boolean isDefaultWorkingDirectory()
    {
        return fUseDefaultWorkingDirButton.getSelection();
    }

    /**
     * Sets the Perl project currently specified by the given launch config,
     * if any.
     */
    protected void setLaunchConfiguration(ILaunchConfiguration config)
    {
        fLaunchConfiguration = config;
    }

    /**
     * Returns the current java project context
     */
    protected ILaunchConfiguration getLaunchConfiguration()
    {
        return fLaunchConfiguration;
    }
}
