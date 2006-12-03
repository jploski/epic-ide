package org.epic.debug.db;

import java.io.IOException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlDebugThread;
import org.epic.debug.ui.action.*;
import org.epic.debug.varparser.*;
import org.epic.debug.varparser.PerlDebugValue;
import org.epic.debug.varparser.PerlDebugVar;

/**
 * @author ruehl
 */
public class StackFrame extends DebugElement implements IStackFrame
{
    private static final String PADWALKER_ERROR =
        "PadWalker module not found - please install";

    private static final String DB_DUMP_LOCAL_VARS;    
    private static final String DB_DUMP_GLOBAL_VARS;
    
    private static final IRegisterGroup[] NO_REGISTER_GROUPS =
        new IRegisterGroup[0];
    
    private static final Set PERL_INTERNAL_VARS = initInternalVars();
    
    private final PerlDebugThread thread;
    private final IPath path;
    private final int lineNumber;
    private final boolean topFrame;
    private final DebuggerInterface db;

    private StackFrame previous;     
    private PerlDebugVar[] actualVars;
    private PerlDebugVar[] displayedVars;
    
    static
    {
        DB_DUMP_LOCAL_VARS = loadHelperScript("dump_local_vars.pl");
        DB_DUMP_GLOBAL_VARS = loadHelperScript("dump_global_vars.pl");
    }

    /**
     * Creates a top-level stack frame.
     * Information about variables is currently available only for this frame.
     */
    public StackFrame(
        PerlDebugThread thread,
        IPath path,
        int lineNumber,
        DebuggerInterface db,
        StackFrame previous) throws DebugException
    {
        super(thread.getDebugTarget());

        this.thread = thread;
        this.path = path;
        this.lineNumber = lineNumber;
        this.topFrame = true;
        this.db = db;
        this.previous = previous;
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
        super(thread.getDebugTarget());
        
        this.thread = thread;
        this.path = path;
        this.lineNumber = lineNumber;
        this.topFrame = false;
        this.db = null;
        
        if (returnType.equals(".")) returnType = "void";
        else if (returnType.equals("@")) returnType = "list";
        else if (returnType.equals("$")) returnType = "scalar";
        
        displayedVars = new PerlDebugVar[2];
        displayedVars[0] = createSpecialVar("Called Function", calledSub);
        displayedVars[1] = createSpecialVar("Return Type", returnType);
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
        if (displayedVars == null && topFrame)
        {
            actualVars = readTopFrameVars();
            computeDisplayedVars();
            highlightChangedVariables();
        }
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
    
    private PerlDebugVar createSpecialVar(String name, String value)
    {
        PerlDebugVar var = new PerlDebugVar(
            getDebugTarget(),
            PerlDebugVar.GLOBAL_SCOPE,
            name,
            new PerlDebugValue(getDebugTarget(), null, value));
        
        var.setSpecial();
        return var;
    }
    
    private void highlightChangedVariables() throws DebugException
    {
        if (previous == null ||
            !previous.getPath().equals(this.getPath())) return;
        
        previous.previous = null; // avoid a memory leak

        PerlDebugVar[] oldVars = (PerlDebugVar[]) previous.getVariables();
        PerlDebugVar[] newVars = (PerlDebugVar[]) this.getVariables();
    
        boolean found;
        for (int new_pos = 0; new_pos < newVars.length; ++new_pos)
        {
            found = false;
            PerlDebugVar var_new = newVars[new_pos];
            if (oldVars != null)
            {
                for (int org_pos = 0; (org_pos < oldVars.length)
                    && !found; ++org_pos)
                {
                    PerlDebugVar var_org = oldVars[org_pos];
                    if (var_new.matches(var_org))
                    {
                        found = true;
                        var_new.calculateChangeFlags(var_org);
                    }
                }
                if (!found)
                {
                    var_new.setChangeFlags(
                        PerlDebugValue.VALUE_HAS_CHANGED, true);
                }
            }
        }
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
        return topFrame;
    }
    
    private PerlDebugVar[] readTopFrameVars() throws DebugException
    {
        try
        {
            if (!db.isSuspended()) return new PerlDebugVar[] {
                createSpecialVar(
                    "Warning",
                    "Variables are only available in suspended mode") };
            
            TokenVarParser varParser = new TokenVarParser(
                getDebugTarget(), PerlDebugPlugin.getDefault().getLog());
            
            List vars = new ArrayList();        
            if (ShowLocalVariableActionDelegate.getPreferenceValue())
            {
                String localVarsString = db.eval(DB_DUMP_LOCAL_VARS);
                if (localVarsString.startsWith(PADWALKER_ERROR))
                {
                    vars.add(createSpecialVar(
                        "Error",
                        "Install PadWalker Perl module to see local variables"));
                }
                else
                {                    
                    varParser.parseVars(
                        localVarsString,
                        PerlDebugVar.LOCAL_SCOPE,
                        vars);
                }
            }
            String globalVarsString = db.eval(DB_DUMP_GLOBAL_VARS);
            varParser.parseVars(
                globalVarsString,
                PerlDebugVar.GLOBAL_SCOPE,
                vars);
            
            return (PerlDebugVar[]) vars.toArray(
                new PerlDebugVar[vars.size()]);
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
    
    private static String loadHelperScript(String scriptName)
    {
        try
        {
            return PerlDebugPlugin.getDefault().loadHelperScript(scriptName);
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return "print \"E\"";
        }
    }
}