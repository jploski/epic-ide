package org.epic.debug.util;

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.debug.internal.core.InputStreamMonitor;
import org.epic.debug.PerlLaunchConfigurationConstants;
import org.epic.debug.db.PerlDebugThread;

/**
 * @author ST
 */
public class DebuggerProxy extends PlatformObject
    implements IProcess, ITerminate, IStreamsProxy
{
    private OutputStreamMonitor mMonitorError;
    private OutputStreamMonitor mMonitorOut;
    private InputStreamMonitor mMonitorIn;
    private ILaunch mLaunch;
    private PerlDebugThread mDebugger;
    private String mLabel;
    private RemotePort mIOStream;
    private RemotePort mErrorStream;
    private boolean mShutDown;
    private final String ioHost;

    public DebuggerProxy(String fLabel, ILaunch fLaunch, String ioHost)
    {
        mLabel = fLabel;
        mLaunch = fLaunch;
        this.ioHost = ioHost;

        create();
        
        mIOStream = new RemotePort("DebuggerProxy.mIOStream");
        mIOStream.startConnect();
        mErrorStream = new RemotePort("DebuggerProxy.mErrorStream");
        mErrorStream.startConnect();
    }

    public void init(PerlDebugThread fDebugger) throws CoreException
    {
        mDebugger = fDebugger;
        mLaunch = fDebugger.getLaunch();
        
        if (shouldRedirectIO())
        {
            if (mIOStream.waitForConnect(true) != RemotePort.WAIT_OK)
                throwCouldNotConnect();
    
            if (mErrorStream.waitForConnect(true) != RemotePort.WAIT_OK)
                throwCouldNotConnect();

            mMonitorIn = new InputStreamMonitor(mIOStream.getOutStream());
            mMonitorOut.setStream(mIOStream.getInStream());
            mMonitorError.setStream(mErrorStream.getInStream());
            mMonitorIn.startMonitoring();
            mMonitorOut.startMonitoring();
            mMonitorError.startMonitoring();
        }

        mLabel = "Remote Perl Script";
    }
    
    public int getErrorPort()
    {
        return mErrorStream.getServerPort();
    }
    
    public String getIOHost()
    {
        return ioHost;
    }
    
    public int getIOPort()
    {
        return mIOStream.getServerPort();
    }

    public String getLabel()
    {
        return mLabel;
    }

    public ILaunch getLaunch()
    {
        return mLaunch;
    }

    public IStreamsProxy getStreamsProxy()
    {
        return this;
    }

    public void setAttribute(String key, String value)
    {
        mLaunch.setAttribute(key, value);
    }

    public String getAttribute(String key)
    {
        return mLaunch.getAttribute(key);
    }

    public int getExitValue() throws DebugException
    {
        return 0;
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter.equals(IProcess.class)) return this;

        if (adapter.equals(IDebugTarget.class))
        {
            IDebugTarget[] targets = getLaunch().getDebugTargets();

            for (int i = 0; i < targets.length; i++)
                if (this.equals(targets[i].getProcess())) return targets[i];

            return null;
        }
        return super.getAdapter(adapter);
    }

    public boolean canTerminate()
    {
        if (mDebugger == null) return !mShutDown;
        return mDebugger.canTerminate();
    }

    public boolean isTerminated()
    {
        if (mDebugger == null) return !canTerminate();
        return mDebugger.isTerminated();
    }

    public IStreamMonitor getErrorStreamMonitor()
    {
        return mMonitorError;
    }

    public IStreamMonitor getOutputStreamMonitor()
    {
        return mMonitorOut;
    }

    public void terminate() throws DebugException
    {
        if (mDebugger != null) mDebugger.terminate();
        shutdown();
    }

    public void write(String input) throws IOException
    {
        if (mMonitorIn != null) mMonitorIn.write(input);
    }

    private void create()
    {
        mMonitorOut = new OutputStreamMonitor();
        mMonitorError = new OutputStreamMonitor();
        fireCreationEvent();
    }

    /**
     * Fire a debug event marking the creation of this element.
     */
    private void fireCreationEvent()
    {
        fireEvent(new DebugEvent(this, DebugEvent.CREATE));
    }

    /**
     * Fire a debug event
     */
    private void fireEvent(DebugEvent event)
    {
        DebugPlugin manager = DebugPlugin.getDefault();
        if (manager != null)
        {
            manager.fireDebugEventSet(new DebugEvent[] { event });
        }
    }

    /**
     * Fire a debug event marking the termination of this process.
     */
    private void fireTerminateEvent()
    {
        fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
    }
    
    private boolean shouldRedirectIO()
    {
        try
        {
            return mLaunch.getLaunchConfiguration().getAttribute(
                PerlLaunchConfigurationConstants.ATTR_REMOTE_CAPTURE_OUTPUT,
                true);
        }
        catch (CoreException e)
        {
            return true; /* should never occur */
        }
    }

    private void shutdown()
    {
        mShutDown = true;
        if (mMonitorError != null) mMonitorError.kill();
        if (mMonitorOut != null) mMonitorOut.kill();
        if (mMonitorIn != null) mMonitorIn.close();
        if (mMonitorOut != null) mMonitorOut.close();
        if (mIOStream != null) mIOStream.shutdown();
        if (mErrorStream != null) mErrorStream.shutdown();
        fireTerminateEvent();
    }

    private void throwCouldNotConnect() throws CoreException
    {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DebugPlugin.getUniqueIdentifier(),
            IStatus.OK,
            "Timed out while waiting for IO redirect from the debugged process",
            null));
    }
}