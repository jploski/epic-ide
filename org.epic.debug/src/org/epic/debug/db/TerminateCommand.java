package org.epic.debug.db;

import java.io.IOException;

import org.epic.debug.PerlDebugPlugin;

/**
 * The "bye-bye" DebugCommand. Terminates the debugger session. 
 * 
 * @author jploski
 */
class TerminateCommand extends DebugCommand
{
    public TerminateCommand(PerlDebugThread thread)
    {
        super(thread);
    }

    protected int runImpl()
    {
        try { getDB().quit(); }
        catch (DebuggerInterface.SessionTerminatedException e) { }
        catch (IOException e)
        {
            PerlDebugPlugin.log(e);
        }
        return TERMINATED;
    }
}
