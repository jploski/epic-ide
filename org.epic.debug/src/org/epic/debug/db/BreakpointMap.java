package org.epic.debug.db;

import java.io.IOException;
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
        String path = canonPath(bp.getResourcePath());
        if (path == null) return;
        
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
        String path = canonPath(bp.getResourcePath());
        if (path == null) return false;

        Set set = (Set) breakpoints.get(path);
        if (set == null) return false;

        return set.contains(bp);
    }

    public Set getBreakpoints(IPath path)
    {
        String canonPath = canonPath(path);
        if (canonPath == null) return Collections.EMPTY_SET;
        
        Set set = (Set) breakpoints.get(canonPath);
        return set != null ? set : Collections.EMPTY_SET;
    }

    public PerlBreakpoint getBreakpoint(IPath path, int line)
    {
        String canonPath = canonPath(path);

        for (Iterator i = getBreakpoints(path).iterator(); i.hasNext();)
        {
            Object obj = i.next();
            if (!(obj instanceof PerlLineBreakpoint)) continue;
            
            PerlLineBreakpoint bp = (PerlLineBreakpoint) obj;
            try
            {
                String bpPath = canonPath(bp.getResourcePath());
                if (bpPath.equals(canonPath) && line == bp.getLineNumber())
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
        String path = canonPath(bp.getResourcePath());

        Set set = (Set) breakpoints.get(path);
        if (set == null) return (false);

        return set.remove(bp);
    }
    
    private String canonPath(IPath path)
    {
        // here we do the path comparisons on canonical path to avoid
        // any ambiguities due to symlinks and the like
        try
        {
            return path.toFile().getCanonicalPath();
        }
        catch (IOException e)
        {
            PerlDebugPlugin.log(e);
            return null;
        }
    }
}