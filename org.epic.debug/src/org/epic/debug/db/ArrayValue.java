package org.epic.debug.db;

import java.util.*;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.PerlDebugPlugin;

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
        List<ArraySlice> slices = null;
        List<ArrayElement> vars = new ArrayList<ArrayElement>();
        
        try
        {
        	int i = 0, sliceStartI = 0;
            while (r.hasMoreEntities())
            {
                vars.add(new ArrayElement(
                    getHolder().getDebuggerInterface(),
                    getHolder(),
                    r.nextEntity()));

                i++;
                
                if (vars.size() == 1000)
                {
                	if (slices == null) slices = new ArrayList<ArraySlice>();
                	slices.add(new ArraySlice(getHolder(), vars, sliceStartI));
                	sliceStartI = i;
                	vars = new ArrayList<ArrayElement>();
                }
            }
            if (slices != null)
            {
            	if (!vars.isEmpty())
            		slices.add(new ArraySlice(getHolder(), vars, sliceStartI));
            	return slices.toArray(new IVariable[slices.size()]);
            }
            else
            {
            	return vars.toArray(new IVariable[vars.size()]);
            }
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
    }
}
