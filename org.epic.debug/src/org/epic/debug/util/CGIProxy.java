/*
 * Created on 03.04.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

package org.epic.debug.util;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;

/** 
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CGIProxy extends PlatformObject implements IProcess, ITerminate
{
	private volatile boolean mIsConnected;
	private Thread mWaitThread;
	private OutputStreamMonitor mMonitorError;
	private OutputStreamMonitor mMonitorOut;
	private OutputStreamMonitor mMonitorIn;
    private ILaunch mLaunch;
    private String mLabel;
	private RemotePort mInStream;
    private RemotePort mOutStream;
    private RemotePort mErrorStream;
    private boolean mIsTerminated;
    private IStreamsProxy mStreamsProxy;

	public CGIProxy(ILaunch fLaunch, String fLabel)
	{
		mLaunch = fLaunch;
		((IProcess) this).setAttribute(ATTR_PROCESS_TYPE, "EpicCGIProxy");
		mIsConnected = false;
		mIsTerminated = false;
		mLabel = fLabel;

		mInStream = new RemotePort("CGIProxy.mInStream");
		mInStream.startConnect();
		mOutStream = new RemotePort("CGIProxy.mOutStream");
		mOutStream.startConnect();
		mErrorStream = new RemotePort("CGIProxy.mErrorStream");
		mErrorStream.startConnect();

		mWaitThread = new Thread("EPIC-Debugger:CGIProxy")
		{

			public void run()
			{
				try
				{
					int ret;
					ret = mInStream.waitForConnect(false);
					if (ret == RemotePort.mWaitError)
						PerlDebugPlugin.getDefault().logError(
							"Could not connect to CGI-Console");
					if (ret == RemotePort.mWaitOK)
					{
						ret = mOutStream.waitForConnect(true);
						if (ret == RemotePort.mWaitOK)
							ret = mErrorStream.waitForConnect(true);
					}

					if (ret != RemotePort.mWaitOK)
					{
						if (ret == RemotePort.mWaitError)
							PerlDebugPlugin.getDefault().logError(
								"Could not connect to CGI-Console");
						terminate();
						return;
					}
				} catch (DebugException e)
				{
					PerlDebugPlugin.getDefault().logError(
						"Could not connect to CGI-Console",
						e);
				}

				mMonitorIn = new OutputStreamMonitor(mInStream.getInStream());
				mMonitorOut = new OutputStreamMonitor(mOutStream.getInStream());
				mMonitorError =
					new OutputStreamMonitor(mErrorStream.getInStream());
				mMonitorIn.startMonitoring();
				mMonitorOut.startMonitoring();
				mMonitorError.startMonitoring();
                mStreamsProxy = new IStreamsProxy() {
                    public IStreamMonitor getErrorStreamMonitor()
                    {
                        return mMonitorError;
                    }

                    public IStreamMonitor getOutputStreamMonitor()
                    {
                        return mMonitorOut;
                    }

                    public void write(String input)
                        throws IOException
                    {
                        // we can't provide input to a CGI process
                        // through console
                    }
                };
				mIsConnected = true;
				fireCreationEvent();
			}
		};

		mWaitThread.start();
	}

	public boolean isConnected()
	{
		return mIsConnected;
	}

    public int getErrorPort()
    {
        return mErrorStream.getServerPort();
    }
    
    public int getInPort()
    {
        return mInStream.getServerPort();
    }

    public int getOutPort()
    {
        return mOutStream.getServerPort();
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getLabel()
	 */
	public String getLabel()
	{
		return mLabel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getLaunch()
	 */
	public ILaunch getLaunch()
	{
		return mLaunch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getStreamsProxy()
	 */
	public IStreamsProxy getStreamsProxy()
	{
		return mStreamsProxy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String key, String value)
	{
		ILaunchConfigurationWorkingCopy workingcopy;
		try
		{
			workingcopy = mLaunch.getLaunchConfiguration().getWorkingCopy();
			workingcopy.setAttribute(key, value);
			workingcopy.doSave();
		} catch (CoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getAttribute(java.lang.String)
	 */
	public String getAttribute(String key)
	{
		try
		{
			return mLaunch.getLaunchConfiguration().getAttribute(
				key,
				(String) null);
		} catch (CoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getExitValue()
	 */
	public int getExitValue() throws DebugException
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter)
	{
		if (adapter.equals(IProcess.class))
		{
			return this;
		}
		if (adapter.equals(IDebugTarget.class))
		{
			ILaunch launch = getLaunch();
			IDebugTarget[] targets = launch.getDebugTargets();
			for (int i = 0; i < targets.length; i++)
			{
				if (this.equals(targets[i].getProcess()))
				{
					return targets[i];
				}
			}
			return null;
		}
		return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate()
	{
		return !isTerminated();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated()
	{
		return mIsTerminated;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException
	{
		shutdown();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStreamsProxy#getErrorStreamMonitor()
	 */
	public IStreamMonitor getErrorStreamMonitor()
	{
		return mMonitorError;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStreamsProxy#getOutputStreamMonitor()
	 */
	public IStreamMonitor getOutputStreamMonitor()
	{
		return mMonitorOut;
	}

	public IStreamMonitor getInputStreamMonitor()
	{
		return mMonitorIn;
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

	void shutdown()
	{
		mMonitorError.kill();
		mMonitorOut.kill();
		mMonitorIn.kill();
		mInStream.shutdown();
		mOutStream.shutdown();
		mErrorStream.shutdown();
		mIsTerminated = true;
		fireTerminateEvent();
	}
	/**
	 * @return
	 */
	public Thread getWaitThread()
	{
		return mWaitThread;
	}

	public void waitForConnect()
	{
		try { mWaitThread.join(30000); }
        catch (InterruptedException e) { }
	}
}
