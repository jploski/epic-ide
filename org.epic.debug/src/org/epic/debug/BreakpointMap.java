/*
 * Created on 28.05.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.Breakpoint;

public class BreakpointMap {
	
	private Map mBPs;
	
	public  BreakpointMap()
	{
		mBPs = new HashMap();
	}
	
	
	
	
	public void add(PerlBreakpoint fBp)
	{
		IPath path =fBp.getResourcePath();
		Set set;
		
		set = (Set) mBPs.get(path);	
		if( set == null)
		{
			set = new HashSet();
			mBPs.put(path,set);
		}
		
		set.add(fBp);
	}
	
	
	public boolean contains(PerlBreakpoint fBp)
	{
		IPath path =fBp.getResourcePath();
		Set set;
	
		set = (Set) mBPs.get(path);	
		if( set == null)
			return(false);
	
		return(set.contains(fBp));
	}
	
	
	
	public Set getBreakpointsForFile(IPath fPath)
	{
		return((Set) mBPs.get(fPath));	
	}
	
	public PerlBreakpoint getBreakpointForLocation(IPath fPath, int fLine)
	{
		Set breakpoints = getBreakpointsForFile(fPath);
		Breakpoint bp;
		if( breakpoints == null)
		 return null;
		
		for( Iterator i = breakpoints.iterator(); i.hasNext();)
		{
			bp = (Breakpoint) i.next();
			if( bp instanceof PerlLineBreakpoint)
			{
				if( ((PerlLineBreakpoint)bp).getResourcePath().equals(fPath) &&
					( fLine ==  ((PerlLineBreakpoint)bp).getLineNumber() )
				  )
				  return ((PerlLineBreakpoint)bp);
			}
		}
		return null;
	}

	public boolean  remove(PerlBreakpoint fBP)
	{
		IPath path =fBP.getResourcePath();
		Set set;
		
		set = (Set) mBPs.get(path);
		
		if( set == null)
			return(false);
			
		return(set.remove(fBP));
	}		
		
}
