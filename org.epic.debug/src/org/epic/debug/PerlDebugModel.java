package org.epic.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

public class PerlDebugModel
{
    public static IBreakpoint lineBreakpointExists(IResource resource, int lineNumber) throws CoreException
    {
        String modelId = PerlDebugPlugin.getUniqueIdentifier();

        IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
        IBreakpoint[] breakpoints = manager.getBreakpoints(modelId);

        for (int i = 0; i < breakpoints.length; i++)
        {
            PerlLineBreakpoint breakpoint = (PerlLineBreakpoint) breakpoints[i];
            IMarker marker = breakpoint.getMarker();
            if (isValidMarker(marker))
            {
                // XXX: refactor this into methods
                if (breakpoint.getLineNumber() == lineNumber && resource.equals(marker.getResource()))
                {
                    return breakpoint;
                }
            }
        }

        return null;
    }

    private static boolean isValidMarker(IMarker marker)
    {
        return marker != null && marker.exists();
    }
}
