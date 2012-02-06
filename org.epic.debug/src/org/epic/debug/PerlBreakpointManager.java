package org.epic.debug;

import java.util.*;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.epic.debug.db.PerlDB;

/**
 * @author ruehl
 */
public class PerlBreakpointManager implements IBreakpointListener
{
    private final Set debuggers;

    public PerlBreakpointManager()
    {
        debuggers = new HashSet();
        DebugPlugin.getDefault().getBreakpointManager()
            .addBreakpointListener(this);
    }
    
    public void addDebugger(PerlDB db)
    {
        synchronized (debuggers)
        {
            debuggers.add(db);
        }

        IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager()
            .getBreakpoints(PerlDebugPlugin.getUniqueIdentifier());

        for (int i = 0; i < bps.length; i++)
        {
            PerlBreakpoint bp = (PerlBreakpoint) bps[i];
            
            try
            {
                if (bp.isEnabled())
                {
                    db.addBreakpoint(bp);
                    bp.addDebugger(db);
                }
            }
            catch (CoreException e)
            {
                PerlDebugPlugin.log(e);
            }
        }
    }

    public synchronized void breakpointAdded(IBreakpoint fBreakpoint)
    {
        if (!(fBreakpoint instanceof PerlBreakpoint)) return;

        PerlBreakpoint bp = (PerlBreakpoint) fBreakpoint;

        try
        {
            if (!bp.isEnabled()) return;
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return;
        }

        synchronized (debuggers)
        {        
            for (Iterator i = debuggers.iterator(); i.hasNext();)
            {
                PerlDB db = (PerlDB) i.next();
                try
                {
                    db.addBreakpoint(bp);
                    bp.addDebugger(db);
                }
                catch (CoreException e)
                {
                    PerlDebugPlugin.log(e);
                }
            }
        }
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta)
    {
        if (!(breakpoint instanceof PerlBreakpoint)) return;

        PerlBreakpoint bp = (PerlBreakpoint) breakpoint;
        PerlDB[] debuggers = bp.getDebuggers();

        for (int i = 0; i < debuggers.length; i++)
        {
            PerlDB db = debuggers[i];
            try
            {
                db.removeBreakpoint(bp);
                bp.removeDebugger(db);
            }
            catch (CoreException e)
            {
                PerlDebugPlugin.log(e);
            }
        }
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta)
    {
        if (!(breakpoint instanceof PerlLineBreakpoint)) return;

        boolean enabledOld = ((Boolean) delta
            .getAttribute(IBreakpoint.ENABLED)).booleanValue();
        boolean enabledNew = false;
        try
        {
            enabledNew = ((Boolean) breakpoint.getMarker().getAttribute(
                IBreakpoint.ENABLED)).booleanValue();
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return;
        }
        if (enabledOld != enabledNew)
        {
            if (enabledNew) this.breakpointAdded(breakpoint);
            else this.breakpointRemoved(breakpoint, delta);
        }
    }

    public void dispose()
    {
        DebugPlugin.getDefault().getBreakpointManager()
            .removeBreakpointListener(this);
    }

    public Set getDebuggers()
    {
        return Collections.unmodifiableSet(debuggers);
    }

    public void removeDebugger(PerlDB db)
    {
        synchronized (debuggers)
        {
            debuggers.remove(db);
        }

        IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager()
            .getBreakpoints(PerlDebugPlugin.getUniqueIdentifier());

        for (int i = 0; i < bps.length; i++)
            ((PerlBreakpoint) bps[i]).removeDebugger(db);
    }
}
