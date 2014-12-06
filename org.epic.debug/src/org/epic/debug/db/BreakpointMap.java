package org.epic.debug.db;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.epic.debug.PerlBreakpoint;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLineBreakpoint;

/**
 * Used to map each source file path to a set of PerlBreakpoints.
 * 
 * @author ruehl
 */
class BreakpointMap
{
    private final Map<String, Set<PerlBreakpoint>> breakpoints;

    public BreakpointMap()
    {
        breakpoints = new HashMap<String, Set<PerlBreakpoint>>();
    }

    public synchronized void add(PerlBreakpoint bp)
    {
        String path = canonPath(bp.getResourcePath());
        if (path == null) return;
        
        Set<PerlBreakpoint> set = breakpoints.get(path);
        if (set == null)
        {
            set = new HashSet<PerlBreakpoint>();
            breakpoints.put(path, set);
        }
        set.add(bp);
    }

    public synchronized boolean contains(PerlBreakpoint bp)
    {
        String path = canonPath(bp.getResourcePath());
        if (path == null) return false;

        Set<?> set = breakpoints.get(path);
        if (set == null) return false;

        return set.contains(bp);
    }

    public synchronized Set<PerlBreakpoint> getBreakpoints(IPath path)
    {
        String canonPath = canonPath(path);
        if (canonPath == null) return Collections.emptySet();
        
        Set<PerlBreakpoint> set = breakpoints.get(canonPath);
        return set != null ? new HashSet<PerlBreakpoint>(set) : Collections.<PerlBreakpoint>emptySet();
    }

    public synchronized PerlBreakpoint getBreakpoint(IPath path, int line)
    {
        String canonPath = canonPath(path);

        for (Iterator<PerlBreakpoint> i = getBreakpoints(path).iterator(); i.hasNext();)
        {
            PerlBreakpoint obj = i.next();
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
    
    public synchronized boolean remove(PerlLineBreakpoint bp, boolean enabled)
    	throws CoreException
    {
    	String path = canonPath(bp.getResourcePath());
    	
    	Set<PerlBreakpoint> set = breakpoints.get(path);
        if (set == null) return false;
        
        int lineNumber = bp.getLineNumber();
        
        for (Iterator<PerlBreakpoint> i = set.iterator(); i.hasNext();)
        {
        	PerlBreakpoint other = i.next();
        	if (!(other instanceof PerlLineBreakpoint)) continue;
        	
        	if (((PerlLineBreakpoint) other).getLineNumber() == lineNumber &&
        	    other.isEnabled() == enabled)
        	{
        		i.remove();
        		return true;
        	}
        }
        return false;
    }

    public synchronized boolean remove(PerlBreakpoint bp)
    {
        String path = canonPath(bp.getResourcePath());

        Set<PerlBreakpoint> set = breakpoints.get(path);
        if (set == null) return false;

        return set.remove(bp);
    }
    
    private synchronized String canonPath(IPath path)
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