package org.epic.debug;

import java.io.BufferedReader;
import java.io.PrintWriter;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.epic.debug.db.PerlDB;
import org.epic.debug.util.*;

/**
 * An implementation of IDebugTarget which tracks a Perl debugger
 * process and communicates with it using a TCP port.
 */
public class DebugTarget extends PerlTarget
{   
    private final IProcess process;
    private final RemotePort debugPort;
    private final PerlDB perlDB;
    private final IPathMapper pathMapper;
    
    public DebugTarget(
        ILaunch launch,
        IProcess process,
        RemotePort debugPort,
        IPathMapper pathMapper) throws CoreException
    {
        super(launch);
        
        this.process = process;
        this.debugPort = debugPort;
        this.perlDB = new PerlDB(this);
        this.pathMapper = pathMapper;
        if (!perlDB.isTerminated()) initDebugger();
    }
    
    public PerlDB getDebugger()
    {
        return perlDB;
    }
    
    public String getName() throws DebugException
    {
        return getProcess().getLabel();
    }
    
    public IProcess getProcess()
    {
        return process;
    }
    
    public RemotePort getRemotePort()
    {
        return debugPort;
    }

    public boolean isLocal()
    {
        return true;
    }
    
    public int getDebugPort()
    {
        return debugPort.getServerPort();
    }
    
    public BufferedReader getDebugReadStream()
    {
        return debugPort.getReadStream();
    }

    public PrintWriter getDebugWriteStream()
    {
        return debugPort.getWriteStream();
    }

    public IPathMapper getPathMapper()
    {
        return pathMapper;
    }

    public void debugSessionTerminated()
    {
        shutdown();
    }

    public void perlDBstarted(PerlDB perlDB)
    {
    }

    public IThread[] getThreads() throws DebugException
    {
        return perlDB.getThreads();
    }

    public boolean hasThreads() throws DebugException
    {
        return perlDB.getThreads() != null;
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
        return perlDB.isTerminated(this);
    }

    public void terminate() throws DebugException
    {
        shutdown();
    }

    public boolean canResume()
    {
        return perlDB.canResume(this);
    }

    public boolean canSuspend()
    {
        return perlDB.canSuspend(this);
    }

    public boolean isSuspended()
    {
        return perlDB.isSuspended(this);
    }

    public void resume() throws DebugException
    {
        perlDB.resume(this);
    }

    public void suspend() throws DebugException
    {
        perlDB.suspend(this);
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

    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException
    {
        unsupportedOperation();
        return null;
    }
    
    protected void initDebugger() throws DebugException
    {
        getDebugger().init(null, -1, -1);
    }
    
    protected void shutdown()
    {
        debugPort.shutdown();
        super.shutdown();
    }
}