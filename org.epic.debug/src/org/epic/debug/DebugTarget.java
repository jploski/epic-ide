package org.epic.debug;

import java.io.BufferedReader;
import java.io.PrintWriter;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IThread;
import org.epic.debug.util.PathMapper;
import org.epic.debug.util.PathMapperCygwin;
import org.epic.debug.util.RemotePort;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author ruehl
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public abstract class DebugTarget extends Target {

	private boolean mShutDownStarted;
	
	private final static IThread[] mNoThreads = new IThread[0];
	RemotePort mDebugPort;

	protected PerlDB mPerlDB;

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
		super(launch);

	}

	public DebugTarget(ILaunch launch, RemotePort fPort) {
		super(launch);
		mDebugPort = fPort;
	}
	public void start() {
		if (connectDebugger(true) != RemotePort.mWaitOK)
			terminate();
	}
	int connectDebugger(boolean fTimeout) {
		int res = connectDebugPort(fTimeout);

		if (res != RemotePort.mWaitOK) {
			return res;
		}

		try {
			mPerlDB = new PerlDB(this);
		} catch (Exception e) {
			PerlDebugPlugin.getDefault().logError(
					"Failing to create PerlDB-Interface !!!", e);
			return RemotePort.mWaitError;
		}

		return (RemotePort.mWaitOK);
	}

	protected int connectDebugPort(boolean fTimeout) {

		if (mDebugPort == null) {
			mDebugPort = new RemotePort("DebugTarget.mDebugPort");
			mDebugPort.startConnect();
		} else
			mDebugPort.startReconnect();

		startPerlProcess();

		int res = mDebugPort.waitForConnect(fTimeout);

		if (res == RemotePort.mWaitError)
			PerlDebugPlugin.errorDialog("Could not connect to debug port!");
        return res;
	}

	public PrintWriter getDebugWriteStream() {
		return mDebugPort.getWriteStream();
	}

	public BufferedReader getDebugReadStream() {
		return mDebugPort.getReadStream();
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
	 */

	public PerlDB getDebugger() {
		return mPerlDB;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getThreads()
	 */
	public IThread[] getThreads() throws DebugException {
		if (mPerlDB == null)
			return mNoThreads;
		return mPerlDB.getThreads();
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#hasThreads()
	 */
	public boolean hasThreads() throws DebugException {
		if (mPerlDB == null)
			return (false);
		return (mPerlDB.getThreads() != null);
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getName()
	 */

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
		if (mPerlDB == null)
			return true;
		return mPerlDB.isTerminated(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() {
		shutdown();
	}

	public boolean canResume() {
		if (mPerlDB == null)
			return false;

		return mPerlDB.canResume(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		if (mPerlDB == null)
			return false;
		return mPerlDB.canSuspend(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		if (mPerlDB == null)
			return false;
		return mPerlDB.isSuspended(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
		mPerlDB.resume(this);
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
		mPerlDB.suspend(this);
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(IBreakpoint)
	 */
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(IBreakpoint,
	 *      IMarkerDelta)
	 */
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(IBreakpoint,
	 *      IMarkerDelta)
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	public boolean canDisconnect() {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDisconnect#disconnect()
	 */
	public void disconnect() {
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
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#getMemoryBlock(long,
	 *      long)
	 */
	public IMemoryBlock getMemoryBlock(long startAddress, long length)
			throws DebugException {
		return null;
	}

	

	public void shutdown(boolean unregister) {
		if (mShutDownStarted)
			return;
		mShutDownStarted = true;
		if (mPerlDB != null && !mPerlDB.isTerminated())
			mPerlDB.shutdown();
		if (mDebugPort != null)
			mDebugPort.shutdown();
		super.shutdown(unregister);
	}

	void debugSessionTerminated() {
		shutdown();
		//		Thread term = new Thread()
		//		{
		//			public void run()
		//			{
		//				shutdown();
		//			}
		//		};
		//
		//		term.start();
		//		return;
	}

	public String getDebugPort() {
		return Integer.toString(mDebugPort.getServerPort());
	}

	public PathMapper getPathMapper() {
		String interpreterType = PerlEditorPlugin.getDefault()
				.getPreferenceStore().getString(
						PerlEditorPlugin.INTERPRETER_TYPE_PREFERENCE);

		if (interpreterType.equals(PerlEditorPlugin.INTERPRETER_TYPE_CYGWIN))
			return (new PathMapperCygwin());
		else
			return (null);
	}

	public void perlDBstarted(PerlDB fDB) {
	}
    
    protected Process startPerlProcess()
    {
        // note: subclasses override this.. ugly as hell but works
        Process p = super.startPerlProcess();
        if (p == null) mDebugPort.shutdown();
        return p;
    }
}