/*
 * Created on 30.05.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */


package org.epic.debug;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;


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

		try {
			if( ! bp.isEnabled() ) return;
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			
			e1.printStackTrace();
		}
			for( Iterator i = mDebuggerInstances.iterator(); i.hasNext();)
			{
				db = (PerlDB) i.next();
				db.addBreakpoint(bp);
				bp.addDebuger(db);
			}
		
		

		try {
			bp.getMarker().setAttribute(PerlBreakpoint.IS_INSTALLED,true);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
		Object[] debuggers = bp.getDebuggers();
		
		for( int x = 0; x < debuggers.length; ++x)
		{
			db = (PerlDB) debuggers[x];
			db.removeBreakpoint(bp);
			bp.removeDebuger(db);
		}
		try {
			bp.getMarker().setAttribute(PerlBreakpoint.IS_INSTALLED,false);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(org.eclipse.debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
		public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		// TODO Auto-generated method stub
		if( !(breakpoint instanceof PerlLineBreakpoint) ) return;
		boolean enabled_old =( (Boolean) delta.getAttribute(IBreakpoint.ENABLED)).booleanValue();
		boolean enabled_new = false;
		try {
			enabled_new = ( (Boolean) breakpoint.getMarker().getAttribute(IBreakpoint.ENABLED)).booleanValue();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		if(  enabled_old != enabled_new )
		{
			if(enabled_new) this.breakpointAdded(breakpoint);
			else this.breakpointRemoved(breakpoint, delta);
		}
		
	}
	
	public void addDebugger( PerlDB fDb ) 
	{
		IBreakpoint[] bps;
		
		mDebuggerInstances.add(fDb);
		bps= DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(PerlDebugPlugin.getUniqueIdentifier());
		for( int x = 0; x < bps.length; ++x)
		{
			try {
				if( ((PerlBreakpoint)bps[x]).isEnabled())
				{
				fDb.addBreakpoint( ((PerlBreakpoint)bps[x]) );
				((PerlBreakpoint)bps[x]).addDebuger(fDb);
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public Set getDebugger()
	{
		return mDebuggerInstances;
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
