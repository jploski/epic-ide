/*
 * Created on 12.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

import org.eclipse.debug.core.model.Breakpoint;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import java.util.*;


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
	IPath mResourcePath;
	Set  mDebuger;
	Set  mInstallations;

	public final static String INVALID_POS = "PerlDebug_INVALID_POS" ;
	public final static String IS_INSTALLED = "PerlDebug_IS_INSTALLED" ;
	
	boolean mIsNoValidBreakpointPosition;
	
	public PerlBreakpoint(IResource resource) {
		super();
		mResourcePath = resource.getRawLocation();
		mDebuger = new HashSet();
		mInstallations = new HashSet();
		mIsNoValidBreakpointPosition = false;
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
		return mResourcePath;
	}
	
	public boolean isInstalled()
	{
		return( getMarker().getAttribute(IS_INSTALLED,false) );
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
	
	
	
	public Iterator getDebugerIterator()
	{
		return( mDebuger.iterator());
	}
	
	public void addInstallation(PerlDB fDb)
	{
		boolean erg;
		
		erg  = mInstallations.add(fDb);
		if( erg && (mInstallations.size() == 1) )
		{	
			try
			{
				getMarker().setAttribute(IS_INSTALLED,true);
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
					getMarker().setAttribute(IS_INSTALLED,false);
				}catch ( Exception e){PerlDebugPlugin.log(e);}
			}
		
	}
	
}
