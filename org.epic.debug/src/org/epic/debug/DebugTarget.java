package org.epic.debug;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.DebugPlugin;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DebugTarget implements IDebugTarget {

	private ILaunch mLaunch;
	private IProcess mProcess;
	private PerlDB mPerlDB;

	/**
	 * Constructor for DebugTarget.
	 */
	public DebugTarget() {
		super();
	}

/**
	 * Constructor for DebugTarget.
	 */
	public DebugTarget(ILaunch launch) {
		super();
		mLaunch = launch;
		try{
		mPerlDB = new PerlDB(this);
		}catch (Exception e){System.out.println("Failing to create DB-Process: "+e+" !!!");}

		//mThreads[0]= new PerlDebugThread("Hallo",mLaunch,this);
		mProcess = DebugPlugin.newProcess(launch,mPerlDB.getProcess(),"Perl-Debugger");
	}
	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
	 */
	public IProcess getProcess() {
		return mProcess;
	}
	
	public PerlDB getDebuger()
	{
		return mPerlDB;
	}
	
	

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getThreads()
	 */
	public IThread[] getThreads() throws DebugException {
		return mPerlDB.getThreads();
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#hasThreads()
	 */
	public boolean hasThreads() throws DebugException {
		return(mPerlDB.getThreads() != null );
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getName()
	 */
	public String getName() throws DebugException {
		return ("Perl-Debugger");
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#supportsBreakpoint(IBreakpoint)
	 */
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return false;
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
		return this;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return mLaunch;
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate() {
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated() {
		return mPerlDB.isTerminated(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException {
	}


	public boolean canResume() {
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(IBreakpoint)
	 */
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(IBreakpoint, IMarkerDelta)
	 */
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(IBreakpoint, IMarkerDelta)
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
	}


	public boolean canDisconnect() {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDisconnect#disconnect()
	 */
	public void disconnect()
	{
	}

	public boolean isDisconnected() {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#supportsStorageRetrieval()
	 */
	public boolean supportsStorageRetrieval() {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#getMemoryBlock(long, long)
	 */
	public IMemoryBlock getMemoryBlock(long startAddress, long length)
		throws DebugException {
		return null;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class arg0) {
		return null;
	}

}