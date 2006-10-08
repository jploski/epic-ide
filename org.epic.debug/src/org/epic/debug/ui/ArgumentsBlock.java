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
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.epic.debug.PerlDebugPlugin;

/**
 * Editor for Perl arguments of a Perl launch configuration.
 * 
 * Adapted from {@link org.eclipse.jdt.internal.debug.ui.launcher.VMArgumentsBlock}.
 * 
 * @author jploski
 */
public class ArgumentsBlock extends AbstractLaunchConfigurationTab
{
    private final String fTitle;
    private final String fLaunchConfigAttr;
    private Text fPerlArgumentsText;
    private Button fPerlArgVariableButton;
    
    public ArgumentsBlock(String title, String launchConfigAttr)
    {
        fTitle = title;
        fLaunchConfigAttr = launchConfigAttr;
    }

    public void createControl(Composite parent)
    {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        setControl(group);
        GridLayout topLayout = new GridLayout();
        group.setLayout(topLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);
        group.setFont(font);
        group.setText(fTitle);

        fPerlArgumentsText = new Text(
            group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 40;
        gd.widthHint = 100;
        fPerlArgumentsText.setLayoutData(gd);
        fPerlArgumentsText.setFont(font);
        fPerlArgumentsText.addModifyListener(new ModifyListener()
        {
            public void modifyText(ModifyEvent evt)
            {
                updateLaunchConfigurationDialog();
            }
        });

        fPerlArgVariableButton = createPushButton(group, "Varia&bles...", null);
        fPerlArgVariableButton.setFont(font);
        fPerlArgVariableButton.setLayoutData(
            new GridData(GridData.HORIZONTAL_ALIGN_END));
        fPerlArgVariableButton.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                StringVariableSelectionDialog dialog = 
                    new StringVariableSelectionDialog(getShell());
                dialog.open();
                String variable = dialog.getVariableExpression();
                if (variable != null)
                {
                    fPerlArgumentsText.insert(variable);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
            }

        });
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
    {
        configuration.setAttribute(fLaunchConfigAttr, (String) null);
    }

    public void initializeFrom(ILaunchConfiguration configuration)
    {
        try
        {
            fPerlArgumentsText.setText(configuration.getAttribute(
                fLaunchConfigAttr, "")); //$NON-NLS-1$
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
            fLaunchConfigAttr,
            getAttributeValueFrom(fPerlArgumentsText));
    }

    public String getName()
    {
        return "Perl Arguments";
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

    public void setEnabled(boolean enabled)
    {
        fPerlArgumentsText.setEnabled(enabled);
        fPerlArgVariableButton.setEnabled(enabled);
    }
}
