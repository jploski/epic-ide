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

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.*;
import org.eclipse.swt.SWT;
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
    private ArgumentsBlock fPerlArgumentsBlock;
    private ArgumentsBlock fProgramArgumentsBlock;
    private WorkingDirectoryBlock fWorkingDirectoryBlock;    

    /**
     * @param perlArgumentsOnly
     *        true if the "Program arguments" and "Working directory"
     *        blocks should be suppressed; false if they should appear 
     */
    public LaunchConfigurationArgumentsTab(boolean perlArgumentsOnly)
    {
        fPerlArgumentsBlock = createPerlArgsBlock();
        
        if (!perlArgumentsOnly)
        {
            fProgramArgumentsBlock = createProgramArgsBlock();
            fWorkingDirectoryBlock = createWorkingDirBlock();
        }
    }

    protected ArgumentsBlock createPerlArgsBlock()
    {
        return new ArgumentsBlock(
            "Perl ar&guments:",
            PerlLaunchConfigurationConstants.ATTR_PERL_PARAMETERS);
    }

    protected ArgumentsBlock createProgramArgsBlock()
    {
        return new ArgumentsBlock(
            "Program &arguments:",
            PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS);
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
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        comp.setLayout(layout);
        comp.setFont(font);

        GridData gd = new GridData(GridData.FILL_BOTH);
        comp.setLayoutData(gd);
        setControl(comp);
        
        if (fProgramArgumentsBlock != null)
            fProgramArgumentsBlock.createControl(comp);
        if (fPerlArgumentsBlock != null)
            fPerlArgumentsBlock.createControl(comp);
        if (fWorkingDirectoryBlock != null)
            fWorkingDirectoryBlock.createControl(comp);
    }

    public void dispose()
    {
    }

    public boolean isValid(ILaunchConfiguration config)
    {
        return fWorkingDirectoryBlock != null
            ? fWorkingDirectoryBlock.isValid(config)
            : true;
    }

    /**
     * Defaults are empty.
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy config)
    {
        if (fProgramArgumentsBlock != null)
            fProgramArgumentsBlock.setDefaults(config);
        if (fPerlArgumentsBlock != null)
            fPerlArgumentsBlock.setDefaults(config);
        if (fWorkingDirectoryBlock != null)
            fWorkingDirectoryBlock.setDefaults(config);
    }

    public void initializeFrom(ILaunchConfiguration configuration)
    {
        if (fProgramArgumentsBlock != null)
            fProgramArgumentsBlock.initializeFrom(configuration);
        if (fPerlArgumentsBlock != null)
            fPerlArgumentsBlock.initializeFrom(configuration);
        if (fWorkingDirectoryBlock != null)
            fWorkingDirectoryBlock.initializeFrom(configuration);
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration)
    {
        if (fProgramArgumentsBlock != null)
            fProgramArgumentsBlock.performApply(configuration);
        if (fPerlArgumentsBlock != null)
            fPerlArgumentsBlock.performApply(configuration);
        if (fWorkingDirectoryBlock != null)
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
        if (fWorkingDirectoryBlock != null)
            fWorkingDirectoryBlock.setLaunchConfigurationDialog(dialog);
        if (fProgramArgumentsBlock != null)
            fProgramArgumentsBlock.setLaunchConfigurationDialog(dialog);
        if (fPerlArgumentsBlock != null)
            fPerlArgumentsBlock.setLaunchConfigurationDialog(dialog);
    }

    public String getErrorMessage()
    {
        String m = super.getErrorMessage();
        if (m == null && fWorkingDirectoryBlock != null)
        {
            return fWorkingDirectoryBlock.getErrorMessage();
        }
        return m;
    }

    public String getMessage()
    {
        String m = super.getMessage();
        if (m == null && fWorkingDirectoryBlock != null)
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
