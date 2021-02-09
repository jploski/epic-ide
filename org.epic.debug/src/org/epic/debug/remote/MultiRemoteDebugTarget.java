package org.epic.debug.remote;

import java.io.IOException;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.epic.debug.*;
import org.epic.debug.db.DebuggerInterface;
import org.epic.debug.util.*;

class MultiRemoteDebugTarget extends DebugElement implements IDebugTarget
{
	boolean done=false;
	RemotePort port=null;
    private final IDebugEventSetListener listener = new IDebugEventSetListener() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            for (int i = 0; i < events.length; i++)
            {
                try
                {
                    if (events[i].getKind() == DebugEvent.TERMINATE)
                    {
                        DebugPlugin.getDefault().removeDebugEventListener(this);
                        terminate(); // we're done when our thread is
                        return;
                    }
                }
                catch (DebugException e)
                {
                    PerlDebugPlugin.log(e);
                }
            }
        } };
    
    public MultiRemoteDebugTarget(
        ILaunch launch,
        RemotePort debugPort)
        throws CoreException
    {
        super(null);
        DebugPlugin.getDefault().addDebugEventListener(listener);
    }

	@Override
	public String getModelIdentifier() {
		return null;
	}

	@Override
	public boolean canTerminate() {
		return true;
	}

	@Override
	public boolean isTerminated() {
		return done;
	}

	@Override
	public void terminate() throws DebugException {
		port.shutdown();
		done=true;
	}

	@Override
	public boolean canResume() {
		return false;
	}

	@Override
	public boolean canSuspend() {
		return false;
	}

	@Override
	public boolean isSuspended() {
		return false;
	}

	@Override
	public void resume() throws DebugException {
	}

	@Override
	public void suspend() throws DebugException {
	}

	@Override
	public void breakpointAdded(IBreakpoint arg0) {
	}

	@Override
	public void breakpointChanged(IBreakpoint arg0, IMarkerDelta arg1) {
	}

	@Override
	public void breakpointRemoved(IBreakpoint arg0, IMarkerDelta arg1) {
	}

	@Override
	public boolean canDisconnect() {
		return false;
	}

	@Override
	public void disconnect() throws DebugException {
	}

	@Override
	public boolean isDisconnected() {
		return false;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long arg0, long arg1)
			throws DebugException {
		return null;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

	@Override
	public String getName() throws DebugException {
		return null;
	}

	@Override
	public IProcess getProcess() {
		return null;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return null;
	}

	@Override
	public boolean hasThreads() throws DebugException {
		return false;
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint arg0) {
		return false;
	}

}
