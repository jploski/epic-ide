/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.epic.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLaunchConfigurationConstants;

/**
 * A launch configuration tab that displays and edits program arguments, Perl
 * interpreter arguments, and working directory launch configuration attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed.
 * </p>
 */
public class LaunchConfigurationArgumentsTab
    extends AbstractLaunchConfigurationTab
{
    private Text fPrgmArgumentsText;
    private PerlArgumentsBlock fVMArgumentsBlock;
    private WorkingDirectoryBlock fWorkingDirectoryBlock;    

    public LaunchConfigurationArgumentsTab()
    {
        fVMArgumentsBlock = createVMArgsBlock();
        fWorkingDirectoryBlock = createWorkingDirBlock();
    }

    protected PerlArgumentsBlock createVMArgsBlock()
    {
        return new PerlArgumentsBlock();
    }

    protected WorkingDirectoryBlock createWorkingDirBlock()
    {
        return new WorkingDirectoryBlock();
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
     */
    public void createControl(Composite parent)
    {
        Font font = parent.getFont();
        Composite comp = new Composite(parent, parent.getStyle());
        GridLayout layout = new GridLayout(1, true);
        comp.setLayout(layout);
        comp.setFont(font);

        GridData gd = new GridData(GridData.FILL_BOTH);
        comp.setLayoutData(gd);
        setControl(comp);

        Group group = new Group(comp, SWT.NONE);
        group.setFont(font);
        layout = new GridLayout();
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        String controlName = "Program &arguments:";
        group.setText(controlName);

        fPrgmArgumentsText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER
            | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 40;
        gd.widthHint = 100;
        fPrgmArgumentsText.setLayoutData(gd);
        fPrgmArgumentsText.setFont(font);
        fPrgmArgumentsText.addModifyListener(new ModifyListener()
        {
            public void modifyText(ModifyEvent evt)
            {
                updateLaunchConfigurationDialog();
            }
        });

        String buttonLabel = "Var&iables..."; //$NON-NLS-1$
        Button pgrmArgVariableButton = createPushButton(group, buttonLabel,
            null);
        pgrmArgVariableButton.setLayoutData(new GridData(
            GridData.HORIZONTAL_ALIGN_END));
        pgrmArgVariableButton.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(
                    getShell());
                dialog.open();
                String variable = dialog.getVariableExpression();
                if (variable != null)
                {
                    fPrgmArgumentsText.insert(variable);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });

        fVMArgumentsBlock.createControl(comp);

        fWorkingDirectoryBlock.createControl(comp);
    }

    public void dispose()
    {
    }

    public boolean isValid(ILaunchConfiguration config)
    {
        return fWorkingDirectoryBlock.isValid(config);
    }

    /**
     * Defaults are empty.
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy config)
    {
        config.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
            (String) null);
        fVMArgumentsBlock.setDefaults(config);
        fWorkingDirectoryBlock.setDefaults(config);
    }

    public void initializeFrom(ILaunchConfiguration configuration)
    {
        try
        {
            fPrgmArgumentsText.setText(configuration.getAttribute(
                PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS, "")); //$NON-NLS-1$
            fVMArgumentsBlock.initializeFrom(configuration);
            fWorkingDirectoryBlock.initializeFrom(configuration);
        }
        catch (CoreException e)
        {
            setErrorMessage(
                "Exception occurred reading configuration:"
                + e.getStatus().getMessage());
            PerlDebugPlugin.log(e);
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration)
    {
        configuration.setAttribute(
            PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
            getAttributeValueFrom(fPrgmArgumentsText));
        fVMArgumentsBlock.performApply(configuration);
        fWorkingDirectoryBlock.performApply(configuration);
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
        return "Arguments";
    }

    public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog)
    {
        super.setLaunchConfigurationDialog(dialog);
        fWorkingDirectoryBlock.setLaunchConfigurationDialog(dialog);
        fVMArgumentsBlock.setLaunchConfigurationDialog(dialog);
    }

    public String getErrorMessage()
    {
        String m = super.getErrorMessage();
        if (m == null)
        {
            return fWorkingDirectoryBlock.getErrorMessage();
        }
        return m;
    }

    public String getMessage()
    {
        String m = super.getMessage();
        if (m == null)
        {
            return fWorkingDirectoryBlock.getMessage();
        }
        return m;
    }

    public Image getImage()
    {
        return
            PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
                PerlDebugImages.DESC_OBJS_LaunchTabArguments);
    }

    public void activated(ILaunchConfigurationWorkingCopy workingCopy)
    {
        // do nothing when activated
    }

    public void deactivated(ILaunchConfigurationWorkingCopy workingCopy)
    {
        // do nothing when deactivated
    }
}
