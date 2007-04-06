package org.epic.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.epic.debug.db.PerlDB;

/**
 * @author ruehl
 */
public class PerlDebugThread extends DebugElement implements IThread
{
    private String mName;
    private IDebugTarget mDebugTarget;
    private ILaunch mLaunch;
    private PerlDB mPerlDB;

    private IStackFrame[] mFrames;
    private final static IBreakpoint[] mNoBreakpoints = new IBreakpoint[1];

    public PerlDebugThread(
        String name,
        ILaunch launch,
        IDebugTarget debugTarget,
        PerlDB fPerlDB)
    {
        super(debugTarget);

        mName = name;
        mDebugTarget = debugTarget;
        mLaunch = launch;
        mFrames = null;
        mPerlDB = fPerlDB;
        fireCreationEvent();
    }

    public IStackFrame[] getStackFrames() throws DebugException
    {
        return mFrames;
    }

    public void setStackFrames(IStackFrame[] fFrames)
    {
        mFrames = fFrames;
    }

    public boolean hasStackFrames() throws DebugException
    {
        return mFrames != null ? (mFrames[0] != null) && isSuspended() : false;
    }

    public int getPriority() throws DebugException
    {
        return 0;
    }

    public IStackFrame getTopStackFrame() throws DebugException
    {
        return mFrames != null ? mFrames[0] : null;
    }


    public String getName() throws DebugException
    {
        if (isSuspended()) return ("<suspended>" + mName);
        else if (!isTerminated()) return ("<running>" + mName);

        return mName;
    }

    public IBreakpoint[] getBreakpoints()
    {
        return mNoBreakpoints;
    }

    public String getModelIdentifier()
    {
        return PerlDebugPlugin.getUniqueIdentifier();
    }

    public IDebugTarget getDebugTarget()
    {
        return mDebugTarget;
    }

    public ILaunch getLaunch()
    {
        return mLaunch;
    }

    public boolean canResume()
    {
        return mPerlDB.canResume(this);
    }

    public boolean canSuspend()
    {
        return mPerlDB.canSuspend(this);
    }

    public boolean isSuspended()
    {
        return mPerlDB.isSuspended(this);
    }

    public void resume() throws DebugException
    {
        mPerlDB.resume(this);
    }

    public void suspend() throws DebugException
    {
        mPerlDB.suspend(this);
    }

    public boolean canStepInto()
    {
        return mPerlDB.canStepInto(this);
    }

    public boolean canStepOver()
    {
        return mPerlDB.canStepOver(this);
    }

    public boolean canStepReturn()
    {
        return mPerlDB.canStepReturn(this);
    }

    public boolean isStepping()
    {
        return mPerlDB.isStepping(this);
    }

    public void stepInto() throws DebugException
    {
        mPerlDB.stepInto(this);
    }

    public void stepOver() throws DebugException
    {
        mPerlDB.stepOver(this);
    }

    public void stepReturn() throws DebugException
    {
        mPerlDB.stepReturn(this);
    }

    public boolean canTerminate()
    {
        return mPerlDB.canTerminate(this);
    }

    public boolean isTerminated()
    {
        return mPerlDB.isTerminated(this);
    }

    public void terminate() throws DebugException
    {
        mPerlDB.terminate();
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == PerlDebugThread.class)
        {
            return this;
        }
        if (adapter == IStackFrame.class)
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

    public PerlDB getPerlDB()
    {
        return mPerlDB;
    }
}
