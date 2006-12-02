package org.epic.debug.db;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.epic.debug.*;

/**
 * Used to map each source file path to a set of PerlBreakpoints.
 * 
 * @author ruehl
 */
class BreakpointMap
{
    private final Map breakpoints;

    public BreakpointMap()
    {
        breakpoints = new HashMap();
    }

    public void add(PerlBreakpoint bp)
    {
        IPath path = bp.getResourcePath();
        
        Set set = (Set) breakpoints.get(path);
        if (set == null)
        {
            set = new HashSet();
            breakpoints.put(path, set);
        }
        set.add(bp);
    }

    public boolean contains(PerlBreakpoint bp)
    {
        IPath path = bp.getResourcePath();

        Set set = (Set) breakpoints.get(path);
        if (set == null) return false;

        return set.contains(bp);
    }

    public Set getBreakpoints(IPath path)
    {
        Set set = (Set) breakpoints.get(path);
        return set != null ? set : Collections.EMPTY_SET;
    }

    public PerlBreakpoint getBreakpoint(IPath path, int line)
    {
        for (Iterator i = getBreakpoints(path).iterator(); i.hasNext();)
        {
            Object obj = i.next();
            if (!(obj instanceof PerlLineBreakpoint)) continue;
            
            PerlLineBreakpoint bp = (PerlLineBreakpoint) obj;
            try
            {
                if (bp.getResourcePath().equals(path) &&
                    line == bp.getLineNumber())
                {
                    return bp;
                }
            }
            catch (CoreException e)
            {
                PerlDebugPlugin.log(e);
            }
        }
        return null;
    }

    public boolean remove(PerlBreakpoint bp)
    {
        IPath path = bp.getResourcePath();

        Set set = (Set) breakpoints.get(path);
        if (set == null) return (false);

        return set.remove(bp);
    }
}
