/*
 * Created on 30.05.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import java.util.*;
import org.eclipse.debug.core.DebugPlugin;


/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlBreakpointManager implements IBreakpointListener {

	/**
	 * 
	 */
	private Set mDebuggerInstances;
	private DebugPlugin mDebugPlugin;
	
	
	public PerlBreakpointManager(DebugPlugin fPlugin) {
		super();
		mDebuggerInstances = new HashSet();
		mDebugPlugin = fPlugin;
		fPlugin.getBreakpointManager().addBreakpointListener(this);
		}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(org.eclipse.debug.core.model.IBreakpoint)
	 */
	public void breakpointAdded(IBreakpoint fBreakpoint) {
		PerlDB db;
		PerlBreakpoint bp;

		if( ! ( fBreakpoint instanceof PerlBreakpoint)  )
					return;
		bp = (PerlBreakpoint)fBreakpoint;

		for( Iterator i = mDebuggerInstances.iterator(); i.hasNext();)
		{
			db = (PerlDB) i.next();
			db.addBreakpoint(bp);
			bp.addDebuger(db);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(org.eclipse.debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointRemoved(IBreakpoint fBreakpoint, IMarkerDelta delta) {
		PerlDB db;
		PerlBreakpoint bp;
		
		if( ! ( fBreakpoint instanceof PerlBreakpoint)  )
						return;
		bp = (PerlBreakpoint)fBreakpoint;
								
		for( Iterator i = bp.getDebugerIterator(); i.hasNext();)
		{
			db = (PerlDB) i.next();
			db.removeBreakpoint(bp);
			bp.removeDebuger(db);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(org.eclipse.debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		// TODO Auto-generated method stub

	}
	
	public void addDebugger( PerlDB fDb ) 
	{
		IBreakpoint[] bps;
		
		mDebuggerInstances.add(fDb);
		bps= DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(PerlDebugPlugin.getUniqueIdentifier());
		for( int x = 0; x < bps.length; ++x)
		{
			fDb.addBreakpoint( ((PerlBreakpoint)bps[x]) );
			((PerlBreakpoint)bps[x]).addDebuger(fDb);
		}
		
	}
	
	public void removeDebugger( PerlDB fDb)
	{
		IBreakpoint[] bps;
		
		mDebuggerInstances.remove(fDb);
				
		bps= DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(PerlDebugPlugin.getUniqueIdentifier());
		for( int x = 0; x < bps.length; ++x)
		{
			((PerlBreakpoint)bps[x]).removeDebuger(fDb);
		}
	}
	
	public void removeBreakpoint( IBreakpoint fBp)
	{
		try{
		mDebugPlugin.getBreakpointManager().removeBreakpoint(fBp,true);
		}catch (Exception e){PerlDebugPlugin.log(e);}
	}
}
