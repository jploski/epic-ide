package org.epic.debug.db;

import java.io.IOException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlDebugThread;

public class StackFrame2 extends DebugElement implements IStackFrame
{
    private static final IRegisterGroup[] NO_REGISTER_GROUPS =
        new IRegisterGroup[0];
    
    private static final String DB_DUMP_LOCAL_VARS;    
    private static final String DB_DUMP_GLOBAL_VARS;

    private final PerlDebugThread thread;
    private final IPath path;
    private final IPath localPath;
    private final int lineNumber;
    private final DebuggerInterface db;
    private final StackFrame2 previous;
    private final int frameIndex; // 0 = top stack frame, 1 = one below...
    
    static
    {
        DB_DUMP_LOCAL_VARS = HelperScript.load("dump_local_vars2.pl");
        DB_DUMP_GLOBAL_VARS = HelperScript.load("dump_global_vars2.pl");
    }
    
    public StackFrame2(
        PerlDebugThread thread,
        IPath path,
        int lineNumber,
        IPath localPath,
        DebuggerInterface db,
        StackFrame2 previous,
        int frameIndex) throws DebugException
    {
        super(thread.getDebugTarget());

        this.thread = thread;
        this.path = path;
        this.localPath = localPath;
        this.lineNumber = lineNumber;
        this.db = db;
        this.previous = previous;
        this.frameIndex = frameIndex;
    }

    public boolean canResume()
    {
        return thread.canResume();
    }

    public boolean canStepInto()
    {
        return thread.canStepInto();
    }

    public boolean canStepOver()
    {
        return thread.canStepOver();
    }

    public boolean canStepReturn()
    {
        return thread.canStepReturn();
    }

    public boolean canSuspend()
    {
        return thread.canSuspend();
    }

    public boolean canTerminate()
    {
        return thread.canTerminate();
    }

    public int getCharEnd() throws DebugException
    {
        return -1;
    }

    public int getCharStart() throws DebugException
    {
        return -1;
    }
    
    public int getFrameIndex()
    {
        return frameIndex;
    }

    public int getLineNumber() throws DebugException
    {
        return lineNumber;
    }

    /**
     * @return path of the stack frame, valid in the file system
     *         of EPIC, or null if the path could not be resolved
     */
    public IPath getLocalPath()
    {
        return localPath;
    }

    public String getModelIdentifier()
    {
        return thread.getModelIdentifier();
    }

    public String getName() throws DebugException
    {
        return path.lastSegment() + "[line: " + Integer.toString(lineNumber) + "]";
    }

    /**
     * @return path of the stack frame, valid in the file system
     *         of "perl -d"; this path is not necessarily local to EPIC
     */
    public IPath getPath()
    {
        return path;
    }
    
    public PerlDebugThread getPerlThread()
    {
        return thread;
    }
    
    public IRegisterGroup[] getRegisterGroups() throws DebugException
    {
        return NO_REGISTER_GROUPS;
    }

    public IThread getThread()
    {
        return thread;
    }

    public IVariable[] getVariables() throws DebugException
    {
        if (db.isDisposed() || !db.isSuspended()) return new IVariable[0];
        
        try
        {
            List vars = new ArrayList();
            if (db.hasPadWalker()) dumpLocalVars(vars);
            return (IVariable[]) vars.toArray(new IVariable[vars.size()]);
        }
        catch (IOException e)
        {
            throw new DebugException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "An error occurred while retrieving variables from the debugger process",
                e));
        }
    }

    public boolean hasRegisterGroups() throws DebugException
    {
        return false;
    }

    public boolean hasVariables() throws DebugException
    {
        return true;
    }

    public boolean isStepping()
    {
        return thread.isStepping();
    }

    public boolean isSuspended()
    {
        return thread.isSuspended();
    }

    public boolean isTerminated()
    {
        return thread.isTerminated();
    }

    public void resume() throws DebugException
    {
        thread.resume();
    }

    public void stepInto() throws DebugException
    {
        thread.stepInto();
    }

    public void stepOver() throws DebugException
    {
        thread.stepOver();
    }

    public void stepReturn() throws DebugException
    {
        thread.stepReturn();
    }

    public void suspend() throws DebugException
    {
        thread.suspend();
    }

    public void terminate() throws DebugException
    {
        thread.terminate();
    }
    
    private void dumpLocalVars(List vars) throws IOException, DebugException
    {         
        String code = HelperScript.replace(
            DB_DUMP_LOCAL_VARS,
            "#SET_OFFSET#",
            "my $offset = " + frameIndex + ";");

        String localVarsString = db.eval(code);
        if (localVarsString == null) return;
        
        DumpedEntityReader r = new DumpedEntityReader(localVarsString);                
        while (r.hasMoreEntities())
        {
            vars.add(new LexicalVariable(
                db,
                this,
                r.nextEntity()));
        }
    }
}
