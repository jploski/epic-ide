package org.epic.debug.remote;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.epic.debug.*;
import org.epic.debug.db.DebuggerInterface;
import org.epic.debug.util.*;

class RemoteDebugTarget extends DebugTarget
{
    private final IDebugEventSetListener listener = new IDebugEventSetListener() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            for (int i = 0; i < events.length; i++)
            {
                try
                {
                    if (events[i].getKind() == DebugEvent.TERMINATE &&
                        getThreads()[0].equals(events[i].getSource()))
                    {
                        DebugPlugin.getDefault().removeDebugEventListener(this);
                        shutdown(); // we're done when our thread is
                        return;
                    }
                }
                catch (DebugException e)
                {
                    PerlDebugPlugin.log(e);
                }
            }
        } };
    
    public RemoteDebugTarget(
        ILaunch launch,
        DebuggerProxy process,
        RemotePort debugPort,
        IPathMapper pathMapper)
        throws CoreException
    {
        super(launch, process, debugPort, pathMapper);
        DebugPlugin.getDefault().addDebugEventListener(listener);
    }

    protected DebuggerInterface initDebuggerInterface(DebuggerInterface db)
        throws DebugException
    {
        boolean shouldRedirectIO = true;
        try
        {
            shouldRedirectIO = getLaunch().getLaunchConfiguration().getAttribute(
                PerlLaunchConfigurationConstants.ATTR_REMOTE_CAPTURE_OUTPUT,
                true);
        }
        catch (CoreException e) { /* should never occur */ } 

        if (getProxy().getIOHost() != null && shouldRedirectIO)
        {
            try
            {
                db.redirectIO(getProxy().getIOHost(), getProxy().getIOPort());
                db.redirectError(getProxy().getIOHost(), getProxy().getErrorPort());
            }
            catch (IOException e) { throwDebugException(e); }
        }

        return super.initDebuggerInterface(db);
    }
    
    private DebuggerProxy getProxy()
    {
        return (DebuggerProxy) getProcess();
    }
}
