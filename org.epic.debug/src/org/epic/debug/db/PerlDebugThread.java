package org.epic.debug.db;

import java.io.IOException;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.eclipse.ui.progress.UIJob;
import org.epic.debug.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * This IThread implementation is responsible for processing user
 * interface events, coordinating communication with the Perl debugger
 * and dispatching debug events. The main role of PerlDebugThread lies
 * in delegating work to helper objects at appropriate times:
 * PerlThreadStack, PerlThreadBreakpoints and PerlDebugJob.
 * 
 * @author ruehl
 * @author jploski
 */
public class PerlDebugThread extends DebugElement implements IThread
{    
    private static final IBreakpoint[] NO_BREAKPOINTS = new IBreakpoint[0];
    
    private final static int INITIALIZING = 0;
    private final static int SUSPENDED = 1;
    private final static int STEPPING = 2;
    private final static int RUNNING = 3;
    private final static int TERMINATING = 4;
    private final static int TERMINATED = 5;

    private final Object LOCK = new Object();
    private final PerlThreadStack stack;
    private final PerlThreadBreakpoints bp;
    private final PerlDebugJob job;
    private final String name;
    private final DebuggerInterface db;

    private int state;

    public PerlDebugThread(
        IDebugTarget debugTarget,
        DebuggerInterface db) throws CoreException
    {
        super(debugTarget);

        this.db = db;
        this.stack = new PerlThreadStack(this);
        this.bp = new PerlThreadBreakpoints(this, db);
        this.state = INITIALIZING;
        this.name = "Main Thread";
        this.job = new PerlDebugJob();

        fireCreationEvent();

        suspended(DebugEvent.BREAKPOINT);
        
        if (!isBreakpointReached() &&
            !PerlEditorPlugin.getDefault().getBooleanPreference(
                PreferenceConstants.DEBUG_SUSPEND_AT_FIRST))
        {
            UIJob resume = new UIJob("PerlDebugThread.resume") {
                public IStatus runInUIThread(IProgressMonitor monitor)
                {
                    try { resume(); }
                    catch (DebugException e)
                    {
                        PerlDebugPlugin.log(e);
                    }
                    return Status.OK_STATUS;
                }
            };
            resume.setSystem(true);
            resume.schedule();
        }
        else fireSuspendEvent(DebugEvent.BREAKPOINT);
    }

    public boolean canResume()
    {
        return isSuspended();
    }

    public boolean canStepInto()
    {
        return isSuspended();
    }

    public boolean canStepOver()
    {
        return isSuspended();
    }

    public boolean canStepReturn()
    {
        return isSuspended();
    }

    public boolean canSuspend()
    {
        return false; // TODO support suspend on demand
    }

    public boolean canTerminate()
    {
        return !isTerminated();
    }
    
    public String evaluateStatement(String text)
        throws DebugException
    {
        try
        {
            String output = db.eval(text);        
            stack.update();
            return output;
        }
        catch (IOException e)
        {
            throwDebugException(e);
            return null;
        }
    }

    public Object getAdapter(Class adapter)
    {
        if (PerlDebugThread.class.isAssignableFrom(adapter))
        {
            return this;
        }
        if (IStackFrame.class.isAssignableFrom(adapter))
        {
            try
            {
                return getTopStackFrame();
            }
            catch (DebugException e)
            {
                // do nothing if not able to get frame
            }
        }
        return super.getAdapter(adapter);
    }

    public IBreakpoint[] getBreakpoints()
    {
        try
        {
            IBreakpoint breakpoint = bp.getCurrentBreakpoint();
            if (breakpoint != null) return new IBreakpoint[] { breakpoint };
            else return NO_BREAKPOINTS;
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return NO_BREAKPOINTS;
        }
    }

    public String getModelIdentifier()
    {
        return PerlDebugPlugin.getUniqueIdentifier();
    }

    public String getName() throws DebugException
    {
        if (isSuspended()) return "<suspended> " + name;
        else if (!isTerminated()) return "<running> " + name;
        else return name;
    }

    public int getPriority() throws DebugException
    {
        return 0;
    }

    public IStackFrame[] getStackFrames() throws DebugException
    {
        return stack.getFrames();
    }

    public IStackFrame getTopStackFrame() throws DebugException
    {
        IStackFrame[] frames = getStackFrames();
        return frames.length > 0 ? frames[0] : null;
    }

    public boolean hasStackFrames() throws DebugException
    {
        return stack.getFrames().length > 0;
    }

    public boolean isStepping()
    {
        synchronized (LOCK)
        {
            return state == STEPPING;
        }
    }

    public boolean isSuspended()
    {
        synchronized (LOCK)
        {
            return state == SUSPENDED;
        }
    }

    public boolean isTerminated()
    {
        synchronized (LOCK)
        {
            return state == TERMINATED;
        }
    }

    public void resume() throws DebugException
    {
        synchronized (LOCK)
        {
            assertSuspended();            
            state = RUNNING;
            fireResumeEvent(DebugEvent.UNSPECIFIED);
            job.setCommand(new ResumeCommand(this));
            job.schedule();
        }        
    }

    public void stepInto() throws DebugException
    {
        step(DebugEvent.STEP_INTO);
    }

    public void stepOver() throws DebugException
    {
        step(DebugEvent.STEP_OVER);
    }

    public void stepReturn() throws DebugException
    {
        step(DebugEvent.STEP_RETURN);
    }

    public void suspend() throws DebugException
    {
        // TODO support suspend on demand 
        throw new DebugException(
            new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                "Suspend not supported",
                null
                ));
    }

    public void terminate() throws DebugException
    {
        synchronized (LOCK)
        {
            if (isSuspended())
            {
                state = TERMINATING;
                job.setCommand(new TerminateCommand(this));
                job.schedule();
            }
            else
            {
                getDebugTarget().getProcess().terminate();
            }
        }
    }
    
    /**
     * Invoked by DebugCommand on PerlDebugJob's thread to indicate
     * that the command has finished - either because the debugger
     * has suspended or terminated.
     */
    void debugCommandFinished(DebugCommand cmd)
    {
        synchronized (LOCK)
        {
            if (state == TERMINATED) return; // terminated while not suspended
        }
        
        if (cmd.hasSuspended())
        {
            suspended(cmd.getCompletionStatus());
        }
        else // terminated
        {
            terminated();
        }
    }
    
    /**
     * Only for use by DebugCommand. 
     */
    DebuggerInterface getDB()
    {
        return db;
    }
    
    IPath getDebuggerPath(IPath epicPath)
    {
        return ((DebugTarget) getDebugTarget()).getPathMapper()
            .getDebuggerPath(epicPath, db);
    }
    
    IPath getEpicPath(IPath dbPath)
    {
        if (!dbPath.isAbsolute())
        {
            try { dbPath = bp.getAbsDBPath(dbPath); }
            catch (CoreException e) { PerlDebugPlugin.log(e); }
        }
        
        return ((DebugTarget) getDebugTarget()).getPathMapper()
            .getEpicPath(dbPath);
    }
    
    void installPendingBreakpoints()
    {
        try
        {
            bp.installPendingBreakpoints();
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
        }
    }
    
    boolean isBreakpointReached() throws CoreException
    {
        return bp.getCurrentBreakpoint() != null;
    }
    
    private void assertSuspended() throws DebugException
    {
        if (!isSuspended()) throw new DebugException(
            new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                "This action only works when the debugger is suspended",
                null
                ));
    }
    
    private void step(int kind) throws DebugException
    {
        synchronized (LOCK)
        {
            assertSuspended();            
            state = STEPPING;
            fireResumeEvent(kind);
            
            DebugCommand cmd;
            switch (kind)
            {
            case DebugEvent.STEP_INTO:
                cmd = new StepIntoCommand(this);
                break;
            case DebugEvent.STEP_OVER:
                cmd = new StepOverCommand(this);
                break;
            case DebugEvent.STEP_RETURN:
                cmd = new StepReturnCommand(this);
                break;
            default:
                assert false : "unrecognized step kind";
                return;
            }
            job.setCommand(cmd);
            job.schedule();
        }
    }
    
    private void suspended(int kind)
    {
        try
        {            
            stack.update();
            bp.installPendingBreakpoints();
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
        }
        synchronized (LOCK)
        {
            // Firing a SUSPENDED event causes the workbench to grab
            // focus or pop-up to the front of the window stack.
            // PerlDebugThread calls suspended to complete its
            // initialization, but it might resume right after that,
            // so we don't issue a SUSPENDED event in this case.

            boolean wasInitializing = state == INITIALIZING;
            state = SUSPENDED;
            if (!wasInitializing) fireSuspendEvent(kind);
        }
    }
    
    private void terminated()
    {
        synchronized (LOCK)
        {            
            db.dispose();
            bp.dispose();
            state = TERMINATED;
            fireTerminateEvent();
        }
    }
    
    void throwDebugException(IOException e) throws DebugException
    {
        throw new DebugException(new Status(
            IStatus.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            "An error occurred during communication with the debugger process",
            e));
    }
    
    void unresolvedDebuggerPath(IPath dbPath)
    {
        PerlDebugPlugin.log(new Status(
            IStatus.WARNING,
            PerlDebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            "Could not map remote path " + dbPath +
            " to a local path. Some breakpoints may be ignored.",
            null));
    }
    
    void unresolvedEpicPath(IPath epicPath)
    {
        PerlDebugPlugin.log(new Status(
            IStatus.WARNING,
            PerlDebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            "Could not map local path " + epicPath +
            " to a remote path. Some breakpoints may be ignored.",
            null));
    }
}