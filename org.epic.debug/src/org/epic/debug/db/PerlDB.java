package org.epic.debug.db;

import gnu.regexp.REMatch;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.epic.debug.*;
import org.epic.debug.db.DebuggerInterface.Command;
import org.epic.debug.ui.action.ShowLocalVariableActionDelegate;
import org.epic.debug.util.DebuggerProxy2;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author ruehl
 */
public class PerlDB implements IDebugElement
{
    private static boolean canDisplayLocalVars = true;
    
    private final RE re = new RE();

    private final PerlDebugThread thread;

    // Working directory of the "perl -d" process
    private final IPath workingDir;

    private final DebuggerInterface db;
    private final BreakpointMap activeBreakpoints;
    private final BreakpointMap pendingBreakpoints;    

    private IPPosition startIP;
    private boolean terminated;
    private DebugTarget target;
    private DebuggerInterface.Command currentCommand;
    
    private final DebuggerInterface.IListener commandListener =
        new DebuggerInterface.IListener() {
        
        public void commandFinished(Command cmd)
        {
            PerlDB.this.commandFinished(cmd);
        }
        
        public void sessionTerminated()
        {
            PerlDB.this.sessionTerminated();
        } };        

    public PerlDB(DebugTarget target) throws CoreException
    {
        this.target = target;        

        workingDir = target.getLocalWorkingDir();        

        pendingBreakpoints = new BreakpointMap();
        activeBreakpoints = new BreakpointMap();

        thread = new PerlDebugThread("Main Thread", target.getLaunch(), target, this);

        BufferedReader in = target.getDebugReadStream();
        PrintWriter out = target.getDebugWriteStream();

        if (PerlEditorPlugin.getDefault().getDebugConsolePreference())
        {
            DebuggerProxy2 p = new DebuggerProxy2(in, out, getLaunch());
            getLaunch().addProcess(p);
            in = p.getDebugIn();
            out = p.getDebugOut();
        }
        
        db = createDebuggerInterface(in, out);       
    }
    
    public void init(
        final String ioHost,
        final int ioPort,
        final int errorPort) throws DebugException
    {
        if (db == null) return;
        
        // This method is separate from the constructor because
        // callbacks to our DebugTarget may occur during its execution.
        // By having it execute after the constructor, we ensure that
        // DebugTarget.perlDB has been assigned before any such callbacks.
        
        if (ioHost != null) // relevant for remote debugging only
        {
            redirectIO(ioHost, ioPort);
            redirectError(ioHost, errorPort);
        }

        PerlDebugPlugin.getPerlBreakPointManager().addDebugger(this);
        target.perlDBstarted(this);
        updateStackFrames();        
        
        if (isBreakPointReached())
            fireDebugEvent(DebugEvent.SUSPEND, DebugEvent.BREAKPOINT);
        else
        {
            fireDebugEvent(DebugEvent.SUSPEND, DebugEvent.STEP_END);
            if (!PerlEditorPlugin.getDefault().getSuspendAtFirstPreference())
                resume(thread);
        }
    }

    public boolean addBreakpoint(PerlBreakpoint bp)
        throws CoreException
    {
        return addBreakpoint(bp, false);
    }

    public boolean addBreakpoint(PerlBreakpoint bp, boolean isPending)
        throws CoreException
    {
        try
        {
            boolean isValid = setBreakpoint(bp, isPending);
            if (!isValid) bp.setInvalidBreakpointPosition(true);
            return isValid;
        }
        catch (IOException e)
        {
            throwDebugException(e);
            return false;
        }
    }

    public boolean canResume(IDebugElement dest)
    {
        return isSuspended(dest);
    }

    public boolean canStepInto(IDebugElement dest)
    {
        return isSuspended(dest);
    }

    public boolean canStepOver(IDebugElement dest)
    {
        return isSuspended(dest);
    }

    public boolean canStepReturn(IDebugElement dest)
    {
        return isSuspended(dest);
    }

    public boolean canSuspend(IDebugElement dest)
    {
        return false;
    }

    public boolean canTerminate()
    {
        return canTerminate(null);
    }

    public boolean canTerminate(IDebugElement dest)
    {
        return !isTerminated();
    }

    public String evaluateStatement(String text)
        throws CoreException
    {
        return evaluateStatement(text, true);
    }

    public String evaluateStatement(String text, boolean updateVars)
        throws CoreException
    {
        try
        {
            String output = db.eval(text);        
            if (updateVars) updateStackFrames();
            return output;
        }
        catch (IOException e) { throwDebugException(e); return null; }
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == this.getClass()) return this;
        else return null;
    }

    public IDebugTarget getDebugTarget()
    {
        return target;
    }

    public ILaunch getLaunch()
    {
        return target.getLaunch();
    }

    public String getModelIdentifier()
    {
        return target.getModelIdentifier();
    }

    public void getRemoteInc(List out) throws CoreException
    {
        try
        {
            String output = db.eval(
                ";{foreach $t(@INC) {print $DB::OUT $t.\"\\n\";}}");
        
            StringTokenizer s = new StringTokenizer(output, "\r\n");            
            while (s.hasMoreTokens()) out.add(s.nextToken());
        }
        catch (IOException e) { throwDebugException(e); }
    }

    public IThread[] getThreads()
    {
        return new IThread[] { thread };
    }

    public boolean isStepping(IDebugElement dest)
    {
        return
            !terminated &&
            !db.isSuspended() &&
            currentCommand != null &&
            currentCommand.isStepCommand();
    }

    public boolean isSuspended(IDebugElement dest)
    {
        return !terminated && currentCommand == null;
    }

    public boolean isTerminated()
    {
        return isTerminated(null);
    }

    public boolean isTerminated(IDebugElement dest)
    {
        return terminated;
    }

    public void redirectError(String host, int port) throws DebugException
    {
        try
        {
            db.redirectError(host, port);
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }

    public void redirectIO(String host, int port) throws DebugException
    {
        try
        {
            db.redirectIO(host, port);
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }

    public void removeBreakpoint(PerlBreakpoint bp) throws CoreException
    {
        if (pendingBreakpoints.remove(bp)) return;
        if (!(bp instanceof PerlLineBreakpoint)) return;

        try
        {
            db.switchToFile(bp.getResourcePath());
            db.removeLineBreakpoint(
                ((PerlLineBreakpoint) bp).getLineNumber());
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }

    public void resume(IDebugElement dest) throws DebugException
    {
        try
        {
            fireDebugEvent(DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST);
            startIP = db.getCurrentIP();
            currentCommand = db.asyncResume();
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }

    public void shutdown()
    {
        abortSession();
    }

    public void stepInto(IDebugElement dest) throws DebugException
    {
        try
        {
            fireDebugEvent(DebugEvent.RESUME, DebugEvent.STEP_INTO);
            startIP = db.getCurrentIP();
            currentCommand = db.asyncStepInto();
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }

    public void stepOver(IDebugElement dest) throws DebugException
    {
        try
        {
            fireDebugEvent(DebugEvent.RESUME, DebugEvent.STEP_OVER);
            startIP = db.getCurrentIP();
            currentCommand = db.asyncStepOver();
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }

    public void stepReturn(IDebugElement dest) throws DebugException
    {
        try
        {
            fireDebugEvent(DebugEvent.RESUME, DebugEvent.STEP_RETURN);
            startIP = db.getCurrentIP();
            currentCommand = db.asyncStepReturn();
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }

    public void suspend(IDebugElement dest)
    {
        // TODO implement it
    }

    public void terminate()
    {
        abortSession();
    }

    public static void updateVariablesView()
    {
        if (ShowLocalVariableActionDelegate.getPreferenceValue() &&
            !canDisplayLocalVars)
        {
            PerlDebugPlugin.errorDialog(
                "Error displaying Local Variables\n" +
                "Install PadWalker on your Perl system or disable " +
                "displaying of local variables");
        }
    
        Set debuggers = PerlDebugPlugin.getPerlBreakPointManager().getDebuggers();
        Iterator iterator = debuggers.iterator();

        while (iterator.hasNext())
        {
            PerlDB db = (PerlDB) iterator.next();
            try
            {
                ((StackFrame) db.thread.getStackFrames()[0]).computeDisplayedVars();
            }
            catch (DebugException e)
            {
                PerlDebugPlugin.log(e);
            }
            db.fireDebugEvent(DebugEvent.EVALUATION, DebugEvent.UNSPECIFIED);
        }
    }

    private void abortSession()
    {
        if (!terminated)
        {
            terminated = true;
            if (db != null) db.dispose(); // db == null when createDebuggerInterface fails
            PerlDebugPlugin.getPerlBreakPointManager().removeDebugger(this);
            target.debugSessionTerminated();
        }
    }
    
    private void commandFinished(Command cmd)
    {
        try
        {
            currentCommand = null;
            if (isTerminated()) return;
    
            IPPosition endIP = maybeSkipStringEval(cmd);            
            
            if (startIP != null && endIP.getPath().equals(startIP.getPath()))
                insertPendingBreakpoints();
            
            updateStackFrames();
            
            switch (cmd.getType())
            {
            case DebuggerInterface.CMD_STEP_INTO:            
            case DebuggerInterface.CMD_STEP_OVER:
            case DebuggerInterface.CMD_STEP_RETURN:
                fireDebugEvent(DebugEvent.SUSPEND,
                    isBreakPointReached()
                    ? DebugEvent.BREAKPOINT
                    : DebugEvent.STEP_END);
                break;
            case DebuggerInterface.CMD_RESUME:
                fireDebugEvent(DebugEvent.SUSPEND, DebugEvent.BREAKPOINT);
                break;        
            }
        }
        catch (IOException e)
        {
            try { throwDebugException(e); }
            catch (CoreException _e) { PerlDebugPlugin.log(_e); }
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
        }
    }
    
    private DebuggerInterface createDebuggerInterface(
        BufferedReader in, PrintWriter out) throws DebugException
    {
        try
        {
            return new DebuggerInterface(
                in,
                out,
                workingDir,
                target.getPathMapper(),
                commandListener);
        }
        catch (IOException e)
        {
            fireDebugEvent(DebugEvent.TERMINATE, DebugEvent.UNSPECIFIED);
            terminated = true;
            
            // Probably an @INC problem at this point;
            // let's not complain too loudly
            //throwDebugException(e);
            return null;
        }
    }

    private void fireDebugEvent(int kind, int detail)
    {
        DebugEvent event = new DebugEvent(thread, kind, detail);
        DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] { event });
    }

    private boolean insertPendingBreakpoints() throws CoreException
    {
        try
        {
            IPPosition pos = db.getCurrentIP();
            Set bps = pendingBreakpoints.getBreakpoints(pos.getPath());
            if (bps.isEmpty()) return false;
        
            for (Iterator i = bps.iterator(); i.hasNext();)
            {
                PerlBreakpoint bp = ((PerlBreakpoint) i.next());
                if (!addBreakpoint(bp, true))
                    bp.setInvalidBreakpointPosition(true);
            }
        
            bps.clear();
        
            return true;
        }
        catch (IOException e)
        {
            throwDebugException(e);
            return false;
        }
    }

    private boolean isBreakPointReached() throws DebugException
    {
        try
        {
            IPPosition pos = db.getCurrentIP();
            if (pos == null) return false;
        
            // XXX: this breaks if new breakpoint types are installed!
            PerlLineBreakpoint bp = (PerlLineBreakpoint)
                activeBreakpoints.getBreakpoint(
                    pos.getPath(), pos.getLine());
    
            return bp != null;
        }
        catch (IOException e)
        {
            throwDebugException(e);
            return false;
        }
        // TODO: reimplement debugging reg-exps here see ToggleBreakpointAdapter)
    }

    private IPPosition maybeSkipStringEval(Command cmd) throws IOException
    {
        // We currently do not support stepping into variable eval
        // expressions, e.g. eval $str;
        // We detect an attempt to step into such an expression by
        // comparing the IP before and after the current step command.
        // If they are both equal, we step out of the eval expression
        // automatically to skip over it
        
        IPPosition endIP = db.getCurrentIP();            
        if (cmd.isStepCommand())
        {
            while (!isTerminated() && endIP.equals(startIP))
            {
                db.stepOver();
                endIP = db.getCurrentIP();
            }
        }
        return endIP;
    }



    private void sessionTerminated()
    {
        terminate();    
    }

    private boolean setBreakpoint(PerlBreakpoint bp, boolean isPending)
        throws IOException
    {
        boolean success;
    
        if (!isPending)
        {
            success = db.switchToFile(bp.getResourcePath());
            if (!success)
            {
                pendingBreakpoints.add(bp);
                db.setLoadBreakpoint(bp.getResourcePath());
                return true;
            }
        }
    
        if (!(bp instanceof PerlLineBreakpoint)) return false;
        
        try
        {
            if (db.setLineBreakpoint(((PerlLineBreakpoint) bp).getLineNumber()))
            {
                activeBreakpoints.add(bp);
                bp.addInstallation(this);
                return true;
            }
            else return false;
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return false;
        }
    }

    private void throwDebugException(IOException e) throws DebugException
    {
        throw new DebugException(new Status(
            IStatus.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            "An error occurred during communication with the debugger process",
            e));
    }

    private void updateStackFrames() throws DebugException
    {
        try
        {
            IPPosition currentIP = db.getCurrentIP();
            if (currentIP == null) return; // debugger terminated?
            
            String stackTrace = db.getStackTrace();
            REMatch[] matches = re.STACK_TRACE.getAllMatches(stackTrace);
            
            IStackFrame[] previousFrames = thread.getStackFrames();
            StackFrame previousTopFrame =
                previousFrames != null
                ? (StackFrame) previousFrames[0]
                : null;
            
            StackFrame[] frames = new StackFrame[matches.length + 1];
            frames[0] = new StackFrame(
                thread,
                currentIP.getPath(),
                currentIP.getLine(),
                db,
                previousTopFrame);
    
            for (int pos = 0; pos < matches.length; ++pos)
            {
                frames[pos + 1] = new StackFrame(
                    thread,
                    db.getPathFor(matches[pos].toString(3)),
                    Integer.parseInt(matches[pos].toString(4)),
                    matches[pos].toString(1),
                    matches[pos].toString(2));
            }
            
            thread.setStackFrames(frames);
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }
}