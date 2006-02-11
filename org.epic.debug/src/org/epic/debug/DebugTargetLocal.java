package org.epic.debug;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.epic.debug.util.RemotePort;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DebugTargetLocal extends DebugTarget implements IDebugEventSetListener 
{

	

	/**
	 * Constructor for DebugTarget.
	 */
	public DebugTargetLocal()
	{
		super();
	}

	/**
		 * Constructor for DebugTarget.
		 */
	public DebugTargetLocal(ILaunch launch)
	{
		super(launch);
		DebugPlugin.getDefault().addDebugEventListener(this);
		initPath();

	}

	/* (non-Javadoc)
	 * @see org.epic.debug.Target#isLocal()
	 */
	boolean isLocal() {
		
		return true;
	}
	
	public void start()
	{
		if (connectDebugger(true) != RemotePort.mWaitOK)
        {
            terminate();
        }
        else
        {
            getDebugger().generateDebugInitEvent();
        }
	}
	 
	
	public void handleDebugEvents(DebugEvent[] events)
	{
		for (int i = 0; i < events.length; i++)
		{
			if (events[i].getKind() == DebugEvent.TERMINATE)
				if (events[i].getSource() == mProcess)
					DebugPlugin.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						terminate();
					}
				});
		}
	}
}