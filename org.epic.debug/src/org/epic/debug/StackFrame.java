package org.epic.debug;

import java.util.*;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.ui.action.*;
import org.epic.debug.varparser.PerlDebugValue;
import org.epic.debug.varparser.PerlDebugVar;

/**
 * @author ruehl
 */
public class StackFrame extends DebugElement implements IStackFrame
{
    private static final IRegisterGroup[] NO_REGISTER_GROUPS =
        new IRegisterGroup[0];
    
    private static final Set PERL_INTERNAL_VARS = initInternalVars();
    
    private final PerlDebugThread thread;
    private final IPath path;
    private final int lineNumber;
    
    private PerlDebugVar[] actualVars;
    private PerlDebugVar[] displayedVars;

    /**
     * Creates a top-level stack frame.
     * Information about variables is currently available only for this frame.
     */
    public StackFrame(
        PerlDebugThread thread,
        IPath path,
        int lineNumber,
        PerlDebugVar[] vars) throws DebugException
    {
        super(thread.getDebugTarget());

        this.thread = thread;
        this.path = path;
        this.lineNumber = lineNumber;
        this.actualVars = vars;
        computeDisplayedVars();
    }
    
    /**
     * Creates a non-top-level stack frame.
     * These frames currently do not provide information about variables.
     */
    public StackFrame(
        PerlDebugThread thread,
        IPath path,
        int lineNumber,
        String returnType,
        String calledSub) throws DebugException
    {
        this(thread, path, lineNumber, new PerlDebugVar[0]);
        
        if (returnType.equals(".")) returnType = "void";
        else if (returnType.equals("@")) returnType = "list";
        else if (returnType.equals("$")) returnType = "scalar";
        
        displayedVars = new PerlDebugVar[2];
        displayedVars[0] = new PerlDebugVar(
            getDebugTarget(),
            PerlDebugVar.GLOBAL_SCOPE,
            "Called Function",
            new PerlDebugValue(getDebugTarget(), null, calledSub));
    
        displayedVars[1] = new PerlDebugVar(
            getDebugTarget(),
            PerlDebugVar.GLOBAL_SCOPE,
            "Return Type",
            new PerlDebugValue(getDebugTarget(), null, returnType));
    
        displayedVars[0].setSpecial();
        displayedVars[1].setSpecial();
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

    public void computeDisplayedVars() throws DebugException
    {
        if (!isTopFrame()) return;
        
        List displayed = new ArrayList(actualVars.length);
    
        for (int i = 0; i < actualVars.length; i++)
            if (isDisplayedVar(actualVars[i]))
                displayed.add(actualVars[i]);
    
        displayedVars = ((PerlDebugVar[])
            displayed.toArray(new PerlDebugVar[displayed.size()]));
    }

    public int getCharEnd() throws DebugException
    {
        return -1;
    }

    public int getCharStart() throws DebugException
    {
        return -1;
    }

    public IDebugTarget getDebugTarget()
    {
        return thread.getDebugTarget();
    }

    public IPath getPath()
    {
        return path;
    }

    public ILaunch getLaunch()
    {
        return thread.getLaunch();
    }

    public int getLineNumber() throws DebugException
    {
        return lineNumber;
    }

    public String getModelIdentifier()
    {
        return PerlDebugPlugin.getUniqueIdentifier();
    }

    public String getName() throws DebugException
    {
        return path.lastSegment() + "[line: " + Integer.toString(lineNumber) + "]";
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
        return displayedVars;
    }

    public boolean hasRegisterGroups() throws DebugException
    {
        return false;
    }

    public boolean hasVariables() throws DebugException
    {
        return displayedVars != null && displayedVars.length > 0;
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

    public String toString()
    {
        return path + ":" + lineNumber;
    }
    
    private static Set initInternalVars()
    {
        ResourceBundle rb = ResourceBundle.getBundle("org.epic.debug.perlIntVars");
        Enumeration e = rb.getKeys();
        Set vars = new HashSet();
        while (e.hasMoreElements()) vars.add(e.nextElement());
        return vars;
    }
    
    private boolean isDisplayedVar(PerlDebugVar var) throws DebugException
    {
        if (var.isGlobalScope())
        {
            boolean isPerlInternal =
                PERL_INTERNAL_VARS.contains(var.getName()); 
            
            if (isPerlInternal &&
                !ShowPerlInternalVariableActionDelegate.getPreferenceValue())    
            {
                return false;
            }
            if (!isPerlInternal &&
                !ShowGlobalVariableActionDelegate.getPreferenceValue())
            {
                return false;
            }
        }
        else if (!ShowLocalVariableActionDelegate.getPreferenceValue())
        {
            return false;
        }
        return true;
    }
    
    private boolean isTopFrame()
    {
        return actualVars.length > 0;
    }
}