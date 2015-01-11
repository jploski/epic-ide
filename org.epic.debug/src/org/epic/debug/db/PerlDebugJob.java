package org.epic.debug.db;

import java.io.IOException;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.epic.debug.PerlDebugPlugin;

/**
 * The job responsible for asynchronous communication with the debugger.
 * In response to a user interface event, PerlDebugThread alters its
 * state, fires a debug event, and schedules a DebugCommand to be executed
 * by this job. When the command completes, it notifies PerlDebugThread
 * which again alters its state and fires a debug event.
 * 
 * Note that not all communication with the debugger occurs through
 * the PerlDebugJob. Asynchronous retrieving of Perl variable values
 * occurs independently from that. However, all communication with
 * the debugger is serial thanks to synchronization in DebuggerInterface. 
 * 
 * @author jploski
 */
class PerlDebugJob extends Job
{
    private DebugCommand command;
    
    public PerlDebugJob()
    {
        super("PerlDebugJob");
        
        setSystem(true);
    }

    protected IStatus run(IProgressMonitor monitor)
    {
        DebugCommand cmd;
        synchronized (this)
        {
            cmd = command;
            command = null;
        }
        try { cmd.run(); }
        catch (IOException e)
        {
            return new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                "An IOException occurred while executing debugger command",
                e);
        }
        return Status.OK_STATUS;
    }

    public void setCommand(DebugCommand command)
    {
        synchronized (this)
        {
            this.command = command;
        }
    }
}
