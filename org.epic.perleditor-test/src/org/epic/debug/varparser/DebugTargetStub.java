package org.epic.debug.varparser;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.*;

class DebugTargetStub implements IDebugTarget
{
    public IProcess getProcess()
    {
        return null;
    }

    public IThread[] getThreads() throws DebugException
    {
        return null;
    }

    public boolean hasThreads() throws DebugException
    {
        return false;
    }

    public String getName() throws DebugException
    {
        return "DebugTargetStub";
    }

    public boolean supportsBreakpoint(IBreakpoint breakpoint)
    {
        return false;
    }

    public String getModelIdentifier()
    {
        return "org.epic.debug";
    }

    public IDebugTarget getDebugTarget()
    {
        return null;
    }

    public ILaunch getLaunch()
    {
        return null;
    }

    public Object getAdapter(Class adapter)
    {
        return null;
    }

    public boolean canTerminate()
    {
        return false;
    }

    public boolean isTerminated()
    {
        return false;
    }

    public void terminate() throws DebugException
    {
    }

    public boolean canResume()
    {
        return false;
    }

    public boolean canSuspend()
    {
        return false;
    }

    public boolean isSuspended()
    {
        return false;
    }

    public void resume() throws DebugException
    {
    }

    public void suspend() throws DebugException
    {
    }

    public void breakpointAdded(IBreakpoint breakpoint)
    {
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta)
    {
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta)
    {
    }

    public boolean canDisconnect()
    {
        return false;
    }

    public void disconnect() throws DebugException
    {
    }

    public boolean isDisconnected()
    {
        return false;
    }

    public boolean supportsStorageRetrieval()
    {
        return false;
    }

    public IMemoryBlock getMemoryBlock(long startAddress, long length)
        throws DebugException
    {
        return null;
    }
}
