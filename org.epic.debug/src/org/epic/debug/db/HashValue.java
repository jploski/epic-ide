package org.epic.debug.db;

import java.util.*;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.PerlDebugPlugin;

/**
 * Represents contents of a Perl hash (blessed or not, doesn't matter).
 * 
 * @author jploski
 */
class HashValue extends PerlValue
{
    private final IVariable[] vars;

    public HashValue(IDebugTarget target, PerlVariable holder)
        throws DebugException
    {
        super(target, holder);
        
        this.vars = parseHashContent(dumpEntity("dump_hash_expr"));
    }
    
    public IVariable[] getVariables() throws DebugException
    {
        return vars;
    }

    public boolean hasVariables() throws DebugException
    {
        return vars.length > 0;
    }
    
    private IVariable[] parseHashContent(String content) throws DebugException
    {
        DumpedEntityReader r = new DumpedEntityReader(content);
        List<HashKey> vars = new ArrayList<HashKey>();
        
        try
        {
            while (r.hasMoreEntities())
                vars.add(new HashKey(
                    getHolder().getDebuggerInterface(),
                    getHolder(),
                    r.nextEntity()));
        }
        catch (Exception e)
        {
            PerlDebugPlugin.log(e);
            throw new DebugException(new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                "An error occurred while dumping array content; " +
                "contents of the Variables view may become invalid",
                e));
        }

        return vars.toArray(new IVariable[vars.size()]);
    }
}
