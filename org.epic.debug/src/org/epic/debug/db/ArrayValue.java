package org.epic.debug.db;

import java.util.*;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;

/**
 * Represents contents of a Perl array (list).
 * 
 * @author jploski
 */
class ArrayValue extends PerlValue
{
    private final IVariable[] vars;

    public ArrayValue(IDebugTarget target, PerlVariable holder)
        throws DebugException
    {
        super(target, holder);
        
        this.vars = parseArrayContent(dumpEntity("dump_array_expr"));
    }
    
    public IVariable[] getVariables() throws DebugException
    {
        return vars;
    }

    public boolean hasVariables() throws DebugException
    {
        return vars.length > 0;
    }
    
    private IVariable[] parseArrayContent(String content) throws DebugException
    {
        DumpedEntityReader r = new DumpedEntityReader(content);
        List vars = new ArrayList();
        
        while (r.hasMoreEntities())
            vars.add(new ArrayElement(
                getHolder().getDebuggerInterface(),
                getHolder(),
                r.nextEntity()));

        return (IVariable[]) vars.toArray(new IVariable[vars.size()]);
    }
}
