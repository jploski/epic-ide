package org.epic.debug;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.epic.debug.db.*;
import org.epic.debug.db.DebuggerInterface.SessionTerminatedException;
import org.epic.debug.ui.action.ShowGlobalVariableActionDelegate;
import org.epic.debug.ui.action.ShowLocalVariableActionDelegate;
import org.epic.debug.util.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * An implementation of IDebugTarget which tracks a Perl debugger
 * process and communicates with it using a TCP port.
 */
public class DebugTarget extends PerlTarget
{
    public static final int SESSION_TERMINATED = 10000;
    
    private final IDebugEventSetListener listener = new IDebugEventSetListener() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            for (int i = 0; i < events.length; i++)
            {
                if (events[i].getKind() == DebugEvent.TERMINATE &&
                    getProcess().equals(events[i].getSource()))
                {
                    shutdown(); // interrupt the acceptNewDebugger thread
                    return;
                }
            }
        } };
    
    private final IProcess process;
    private final RemotePort debugPort;
    private final PerlDebugThread thread;
    private final DebuggerInterface db;
    private final IPathMapper pathMapper;
    
    /**
     * @exception DebugException with status SESSION_TERMINATED
     *            if the Perl process associated with this DebugTarget
     *            has terminated prematurely, most probably due to
     *            compile-time errors in the script to be executed
     */
    public DebugTarget(
        ILaunch launch,
        IProcess process,
        RemotePort debugPort,
        IPathMapper pathMapper) throws CoreException
    {
        super(launch);
        
        try
        {
            try
            {
                this.process = process;
                this.debugPort = debugPort;
                this.pathMapper = pathMapper;
                this.db = createDebuggerInterface();
                
                HashKeySorter.init();
                
                checkPadWalker();
                this.thread = new PerlDebugThread(this, db);
                
                registerDebugEventListener(listener);        
                fireCreationEvent();
            }
            catch (SessionTerminatedException e)
            {
                throw new DebugException(new Status(
                    IStatus.OK,
                    PerlDebugPlugin.getUniqueIdentifier(),
                    DebugTarget.SESSION_TERMINATED,
                    "Debugger session terminated (compile error?)",
                    e));
            }
        }
        catch (CoreException e)
        {
            shutdown();
            throw e;
        }
        catch (RuntimeException e)
        {
            shutdown();
            throw e;
        }
    }
    
    public String getName() throws DebugException
    {
        return getProcess().getLabel();
    }
    
    public IProcess getProcess()
    {
        return process;
    }    

    public boolean isLocal()
    {
        return true;
    }

    public IPathMapper getPathMapper()
    {
        return pathMapper;
    }
    
    public IThread getThread()
    {
    	return thread;
    }

    public IThread[] getThreads() throws DebugException
    {
        return new IThread[] { thread };
    }

    public boolean hasThreads() throws DebugException
    {
        return true;
    }

    public boolean supportsBreakpoint(IBreakpoint breakpoint)
    {
        return false; // TODO?
    }

    public boolean canTerminate()
    {
        return !isTerminated();
    }

    public boolean isTerminated()
    {
        return thread.isTerminated();
    }

    public void terminate() throws DebugException
    {
        thread.terminate();
    }

    public boolean canResume()
    {
        return thread.canResume();
    }

    public boolean canSuspend()
    {
        return thread.canSuspend();
    }

    public boolean isSuspended()
    {
        return thread.isSuspended();
    }

    public void resume() throws DebugException
    {
        thread.resume();
    }

    public void suspend() throws DebugException
    {
        thread.suspend();
    }

    public void breakpointAdded(IBreakpoint breakpoint)
    {
    }

    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta)
    {
    }

    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta)
    {
    }

    public boolean canDisconnect()
    {
        return false;
    }

    public void disconnect() throws DebugException
    {
        unsupportedOperation();
    }

    public boolean isDisconnected()
    {
        return false;
    }

    public boolean supportsStorageRetrieval()
    {
        return false;
    }

    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException
    {
        unsupportedOperation();
        return null;
    }
    
    protected void shutdown()
    {
        unregisterDebugEventListener(listener);
        debugPort.shutdown();
        super.shutdown();
    }
    
    protected RemotePort getRemotePort()
    {
        return debugPort;
    }
    
    protected DebuggerInterface initDebuggerInterface(DebuggerInterface db)
        throws DebugException
    {
        if (getPathMapper().requiresEffectiveIncPath())
            getPathMapper().setEffectiveIncPath(getEffectiveIncPath(db));       

        return db;
    }
    
    protected void registerDebugEventListener(IDebugEventSetListener listener)
    {
        DebugPlugin.getDefault().addDebugEventListener(listener);
    }
    
    protected final void throwDebugException(IOException e) throws DebugException
    {
        throw new DebugException(new Status(
            IStatus.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            "An error occurred during communication with the debugger process",
            e));
    }

    protected void unregisterDebugEventListener(IDebugEventSetListener listener)
    {
        DebugPlugin.getDefault().removeDebugEventListener(listener);
    }
    
    protected void checkPadWalker() throws DebugException
    {
        try
        {
            if ((ShowLocalVariableActionDelegate.getPreferenceValue() ||
                ShowGlobalVariableActionDelegate.getPreferenceValue())
                && !db.hasPadWalker())
            {
                PerlDebugPlugin.errorDialog(
                    "Error displaying local variables\n" +
                    "Install PadWalker and restart Eclipse " +
                    "or disable displaying of local variables.");
            }
        }
        catch (IOException e)
        {
            throwDebugException(e);
        }
    }
    
    protected boolean getDebugConsolePreference()
    {
        return PerlEditorPlugin.getDefault().getBooleanPreference(
            PreferenceConstants.DEBUG_DEBUG_CONSOLE);
    }

    private DebuggerInterface createDebuggerInterface()
        throws SessionTerminatedException, DebugException
    {
        BufferedReader in = debugPort.getReadStream();
        PrintWriter out = debugPort.getWriteStream();

        if (getDebugConsolePreference())
        {
            DebuggerProxy2 p = new DebuggerProxy2(in, out, getLaunch());
            getLaunch().addProcess(p);
            in = p.getDebugIn();
            out = p.getDebugOut();
        }
        
        try
        {   
            return initDebuggerInterface(new DebuggerInterface(in, out));
        }
        catch (SessionTerminatedException e)
        {
            throw e; // happens on compile errors in the script
        }
        catch (IOException e)
        {
            throwDebugException(e);
            return null; // unreachable
        }
    }
    
    private List<IPath> getEffectiveIncPath(DebuggerInterface db)
        throws DebugException
    {
        try
        {
            List<IPath> ret = new ArrayList<IPath>();
            String output = db.eval(
                ";{foreach $t(@INC) {print $DB::OUT $t.\"\\n\";}}");
        
            StringTokenizer s = new StringTokenizer(output, "\r\n");            
            while (s.hasMoreTokens()) ret.add(new Path(s.nextToken()));
            return ret;
        }
        catch (IOException e) { throwDebugException(e); return null; }
    }
}