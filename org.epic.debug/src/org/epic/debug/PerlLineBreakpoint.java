/*
 * Created on 11.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;


/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */


public class PerlLineBreakpoint extends PerlBreakpoint implements ILineBreakpoint {
	
	private static final String Perl_LINE_BREAKPOINT = "org.epic.debug.perlLineBreakpointMarker"; //$NON-NLS-1$
	
	
	public PerlLineBreakpoint() {
						super();
						Map attributes = new HashMap(10); 
					}		
					
	public PerlLineBreakpoint(IResource resource, int lineNumber) throws DebugException, CoreException {
				super();
			    Map attributes = new HashMap(10); 
				createPerlLineBreakpoint(resource,lineNumber, -1, -1, true, attributes,getMarkerID());
			}
			
	
	public PerlLineBreakpoint(IResource resource, int lineNumber, int charStart, int charEnd, boolean add, Map attributes) 
		 	throws DebugException,CoreException {
				super();
				if( attributes == null)
				{ attributes = new HashMap(10); }
				createPerlLineBreakpoint(resource,lineNumber, charStart, charEnd, add, attributes,getMarkerID());
		}
		
	String getMarkerID()
	{
		return(Perl_LINE_BREAKPOINT);
	}
	
	/**
	 * 
	 */
	public void  createPerlLineBreakpoint(final IResource resource,final int lineNumber,final int charStart, final int charEnd,final boolean add, final Map attributes, final String markerType) 
		throws CoreException,DebugException{
		
		
		IWorkspaceRunnable wr= new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {

						// create the marker
						setMarker(resource.createMarker(markerType));
						//setMarker(resource.createMarker("org.eclipse.debug.core.lineBreakpointMarker"));

					   //setMarker(resource.createMarker(IMarker.PROBLEM));

						// add attributes
						addLineBreakpointAttributes(attributes, getModelIdentifier(), true, lineNumber, charStart, charEnd);
						// set attributes
						ensureMarker().setAttributes(attributes);

						// add to breakpoint manager if requested
						register(add);
					}
				};
				run(wr);
	}


	public void addLineBreakpointAttributes(Map attributes, String modelIdentifier, boolean enabled, int lineNumber, int charStart, int charEnd) {
			//addBreakPointAttributes(attributes);
			attributes.put(IBreakpoint.ID, modelIdentifier);
			attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
			attributes.put(IMarker.CHAR_START, new Integer(-1));
			attributes.put(IMarker.CHAR_END, new Integer(-1));
			attributes.put(PerlBreakpoint.INVALID_POS, new Boolean(false));
			attributes.put(IBreakpoint.PERSISTED, Boolean.TRUE);
		    attributes.put(IBreakpoint.ENABLED, Boolean.TRUE);
		    attributes.put(IBreakpoint.REGISTERED, Boolean.FALSE);
		    
		}
		
	public int getLineNumber()
	{
		return getMarker().getAttribute(IMarker.LINE_NUMBER,-1);	
	}
	
	public int getCharStart()
	{
		return getMarker().getAttribute(IMarker.CHAR_START,-1);	
	}
	
	public int getCharEnd()
	{
		return getMarker().getAttribute(IMarker.CHAR_END,-1);	
	}
	/**
		 * Add this breakpoint to the breakpoint manager,
		 * or sets it as unregistered.
		 */
		protected void register(boolean register) throws CoreException {
			if (register) {
				DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(this);
			} else {
				setRegistered(false);
			}
			IBreakpointManager m  = DebugPlugin.getDefault().getBreakpointManager();
			IBreakpoint [] b = m.getBreakpoints(PerlDebugPlugin.getUniqueIdentifier());
		}
		
	/**
		 * @see IBreakpoint#setMarker(IMarker)
		 */
		public void setMarker(IMarker marker) throws CoreException {
			super.setMarker(marker);
			//configureAtStartup();
		}
	

	/**
		 * Execute the given workspace runnable
		 */
		protected void run(IWorkspaceRunnable wr) throws DebugException {
			try {
				ResourcesPlugin.getWorkspace().run(wr, null);
			} catch (CoreException e) {
				throw new DebugException(e.getStatus());
			}
		}
		
	
}
