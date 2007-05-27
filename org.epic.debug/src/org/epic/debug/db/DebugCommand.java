package org.epic.debug.db;

import java.io.IOException;

import org.eclipse.debug.core.DebugEvent;

/**
 * Base class for "debug commands" - user commands issued to
 * the debugger. These commands are created by PerlDebugThread
 * and executed by PerlDebugJob. 
 * 
 * @author jploski
 */
abstract class DebugCommand
{
    /**
     * Command completion status indicating that the Perl debugger
     * has suspended after a step.
     */
    public static final int STEP_END = DebugEvent.STEP_END;

    /**
     * Command completion status indicating that the Perl debugger
     * has suspended by reaching a breakpoint.
     */
    public static final int BREAKPOINT = DebugEvent.BREAKPOINT;
    
    /**
     * Command completion status indicating that the Perl debugger
     * has terminated.
     */
    public static final int TERMINATED = STEP_END + BREAKPOINT + 1;
    
    private final PerlDebugThread thread;
    private int completionStatus;
    
    protected DebugCommand(PerlDebugThread thread)
    {
        this.thread = thread;
    }
    
    public int getCompletionStatus()
    {
        return completionStatus;
    }
    
    /**
     * @return true if the command finished because the debugger
     *         has suspended; false if it finished because the debugger
     *         has terminated
     */
    public boolean hasSuspended()
    {
        return
            completionStatus == STEP_END ||
            completionStatus == BREAKPOINT;
    }
    
    public void run() throws IOException
    {
        completionStatus = runImpl();
        getThread().debugCommandFinished(this);
    }
    
    protected DebuggerInterface getDB()
    {
        return thread.getDB();
    }
    
    protected PerlDebugThread getThread()
    {
        return thread;
    }

    protected final IPPosition maybeSkipStringEval(IPPosition startIP)
        throws IOException
    {
        // We currently do not support stepping into variable eval
        // expressions, e.g. eval $str;
        // We detect an attempt to step into such an expression by
        // comparing the IP before and after the current step command.
        // If they are both equal, we step out of the eval expression
        // automatically to skip over it
        
        IPPosition endIP = getDB().getCurrentIP();            
        while (endIP != null && endIP.equals(startIP))
        {
            getDB().stepInto();
            endIP = getDB().getCurrentIP();
        }
        return endIP;
    }

    /**
     * Runs the command until the debugger suspends again or terminates.
     * 
     * @return completion status: STEP_END, BREAKPOINT or TERMINATED
     */
    protected abstract int runImpl() throws IOException;
}
