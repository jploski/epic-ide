package org.epic.debug.db;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.epic.debug.PerlDebugPlugin;

/**
 * The "resume" DebugCommand. Lets the debugger continue execution
 * until either a breakpoint (known to EPIC) is reached or the debugger
 * terminates. 
 * 
 * @author jploski
 */
class ResumeCommand extends DebugCommand
{
    public ResumeCommand(PerlDebugThread thread)
    {
        super(thread);
    }
    
    protected int runImpl() throws IOException
    {
        try
        {            
            boolean breakpointReached = false;
            while (!breakpointReached)
            {
                getDB().resume();                
                breakpointReached = getThread().isBreakpointReached();
            }
            return BREAKPOINT;
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return BREAKPOINT;
        }
        catch (DebuggerInterface.SessionTerminatedException e)
        {
            return TERMINATED;
        }
    }
}
