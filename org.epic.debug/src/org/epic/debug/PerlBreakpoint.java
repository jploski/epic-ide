/*
 * Created on 12.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package org.epic.debug;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.Breakpoint;
import org.eclipse.debug.core.model.IBreakpoint;





/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class PerlBreakpoint extends Breakpoint {

	/**
	 * 
	 */
	//IPath mResourcePath;
	Set  mDebuger;
	Set  mInstallations;

	public final static String INVALID_POS = "PerlDebug_INVALID_POS" ;
	public final static String IS_INSTALLED = "PerlDebug_IS_INSTALLED" ;
	//public final static String RESOURCE_PATH = "PerlDebug_BreakPointResourcePath" ;
	
	boolean mIsNoValidBreakpointPosition;
	
//	public PerlBreakpoint(IResource resource) {
//		super();
//		mResourcePath = resource.getRawLocation();
//		mDebuger = new HashSet();
//		mInstallations = new HashSet();
//		mIsNoValidBreakpointPosition = false;
//	}

	public PerlBreakpoint() {
			super();
			mDebuger = new HashSet();
			mInstallations = new HashSet();
			mIsNoValidBreakpointPosition = false;
			//mResourcePath = null;
		}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IBreakpoint#getModelIdentifier()
	 */
	public String getModelIdentifier() 
	{
		return PerlDebugPlugin.getUniqueIdentifier();
	}
	
	public IPath getResourcePath()
	{
		return getMarker().getResource().getLocation();
	}

	public boolean isInstalled()
	{
		return( mInstallations.size() > 0);
		//return( getMarker().getAttribute(IS_INSTALLED,false) );
	}
	
	public void setIsNoValidBreakpointPosition( boolean fVal)
	{
		try{
				getMarker().setAttribute(INVALID_POS,fVal);
		}catch ( Exception e){PerlDebugPlugin.log(e);}	
		
		//DebugPlugin.getDefault().getBreakpointManager().fireBreakpointChanged(this);
		
	}
	public boolean isNoValidBreakpointPosition()
	{
		return getMarker().getAttribute(INVALID_POS,false);	
	}
	public void addDebuger(PerlDB fDB)
	{
		mDebuger.add(fDB);	
	}
	
	public void removeDebuger(PerlDB fDb)
	{
		mDebuger.remove(fDb);
		removeInstallation(fDb);	
	}
	
	
	
	public Object[] getDebuggers()
	{
		return(  mDebuger.toArray());
	}
	
	public void addInstallation(PerlDB fDb)
	{
		boolean erg;
		
		erg  = mInstallations.add(fDb);
		if( erg && (mInstallations.size() == 1) )
		{	
			try
			{
			//	getMarker().setAttribute(IS_INSTALLED,true);
			}catch ( Exception e){PerlDebugPlugin.log(e);}
		}
			
	}
	
	public void removeInstallation(PerlDB fDb)
	{
			boolean erg;
		
			erg  = mInstallations.remove(fDb);
			if( erg && (mInstallations.size() == 0) )
			{	
				try
				{
			//		getMarker().setAttribute(IS_INSTALLED,false);
				}catch ( Exception e){PerlDebugPlugin.log(e);}
			}
		
	}
	
	
	/**
			 * @see IBreakpoint#setMarker(IMarker)
			 */
			public void setMarker(IMarker marker) throws CoreException {
				marker.setAttribute(IS_INSTALLED,false);
				super.setMarker(marker);
//				if( mResourcePath == null )
//				{
//					String pathString = (String)marker.getAttribute(RESOURCE_PATH);
//					if (pathString != null)
//						mResourcePath =  new Path( marker.getAttribute(RESOURCE_PATH,pathString));
//				}
				//configureAtStartup();
				
			}
			
//	void addBreakPointAttributes(Map attributes)
//	{
//		attributes.put(RESOURCE_PATH,mResourcePath.toString());
//
//	}
			
}
