package org.epic.debug.remote;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.epic.debug.DebugTarget;
import org.epic.debug.util.*;

class RemoteDebugTarget extends DebugTarget
{
    private final PathMapper mapper;
    
    public RemoteDebugTarget(
        ILaunch launch,
        DebuggerProxy process,
        RemotePort debugPort,
        PathMapper mapper,
        IPath workingDir)
        throws CoreException
    {
        super(launch, process, debugPort, workingDir);
        this.mapper = mapper;
    }
    
    public PathMapper getPathMapper()
    {
        return mapper;
    }

    protected void initDebugger() throws DebugException
    {
        getDebugger().init(
            getProxy().getIOHost(),
            getProxy().getIOPort(),
            getProxy().getErrorPort());
    }
    
    private DebuggerProxy getProxy()
    {
        return (DebuggerProxy) getProcess();
    }
}
