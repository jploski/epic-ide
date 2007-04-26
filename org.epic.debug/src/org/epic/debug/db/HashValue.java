package org.epic.debug.db;

import java.util.*;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;

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
        List vars = new ArrayList();
        
        while (r.hasMoreEntities())
            vars.add(new HashKey(
                getHolder().getDebuggerInterface(),
                getHolder(),
                r.nextEntity()));

        return (IVariable[]) vars.toArray(new IVariable[vars.size()]);
    }
}
