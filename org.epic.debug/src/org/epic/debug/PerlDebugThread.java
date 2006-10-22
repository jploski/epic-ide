package org.epic.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PerlDebugThread extends DebugElement implements IThread {

	/**
	 * Constructor for PerlDebugThread.
	 */
	private String 		mName;
	private IDebugTarget 	mDebugTarget;
	private ILaunch 		mLaunch;
	private PerlDB			mPerlDB;
	
	private IStackFrame [] mFrames;  
	private  final static IBreakpoint[] mNoBreakpoints = new IBreakpoint[1];
	


	public PerlDebugThread(String name,ILaunch launch, IDebugTarget debugTarget,PerlDB fPerlDB) {
		super(debugTarget);
		mName = name;
		mDebugTarget = debugTarget;
		mLaunch = launch;
		mFrames =null;
		mPerlDB = fPerlDB;
		fireCreationEvent();
	}

	/**
	 * @see org.eclipse.debug.core.model.IThread#getStackFrames()
	 */
	public IStackFrame[] getStackFrames() throws DebugException {
		return mFrames;
	}
	
	public void setStackFrames(IStackFrame[] fFrames)
	{
		mFrames = fFrames;
	}
	/**
	 * @see org.eclipse.debug.core.model.IThread#hasStackFrames()
	 */
	public boolean hasStackFrames() throws DebugException {
		return mFrames != null ? (mFrames[0] != null) && isSuspended() : false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IThread#getPriority()
	 */
	public int getPriority() throws DebugException {
		return 0;
	}

	/**
	 * @see org.eclipse.debug.core.model.IThread#getTopStackFrame()
	 */
	public IStackFrame getTopStackFrame() throws DebugException {
		return mFrames != null ? mFrames[0] : null;
	}

	/**
	 * @see org.eclipse.debug.core.model.IThread#getName()
	 */
	public String getName() throws DebugException {
		if( isSuspended() )
		 	return ("<suspended>"+mName);
		else
			if( !isTerminated() )
				return ("<running>"+mName);
			
		return (mName);
		
	}


	/**
	 * @see org.eclipse.debug.core.model.IThread#getBreakpoints()
	 */
	public IBreakpoint[] getBreakpoints() {
		return mNoBreakpoints;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return PerlDebugPlugin.getUniqueIdentifier();
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return mDebugTarget;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return mLaunch;
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
	 */
	public boolean canResume() {
		return( mPerlDB.canResume(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		return( mPerlDB.canSuspend(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		System.out.println(" isSuspended:"+mPerlDB.isSuspended(this)+"\n");
		return( mPerlDB.isSuspended(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
			System.out.println("DEbugperl-Thread: Resuming\n");
		    mPerlDB.resume(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
		System.out.println("DEbugperl-Thread: suspending\n");
		mPerlDB.suspend(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepInto()
	 */
	public boolean canStepInto() {
		return( mPerlDB.canStepInto(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepOver()
	 */
	public boolean canStepOver() {
		return( mPerlDB.canStepOver(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepReturn()
	 */
	public boolean canStepReturn() {
		return( mPerlDB.canStepReturn(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#isStepping()
	 */
	public boolean isStepping() {
		return( mPerlDB.isStepping(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#stepInto()
	 */
	public void stepInto() throws DebugException {
			System.out.println("DEbugperl-Thread: StepingInto\n");
			mPerlDB.stepInto(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#stepOver()
	 */
	public void stepOver() throws DebugException {
		System.out.println("DEbugperl-Thread: StepingOver\n");
		mPerlDB.stepOver(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#stepReturn()
	 */
	public void stepReturn() throws DebugException {
			System.out.println("DEbugperl-Thread: StepReturn\n");
			mPerlDB.stepReturn(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate() {
		return(mPerlDB.canTerminate(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated() {
		System.out.println("DEbugperl-Thread: is terminated\n");
		return(mPerlDB.isTerminated(this));
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException {
		System.out.println("DEbugperl-Thread: terminating\n");
		mPerlDB.terminate();
	}
	

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == PerlDebugThread.class) {
			return this;
		}
		if (adapter == StackFrame.class) {
			try {
				return (StackFrame)getTopStackFrame();
			} catch (DebugException e) {
				// do nothing if not able to get frame
			}
		}
		return super.getAdapter(adapter);
	}
	
	public PerlDB getPerlDB()
	{
		return(mPerlDB);
	}

}
