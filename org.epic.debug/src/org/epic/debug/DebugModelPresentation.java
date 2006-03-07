package org.epic.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.epic.core.util.FileUtilities;
import org.epic.debug.ui.PerlDebugImages;
import org.epic.debug.ui.PerlImageDescriptor;
import org.epic.debug.varparser.PerlDebugVar;
/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DebugModelPresentation implements IDebugModelPresentation
{

	/**
	 * Constructor for DebugModelPresentation.
	 */
	public DebugModelPresentation()
	{
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#setAttribute(String, Object)
	 */
	public void setAttribute(String attribute, Object value)
	{
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object element)
	{

		if (element instanceof PerlDebugVar)
		{
			try
			{
				if (((PerlDebugVar) element).getHideImage())
					return PerlDebugPlugin
						.getDefaultDesciptorImageRegistry()
						.get(
						new PerlImageDescriptor(
							DebugUITools.getImageDescriptor(
								IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED),
							0));
				;

				if (((PerlDebugVar) element).isLocalScope())
				{
					if (((PerlDebugVar) element).isTainted())
						return PerlDebugPlugin
							.getDefaultDesciptorImageRegistry()
							.get(
							PerlDebugImages.DESC_OBJS_CHANGED_DEBUG_VAR_LOCAL);
					else
						return PerlDebugPlugin
							.getDefaultDesciptorImageRegistry()
							.get(
							PerlDebugImages.DESC_OBJS_DEBUG_VAR_LOCAL);
				}

				if (((PerlDebugVar) element).isTainted())
					return PerlDebugPlugin
						.getDefaultDesciptorImageRegistry()
						.get(
						PerlDebugImages.DESC_OBJS_CHANGED_DEBUG_VAR);
			} catch (DebugException e)
			{
				e.printStackTrace();
			}
		}

		if (element instanceof IMarker)
		{
			IBreakpoint bp = getBreakpoint((IMarker) element);
			if (bp != null && bp instanceof PerlBreakpoint)
			{
				return getPerlBreakpointImage((PerlLineBreakpoint) bp);
			}
		}
		if (element instanceof PerlBreakpoint)
			return (getPerlBreakpointImage((PerlBreakpoint) element));

		return null;
	}

	protected Image getPerlBreakpointImage(PerlBreakpoint breakpoint)
	{

		try
		{
			int flags = computeBreakpointAdornmentFlags(breakpoint);
			PerlImageDescriptor descriptor = null;

			if (breakpoint.isEnabled()
				&& !breakpoint.isNoValidBreakpointPosition())
			{
				if (breakpoint instanceof PerlRegExpBreakpoint)
					descriptor =
						new PerlImageDescriptor(
							PerlDebugImages.DESC_OBJS_REGEXP_BP_ENABLED,
							flags);
				else
					descriptor =
						new PerlImageDescriptor(
							DebugUITools.getImageDescriptor(
								IDebugUIConstants.IMG_OBJS_BREAKPOINT),
							flags);
			} else
			{
				if (breakpoint instanceof PerlRegExpBreakpoint)
									descriptor =
										new PerlImageDescriptor(
											PerlDebugImages.DESC_OBJS_REGEXP_BP_DISABLED,
											flags);
								else
				descriptor =
					new PerlImageDescriptor(
						DebugUITools.getImageDescriptor(
							IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED),
						flags);
			}
			return PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
				descriptor);
		} catch (CoreException e)
		{
			PerlDebugPlugin.log(e);
		}

		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
	 */
	public String getText(Object element)
	{
		StringBuffer erg = new StringBuffer();

		if (element instanceof PerlLineBreakpoint)
		{
			PerlLineBreakpoint bp = ((PerlLineBreakpoint) element);

			if (bp.getResourcePath() == null)
				return ("[]");
			erg.append(bp.getResourcePath().lastSegment());
			erg.append(" [line: " + Integer.toString(bp.getLineNumber()) + "]");
			if (bp.isNoValidBreakpointPosition())
				erg.append(" - <invalid position>");
			return (erg.toString());
		}
		return (null);
	}

	/**
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#computeDetail(IValue, IValueDetailListener)
	 */
	public void computeDetail(IValue value, IValueDetailListener listener)
	{
		try
		{
			listener.detailComputed(value, value.getValueString());
		} catch (DebugException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ISourcePresentation#getEditorInput(Object)
	 */
	public IEditorInput getEditorInput(Object element)
	{
		IEditorInput i;
		StackFrame frame = null;
		IPath p;

		System.out.println(
			"@@@@@@@@getInput: " + (element instanceof StackFrame) + "\n");
		if (element instanceof StackFrame)
		{
			frame = (StackFrame) element;
		} else
		{
			if( element instanceof PerlBreakpoint )
			{ return (((PerlBreakpoint) element).getEditorInput());}
		}
		if( frame == null) return null;
		p = frame.get_IP_Path();
		/*IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IFile file;
		file = myWorkspaceRoot.getFileForLocation(p);
		if( file != null )
			i= new FileEditorInput(file);
		else
		{
		  	i = new LocalFileStorageEditorInput(p.toString());
		  	i = new LocalFileEditorInput(i);
		} */
		i = FileUtilities.getFileEditorInput(p);

		return (i);

	}

	/**
	 * @see org.eclipse.debug.ui.ISourcePresentation#getEditorId(IEditorInput, Object)
	 */
	public String getEditorId(IEditorInput input, Object element)
	{
		System.out.println("@@@@@@@@getEditor\n");
		IEditorRegistry registry =
			PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor descriptor =
			registry.getDefaultEditor(input.getName());
		if (descriptor != null)
			return descriptor.getId();

		return null;
		//return "org.epic.perleditor.editors.PerlEditor";
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener)
	{
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose()
	{
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property)
	{
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener)
	{
	}

	/**
	 * Returns the adornment flags for the given breakpoint.
	 * These flags are used to render appropriate overlay
	 * icons for the breakpoint.
	 */
	private int computeBreakpointAdornmentFlags(PerlBreakpoint breakpoint)
	{
		int flags = 0;
		if (breakpoint.isNoValidBreakpointPosition())
			return 0;

		try
		{
			if (breakpoint.isEnabled())
			{
				flags |= PerlImageDescriptor.ENABLED;
			}
			if (breakpoint.isInstalled())
			{
				flags |= PerlImageDescriptor.INSTALLED;
			}

		} catch (CoreException e)
		{

			PerlDebugPlugin.log(e);
		}
		return flags;
	}

	protected IBreakpoint getBreakpoint(IMarker marker)
	{
		return DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(
			marker);
	}
}
