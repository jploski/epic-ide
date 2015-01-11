package org.epic.debug.db;

import java.io.IOException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.ui.action.*;

public class StackFrame extends DebugElement implements IStackFrame
{
    private static final IRegisterGroup[] NO_REGISTER_GROUPS =
        new IRegisterGroup[0];
    
    private static final String DB_DUMP_LOCAL_VARS;    
    private static final String DB_DUMP_GLOBAL_VARS;
    private static final String DB_DUMP_INTERNAL_VARS;
    
    private static final Set<String> PERL_INTERNAL_VARS = initInternalVars();

    private PerlVariable[] vars;
    private final PerlDebugThread thread;
    private final IPath path;
    private final IPath localPath;
    private final int lineNumber;
    private final DebuggerInterface db;
    private StackFrame previous;
    private final int frameIndex; // 0 = top stack frame, 1 = one below...
    private final HashMap<String, PerlVariable> rememberedVariables; // only for RFE 1708299
    
    static
    {
        DB_DUMP_LOCAL_VARS = HelperScript.load("dump_local_vars.pl");
        DB_DUMP_GLOBAL_VARS = HelperScript.load("dump_global_vars.pl");
        DB_DUMP_INTERNAL_VARS = HelperScript.load("dump_internal_vars.pl");
    }
    
    public StackFrame(
        PerlDebugThread thread,
        IPath path,
        int lineNumber,
        IPath localPath,
        DebuggerInterface db,
        StackFrame previous,
        int frameIndex) throws DebugException
    {
        super(thread.getDebugTarget());
        
        if (previous != null) previous.previous = null; // conserve memory

        this.thread = thread;
        this.path = path;
        this.localPath = localPath;
        this.lineNumber = lineNumber;
        this.db = db;
        this.previous = previous;
        this.frameIndex = frameIndex;
        this.rememberedVariables = new HashMap<String, PerlVariable>();
        
        if (HighlightVarUpdatesActionDelegate.getPreferenceValue())
            getVariables();
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
    
    public void discardCachedVars()
    {
        this.vars = null;
        rememberedVariables.clear();
        fireChangeEvent(DebugEvent.CONTENT);
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
    
    /**
     * @return the given variable as it was on the previous suspend or
     *         null if it is not remembered
     */
    public PerlVariable getPreviousVariable(PerlVariable var)
    {
        if (previous == null) return null;
        return previous.getRememberedVariable(
            var.getDumpedEntity().getAddress());
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
        if (!thread.isSuspended()) return new IVariable[0];
        if (this.vars == null)
        {        
            try
            {
                List<PerlVariable> vars = new ArrayList<PerlVariable>();
                
                if (ShowPerlInternalVariableActionDelegate.getPreferenceValue()) dumpInternalVars(vars);
                if (ShowGlobalVariableActionDelegate.getPreferenceValue()) dumpGlobalVars(vars);
                if (ShowLocalVariableActionDelegate.getPreferenceValue() && db.hasPadWalker()) dumpLocalVars(vars);
                this.vars = vars.toArray(new PerlVariable[vars.size()]);

                if (HighlightVarUpdatesActionDelegate.getPreferenceValue() &&
                    rememberedVariables.isEmpty()) rememberVariables();
                
                fireChangeEvent(DebugEvent.CONTENT);
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
        return this.vars;
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
    
    private void dumpGlobalVars(List<PerlVariable> vars) throws IOException, DebugException
    {
        String globalVarsString = db.eval(DB_DUMP_GLOBAL_VARS);
        if (globalVarsString == null) return;
        
        try
        {
            DumpedEntityReader r = new DumpedEntityReader(globalVarsString);
            while (r.hasMoreEntities())
            {
                DumpedEntity ent = r.nextEntity();
                if (!PERL_INTERNAL_VARS.contains(ent.getName()))
                    vars.add(new PackageVariable(db, this, ent));
            }
        }
        catch (Exception e)
        {
            PerlDebugPlugin.log(e);
            throw new DebugException(new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                "An error occurred while dumping global variables; " +
                "contents of the Variables view may become invalid",
                e));
        }
    }

    private void dumpInternalVars(List<PerlVariable> vars) throws IOException, DebugException
    {
        String internalVarsString = db.eval(DB_DUMP_INTERNAL_VARS);
        if (internalVarsString == null) return;
        
        try
        {
            DumpedEntityReader r = new DumpedEntityReader(internalVarsString);
            while (r.hasMoreEntities())
            {
                DumpedEntity ent = r.nextEntity();
                if (PERL_INTERNAL_VARS.contains(ent.getName()))
                    vars.add(new PackageVariable(db, this, ent));
            }
        }
        catch (Exception e)
        {
            PerlDebugPlugin.log(e);
            throw new DebugException(new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                "An error occurred while dumping Perl internal variables; " +
                "contents of the Variables view may become invalid",
                e));
        }
    }

    private void dumpLocalVars(List<PerlVariable> vars) throws IOException, DebugException
    {         
        String code = HelperScript.replace(
            DB_DUMP_LOCAL_VARS,
            "#SET_OFFSET#",
            "my $offset = " + frameIndex + ";");

        String localVarsString = db.eval(code);
        if (localVarsString == null) return;
        
        try
        {        
            DumpedEntityReader r = new DumpedEntityReader(localVarsString);                
            while (r.hasMoreEntities())
            {
                vars.add(new LexicalVariable(
                    db,
                    this,
                    r.nextEntity()));
            }
        }
        catch (Exception e)
        {
            throw new DebugException(new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                "An error occurred while dumping local variables; " +
                "contents of the Variables view may become invalid",
                e));
        }
    }
    
    private PerlVariable getRememberedVariable(String addr)
    {
        return rememberedVariables.get(addr);
    }
    
    private static Set<String> initInternalVars()
    {
        ResourceBundle rb = ResourceBundle.getBundle("org.epic.debug.perlIntVars");
        Enumeration<String> e = rb.getKeys();
        Set<String> vars = new HashSet<String>();
        while (e.hasMoreElements()) vars.add(e.nextElement());
        return vars;
    }
    
    private void rememberVariables() throws DebugException
    {
        LinkedList<IVariable[]> queue = new LinkedList<IVariable[]>();
        queue.add(this.vars);
        
        while (!queue.isEmpty())
        {
            IVariable[] vars = queue.removeFirst();
        
            for (int i = 0; i < vars.length; i++)
            {
                if (!(vars[i] instanceof PerlVariable))
                    continue; // could be an ArraySlice
                
                PerlVariable var = (PerlVariable) vars[i];
                String addr = var.getDumpedEntity().getAddress();
                
                if (rememberedVariables.put(addr, var) == null &&
                    var.getValue().hasVariables())
                {
                    queue.add(var.getValue().getVariables());
                }
            }
        }
    }
}
