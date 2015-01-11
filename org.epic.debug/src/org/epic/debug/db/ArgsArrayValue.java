package org.epic.debug.db;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.epic.debug.PerlDebugPlugin;

/**
 * A subclass of ArrayValue for displaying the contents of
 * the Perl internal \@_ variable. Unlike normal array variables,
 * it cannot use dump_entity.pl because @_ is not in any symbol table.
 */
public class ArgsArrayValue extends ArrayValue
{
    private static final String DB_DUMP_ARGS;
    
    static
    {
        DB_DUMP_ARGS = HelperScript.load("dump_args.pl");
    }
    
    public ArgsArrayValue(IDebugTarget target, PerlVariable holder)
        throws DebugException
    {
        super(target, holder);
    }
    
    protected String dumpEntity(String subName) throws DebugException
    {
        try
        {
            PerlVariable holder = getHolder();
            if (!holder.getStackFrame().getThread().isSuspended()) return "";
            
            return holder.getDebuggerInterface().eval(DB_DUMP_ARGS);
        }
        catch (IOException e)
        {
            throw new DebugException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "An error occurred while retrieving variables from the debugger process",
                e));
        }
    }
}
