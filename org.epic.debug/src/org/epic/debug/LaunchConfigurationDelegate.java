package org.epic.debug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.*;
import org.epic.core.Constants;
import org.epic.core.Perspective;
import org.epic.debug.util.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Abstract base class for Perl launch configuration delegates.
 * This class defines some helper methods and common behavior useful
 * regardless of the actual launch configuration type. Subclasses
 * must implement {@link #doLaunch}.
 */
public abstract class LaunchConfigurationDelegate
    extends org.eclipse.debug.core.model.LaunchConfigurationDelegate
{
    public final void launch(
        ILaunchConfiguration configuration,
        String mode,
        ILaunch launch,
        IProgressMonitor monitor) throws CoreException
    {
        launch.setSourceLocator(new SourceLocator());

        doLaunch(configuration, mode, launch, monitor);
        
        // Switch to Debug Perspective
        if (launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE))
            Perspective.switchPerspective(Constants.DEBUG_PERSPECTIVE_ID);
    }
    
    protected abstract void doLaunch(
        ILaunchConfiguration configuration,
        String mode,
        ILaunch launch,
        IProgressMonitor monitor) throws CoreException;
    
    
    protected IPathMapper getPathMapper(ILaunch launch) throws CoreException
    {
        if (isCygwin())
            return new CygwinPathMapper();
        else
            return new NullPathMapper();
    }

    protected IProject getProject(ILaunch launch) throws CoreException
    {
        String projectName =
            launch.getLaunchConfiguration().getAttribute(
                PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                "");
        
        return PerlDebugPlugin.getWorkspace().getRoot().getProject(
            projectName);
    }
    
    protected final IProject[] getProjectsForProblemSearch(
        ILaunchConfiguration configuration, String mode)
    {
        try
        {            
            String prjName =
                configuration.getAttribute(
                    PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                    (String)null);

            IProject p[] = new IProject[1];
            p[0] = PerlDebugPlugin.getWorkspace().getRoot().getProject(prjName);
            return p;
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return null;
        }
    }
    
    protected final boolean isCygwin()
    {
        String interpreterType = PerlEditorPlugin.getDefault()
            .getPreferenceStore().getString(
                PreferenceConstants.DEBUG_INTERPRETER_TYPE);

        return PreferenceConstants.DEBUG_INTERPRETER_TYPE_CYGWIN.equals(
            interpreterType);
    }
    
    protected final boolean isDebugMode(ILaunch launch)
    {
        return launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE);
    }
}
