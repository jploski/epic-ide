/*
 * Created on 11.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;


/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */


public class PerlRegExpBreakpoint extends PerlLineBreakpoint{
	
	private static final String Perl_REGEXP_BREAKPOINT = "org.epic.debug.perlRegExpBreakpointMarker"; //$NON-NLS-1$
	
	
	public PerlRegExpBreakpoint() {
						super();
 
					}		
					
	public PerlRegExpBreakpoint(IResource resource, int lineNumber) throws DebugException, CoreException {
				super(resource, lineNumber);
			}
			
	
	public PerlRegExpBreakpoint(IResource resource, int lineNumber, int charStart, int charEnd, boolean add, Map attributes) 
		 	throws DebugException,CoreException {
				super(resource, lineNumber, charStart, charEnd, add, attributes);
		}
		
	String getMarkerID()
		{
			return(Perl_REGEXP_BREAKPOINT);
		}


	
}
