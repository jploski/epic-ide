package org.epic.debug.db;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IBreakpoint;
import org.epic.debug.*;

/**
 * A helper class for PerlDebugThread. This class is responsible for
 * managing breakpoints. In particular, this includes taking care of
 * breakpoints which are set in the user interface while the debugger
 * is not suspended (the real Perl debugger only set breakpoints when
 * suspended). Another important issue is path translation - breakpoints
 * are on local IResources but the paths communicated to the debugger
 * must match the remote file system.
 * 
 * This class uses epic_breakpoints.pm on the Perl side (see also
 * documentation of epic_breakpoints.pm).
 * 
 * @author jploski
 */
class PerlThreadBreakpoints
{
    private final IBreakpointListener listener = new IBreakpointListener() {
        public void breakpointAdded(IBreakpoint breakpoint)
        {
            if (!(breakpoint instanceof PerlLineBreakpoint)) return;

            PerlLineBreakpoint bp = (PerlLineBreakpoint) breakpoint;

            try { if (!bp.isEnabled()) return; }
            catch (CoreException e)
            {
                PerlDebugPlugin.log(e);
                return;
            }
            if (thread.isSuspended()) addBreakpoint(bp);
            else
            {
            	try
            	{
            		// If we had a pending remove for this breakpoint,
            		// it is cancelled by this newer add request.
            		// Otherwise, we have to create a pending add.
            		
            		if (pendingBreakpoints.remove(bp, false))
            			activeBreakpoints.add(bp);
            		else
            			pendingBreakpoints.add(bp);
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

        public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta)
        {
            if (!(breakpoint instanceof PerlLineBreakpoint)) return;

            PerlLineBreakpoint bp = (PerlLineBreakpoint) breakpoint;
            if (thread.isSuspended()) removeBreakpoint(bp);
            else
            {
            	try
            	{
            		// If we had a pending add for this breakpoint,
            		// it is cancelled by this newer remove request.
            		// Otherwise, we have to create a pending remove.
            		
            		if (pendingBreakpoints.remove(bp, true))
            		{
            			// nothing to do
            		}
            		else
            		{
                    	activeBreakpoints.remove(bp);
                    	pendingBreakpoints.add(bp);
            			bp.pendingRemove();
            		}
            	}
                catch (CoreException e)
                {
                    PerlDebugPlugin.log(e);
                }
            }
        }
    };
    
    private static final String DB_ADD_BREAK =
        ";{\n#PARAMS#\nepic_breakpoints::add_breakpoint($file, $line, $cond);\n};";

    private static final String DB_GET_ABS_PATH =
        ";{\n#PARAMS#\nepic_breakpoints::get_abs_path($file);\n};";
    
    private static final String DB_REMOVE_BREAK =
        ";{\n#PARAMS#\nepic_breakpoints::remove_breakpoint($file, $line);\n};";
    
    private final PerlDebugThread thread;
    private final DebuggerInterface db;

    /**
     * All breakpoints that have been effectively set in the debugger.
     */
    private final BreakpointMap activeBreakpoints;
    
    /**
     * Breakpoints that have been set/unser in the GUI while the debugger
     * was not suspended. We wait until next suspend to insert them.
     */
    private final BreakpointMap pendingBreakpoints;
    
    public PerlThreadBreakpoints(
        PerlDebugThread thread,
        DebuggerInterface db) throws DebugException
    {
        this.thread = thread;
        this.db = db;

        pendingBreakpoints = new BreakpointMap();
        activeBreakpoints = new BreakpointMap();

        installInitialBreakpoints();
        
        DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(listener);
    }
    
    public void dispose()
    {
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(listener);
    }
    
    public IPath getAbsDBPath(IPath relativeDBPath) throws CoreException
    {
        // This method is necessary to resolve relative paths that may
        // be returned by IPPosition.getPath(). Such paths are returned
        // if the script contains something like "use lib 'foo';"
        // We resolve them through epic_breakpoints.pm, hoping for
        // the best...
        
        try
        {
            String code = HelperScript.replace(
                DB_GET_ABS_PATH,
                "#PARAMS#",
                "my $file = <<'EOT';\n" + relativeDBPath + "\nEOT\n");                
            
            return new Path(db.eval(code).trim());
        }
        catch (IOException e)
        {
            thread.throwDebugException(e);
            return null;
        }
    }
    
    public IBreakpoint getCurrentBreakpoint() throws CoreException
    {
        try
        {
            // ensure that activeBreakpoints are up-to-date:
            IPPosition pos = installPendingBreakpoints();
            if (pos == null) return null;
            
            IPath epicPath = thread.getEpicPath(pos.getPath());
            if (epicPath == null)
            {
                thread.unresolvedDebuggerPath(pos.getPath());
                return null;
            }
        
            // XXX: this breaks if new breakpoint types are installed!
            return (PerlLineBreakpoint)
                activeBreakpoints.getBreakpoint(epicPath, pos.getLine());
        }
        catch (CoreException e)
        {
            if (e.getCause() instanceof DebuggerInterface.SessionTerminatedException)
            {            
                return null;
            }
            else
            {
                throw e;
            }
        }
        // TODO: reimplement debugging reg-exps here see ToggleBreakpointAdapter)
    }
    
    public IPPosition installPendingBreakpoints() throws CoreException
    {
        try
        {
            IPPosition pos = db.getCurrentIP();            
            IPath epicPath = thread.getEpicPath(pos.getPath());
            if (epicPath == null)
            {
                thread.unresolvedDebuggerPath(pos.getPath());
                return pos;
            }
            
            Set<PerlBreakpoint> bps = pendingBreakpoints.getBreakpoints(epicPath);
            if (bps.isEmpty()) return pos;
        
            for (Iterator<PerlBreakpoint> i = bps.iterator(); i.hasNext();)
            {
                PerlBreakpoint bp = i.next();
                if (bp.isEnabled()) addBreakpoint(bp);
                else removeBreakpoint(bp);
            }
            //bps.clear();
            return pos;
        }
        catch (IOException e)
        {
            thread.throwDebugException(e);
            return null;
        }
    }
    
    private boolean addBreakpoint(PerlBreakpoint bp)
    {
        if (!(bp instanceof PerlLineBreakpoint)) return false;
        //assert thread.isSuspended();    
        
        IPath dbPath = thread.getDebuggerPath(bp.getResourcePath());
        if (dbPath == null)
        {
            thread.unresolvedEpicPath(bp.getResourcePath());
            return false;
        }
    
        try
        {
            PerlLineBreakpoint lbp = (PerlLineBreakpoint) bp;
    
            String code = HelperScript.replace(
                DB_ADD_BREAK,
                "#PARAMS#",
                "my $file = <<'EOT';\n" + dbPath + "\nEOT\n" +
                "my $line = <<'EOT';\n" + lbp.getLineNumber() + "\nEOT\n" +
                getConditionParam(lbp));                
            
            db.eval(code);
            // TODO: what about bp.setInvalidBreakpointPosition(true);
            pendingBreakpoints.remove(lbp);
            activeBreakpoints.add(lbp);
            return true;
        }
        catch (Exception e)
        {
            PerlDebugPlugin.log(e);
            return false;
        }
    }
    
    private String getConditionParam(PerlLineBreakpoint bp)
        throws CoreException
    {
        if (!bp.isConditionEnabled()) return "my $cond = '';\n";
        
        String condition = bp.getCondition();
        condition = condition.replaceAll("\\n\\r", " ");
        condition = condition.replaceAll("\\r", " ");
        condition = condition.replaceAll("\\n", " ");
        
        return "my $cond = <<'EOT';\n" + condition + "\nEOT\n"; 
    }

    private void installInitialBreakpoints()
    {    
        IBreakpoint[] bps = DebugPlugin.getDefault().getBreakpointManager()
            .getBreakpoints(PerlDebugPlugin.getUniqueIdentifier());

        for (int i = 0; i < bps.length; i++)
        {
            PerlBreakpoint bp = (PerlBreakpoint) bps[i];
            
            try
            {
                if (bp.isEnabled()) addBreakpoint(bp);
            }
            catch (Exception e)
            {
                PerlDebugPlugin.log(e);
            }
        }
    }
    
    private boolean removeBreakpoint(PerlBreakpoint bp)
    {
        if (!(bp instanceof PerlLineBreakpoint)) return false;
        //assert thread.isSuspended();

        IPath dbPath = thread.getDebuggerPath(bp.getResourcePath());
        if (dbPath == null)
        {
            thread.unresolvedEpicPath(bp.getResourcePath());
            return false;
        }
        
        try
        {
            PerlLineBreakpoint lbp = (PerlLineBreakpoint) bp;

            String code = HelperScript.replace(
                DB_REMOVE_BREAK,
                "#PARAMS#",
                "my $file = <<'EOT';\n" + dbPath + "\nEOT\n" +
                "my $line = <<'EOT';\n" + lbp.getLineNumber() + "\nEOT\n");
            
            db.eval(code);
            pendingBreakpoints.remove(lbp);
            activeBreakpoints.remove(lbp);
            return true;
        }
        catch (Exception e)
        {
            PerlDebugPlugin.log(e);
            return false;
        }
    }
}
