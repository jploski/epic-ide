package org.epic.debug;

import java.io.BufferedReader;
import java.io.PrintWriter;

import org.eclipse.core.resources.IMarkerDelta;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.Launch;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IThread;
import org.epic.debug.util.RemotePort;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DebugTargetLocal extends DebugTarget 
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
		initPath();

	}

	public void start()
	{
		if (connectDebugger(true) != RemotePort.mWaitOK)
					terminate();
	}
	 
	

}