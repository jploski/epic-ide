package org.epic.debug;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.core.resources.*;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IEditorDescriptor;
/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DebugModelPresentation implements IDebugModelPresentation {

	/**
	 * Constructor for DebugModelPresentation.
	 */
	public DebugModelPresentation() {
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#setAttribute(String, Object)
	 */
	public void setAttribute(String attribute, Object value) {
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object element) {
		
		if (element instanceof IMarker) {
			IBreakpoint bp = getBreakpoint((IMarker)element);
			if (bp != null && bp instanceof PerlBreakpoint) {
				return getPerlBreakpointImage((PerlLineBreakpoint)bp);
						}
			}
		if( element instanceof PerlBreakpoint)
		return( getPerlBreakpointImage((PerlBreakpoint) element) );
		
		return null;
	}

	protected Image getPerlBreakpointImage(PerlBreakpoint breakpoint)  {
			
			try{
				int flags= computeBreakpointAdornmentFlags(breakpoint);
				PerlImageDescriptor descriptor= null;
				if (  breakpoint.isEnabled() && ! breakpoint.isNoValidBreakpointPosition() ) 
				{
					descriptor= new PerlImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_OBJS_BREAKPOINT), flags);
				} else {
					descriptor= new PerlImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED), flags);
				}
				return PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(descriptor);
			} catch (CoreException e )
				{ 
					PerlDebugPlugin.log(e);
				}
				
			return null;
		}



	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
	 */
	public String getText(Object element) {
		StringBuffer erg = new StringBuffer();
		
		if( element instanceof PerlLineBreakpoint )
		{
			PerlLineBreakpoint bp = ((PerlLineBreakpoint)element);
			
			erg.append( bp.getResourcePath().lastSegment());
			erg.append(" [line: "+Integer.toString(bp.getLineNumber())+"]");
			if( bp.isNoValidBreakpointPosition())
				erg.append(" - <invalid position>");
			return(erg.toString());	
		}
		return(null);
	}

	/**
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#computeDetail(IValue, IValueDetailListener)
	 */
	public void computeDetail(IValue value, IValueDetailListener listener) {
	}

	/**
	 * @see org.eclipse.debug.ui.ISourcePresentation#getEditorInput(Object)
	 */
	public IEditorInput getEditorInput(Object element) {
		IEditorInput i;
		StackFrame frame;
		IPath p;
		
		System.out.println("@@@@@@@@getInput: "+(element instanceof StackFrame)+"\n");
		if(element instanceof StackFrame )
		{
			frame = (StackFrame) element;
		}
		else
		{
			return(null);
		}
		
		
		p = frame.get_IP_Path();
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IFile file;
		file = myWorkspaceRoot.getFileForLocation(p);
		if( file != null )
			i= new FileEditorInput(file);
		else
		  	i = new LocalFileStorageEditorInput(p.toString());
		  	
		return(i);

	}

	/**
	 * @see org.eclipse.debug.ui.ISourcePresentation#getEditorId(IEditorInput, Object)
	 */
	public String getEditorId(IEditorInput input, Object element) {
		System.out.println("@@@@@@@@getEditor\n");
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
				IEditorDescriptor descriptor= registry.getDefaultEditor(input.getName());
				if (descriptor != null)
					return descriptor.getId();

				return null;
		//return "org.epic.perleditor.editors.PerlEditor";
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}


	/**
	 * Returns the adornment flags for the given breakpoint.
	 * These flags are used to render appropriate overlay
	 * icons for the breakpoint.
	 */
	private int computeBreakpointAdornmentFlags( PerlBreakpoint breakpoint)  {
		int flags= 0;
		if( breakpoint.isNoValidBreakpointPosition())
			return 0;
			
		try {
			if (breakpoint.isEnabled()) {
				flags |= PerlImageDescriptor.ENABLED;
			}
			if (breakpoint.isInstalled()) {
				flags |= PerlImageDescriptor.INSTALLED;
			}
			
			} catch (CoreException e) {
			
			PerlDebugPlugin.log(e);
		}
		return flags;
	}
	
	protected IBreakpoint getBreakpoint(IMarker marker) {
			return DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
			}
}
