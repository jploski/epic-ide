package org.epic.debug.cgi;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.epic.debug.DebugTarget;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.util.IPathMapper;
import org.epic.debug.util.RemotePort;

/**
 * An implementation of IDebugTarget which tracks a Perl debugger
 * process launched by the web server and communicates with that
 * process using a TCP port.
 * <p>
 * Termination of the Perl debugger process normally results in
 * termination of the tracking DebugTarget instance. CGIDebugTarget
 * adds a little twist to this behavior: upon termination, it creates
 * a clone of itself, which reconnects to another (new) Perl debugger
 * process listening on the same port. If no Perl debugger responds,
 * reconnection attempts are repeated indefinitely. This strategy
 * allows multiple CGI scripts to be debugged (serially) within
 * the same debug session.
 */
public class CGIDebugTarget extends DebugTarget
{
    private final IDebugEventSetListener listener = new IDebugEventSetListener() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            for (int i = 0; i < events.length; i++)
            {
                if (events[i].getKind() == DebugEvent.TERMINATE)
                {
                	if (getThread().equals(events[i].getSource()))
                	{
                		debugSessionTerminated();
                		return;
                	}                
                	else if (getProcess().equals(events[i].getSource()))
                	{
                		getRemotePort().shutdown();
                	}
                }
            }
        }
    };
    
    private CGIDebugTarget(CGIDebugTarget previous) throws CoreException
    {
        super(
            previous.getLaunch(),
            previous.getProcess(),
            previous.getRemotePort(),
            previous.getPathMapper());

        DebugPlugin.getDefault().addDebugEventListener(listener);
    }
    
    public CGIDebugTarget(
        ILaunch launch,
        IProcess process,
        RemotePort debugPort,
        IPathMapper pathMapper) throws CoreException
    {
        super(launch, process, debugPort, pathMapper);
        DebugPlugin.getDefault().addDebugEventListener(listener);
    }
    
    public String getName() throws DebugException
    {
        return "CGI Perl Debugger";
    }
    
    public IThread[] getThreads() throws DebugException
    {
    	return isTerminated() ? new IThread[0] : super.getThreads();
    }

    public boolean hasThreads() throws DebugException
    {
    	return getThreads().length > 0;
    }

	public void debugSessionTerminated()
	{
        fireTerminateEvent();
        getLaunch().removeDebugTarget(CGIDebugTarget.this);

        Job acceptNewDebugger = new Job("EPIC.acceptNewDebugger") {
        	protected IStatus run(IProgressMonitor monitor)
        	{
        		if (acceptNewDebugger()) { respawn(); return Status.OK_STATUS; }
        		else return Status.CANCEL_STATUS;
        	}
        };
        acceptNewDebugger.setPriority(Job.LONG);
        acceptNewDebugger.setSystem(true);
        acceptNewDebugger.schedule();
	}
    
    private boolean acceptNewDebugger()
    {
        getRemotePort().startReconnect();
        
        if (getRemotePort().waitForConnect(false) != RemotePort.WAIT_OK)
        {
            if (getProcess().isTerminated()) return false;
            
            PerlDebugPlugin.errorDialog("Could not connect to debug port!");
            try { terminate(); }
            catch (DebugException e) { PerlDebugPlugin.log(e); }
            return false;
        }
        return true;
    }
    
    private void respawn()
    {
        try
        {
            DebugPlugin.getDefault().removeDebugEventListener(listener);
            getLaunch().addDebugTarget(
                new CGIDebugTarget(CGIDebugTarget.this));
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            try { terminate(); }
            catch (DebugException _e)
            {
                PerlDebugPlugin.log(_e);
            }
        }
    }
}