package org.epic.debug.local;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlTarget;

/**
 * An implementation of IDebugTarget which tracks a local Perl
 * process launched in normal (non-debug) mode.
 */
public class RunLocalTarget extends PerlTarget
{
    private final IProcess process;

    public RunLocalTarget(
        ILaunch launch, IProcess process) throws CoreException
    {
        super(launch);
        
        this.process = process;
    }
    
    public String getName() throws DebugException
    {
        return process.getLabel();
    }
    
    public IProcess getProcess()
    {
        return process;
    }

    public IThread[] getThreads() throws DebugException
    {
        return new IThread[0];
    }

    public boolean hasThreads() throws DebugException
    {
        return false;
    }
    
    public boolean isLocal()
    {
        return true;
    }

    public boolean supportsBreakpoint(IBreakpoint breakpoint)
    {
        return false;
    }

    public boolean canTerminate()
    {
        return !isTerminated();
    }

    public boolean isTerminated()
    {
        return process.isTerminated();
    }

    public void terminate() throws DebugException
    {
        process.terminate();
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
        unsupportedOperation();
    }

    public void suspend() throws DebugException
    {
        unsupportedOperation();
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
        unsupportedOperation();
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
        unsupportedOperation();
        return null;
    }
}