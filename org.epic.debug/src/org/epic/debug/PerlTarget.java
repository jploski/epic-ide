package org.epic.debug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.*;

/**
 * @author ruehl
 */
public abstract class PerlTarget extends DebugElement implements IDebugTarget
{
    private ILaunch launch;
    private boolean mShutDownStarted;

    private final IProject project;

    public PerlTarget(ILaunch launch) throws CoreException
    {
        super(null);

        this.launch = launch;
        this.project = initProject();

        fireCreationEvent();
    }
    
    public IDebugTarget getDebugTarget()
    {
        return this;
    }

    public ILaunch getLaunch()
    {
        return launch;
    }

    public String getModelIdentifier()
    {
        return PerlDebugPlugin.getUniqueIdentifier();
    }

    public IProject getProject()
    {
        return project;
    }

    public abstract boolean isLocal();

    protected void shutdown()
    {  
        if (mShutDownStarted) return;
        mShutDownStarted = true;

        try
        {
            if (getProcess() != null) getProcess().terminate();
        }
        catch (DebugException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Error terminating process " + getProcess().getLabel(),
                e);
        }
        fireTerminateEvent();
    }
    
    protected final void unsupportedOperation() throws DebugException
    {
        throw new DebugException(new Status(
            IStatus.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            DebugException.NOT_SUPPORTED,
            "operation not supported",
            null));
    }

    protected IProject initProject() throws CoreException
    {
        String projectName =
            getLaunch().getLaunchConfiguration().getAttribute(
                PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                "");
        
        return PerlDebugPlugin.getWorkspace().getRoot().getProject(
            projectName);
    }
}