package org.epic.debug.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
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
import org.epic.debug.*;
import org.epic.debug.db.*;

/**
 * @author ruehl
 */
public class DebugModelPresentation implements IDebugModelPresentation
{
    public void setAttribute(String attribute, Object value)
    {
    }

    public Image getImage(Object element)
    {
        if (element instanceof PerlVariable)
        {
            boolean contentChanged = false;
            try
            {
                contentChanged = ((PerlVariable) element).hasContentChanged();
            }
            catch (DebugException e) { PerlDebugPlugin.log(e); }
            
            if (!((PerlVariable) element).isPackageScope())
            {
                return PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
                    contentChanged
                    ? PerlDebugImages.DESC_OBJS_CHANGED_DEBUG_VAR_LOCAL
                    : PerlDebugImages.DESC_OBJS_DEBUG_VAR_LOCAL);
            }
            else if (contentChanged)
            {
                return PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
                    PerlDebugImages.DESC_OBJS_CHANGED_DEBUG_VAR);
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
                && !breakpoint.isInvalidBreakpointPosition())
            {
                if (breakpoint instanceof PerlRegExpBreakpoint) descriptor = new PerlImageDescriptor(
                    PerlDebugImages.DESC_OBJS_REGEXP_BP_ENABLED, flags);
                else descriptor = new PerlImageDescriptor(DebugUITools
                    .getImageDescriptor(IDebugUIConstants.IMG_OBJS_BREAKPOINT),
                    flags);
            }
            else
            {
                if (breakpoint instanceof PerlRegExpBreakpoint) descriptor = new PerlImageDescriptor(
                    PerlDebugImages.DESC_OBJS_REGEXP_BP_DISABLED, flags);
                else descriptor = new PerlImageDescriptor(
                    DebugUITools
                        .getImageDescriptor(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED),
                    flags);
            }
            return PerlDebugPlugin.getDefaultDesciptorImageRegistry().get(
                descriptor);
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
        }

        return null;
    }

    public String getText(Object element)
    {
        StringBuffer erg = new StringBuffer();

        if (element instanceof PerlLineBreakpoint)
        {
            PerlLineBreakpoint bp = ((PerlLineBreakpoint) element);

            if (bp.getResourcePath() == null) return ("[]");
            erg.append(bp.getResourcePath().lastSegment());
            try
            {
                erg.append(" [line: " + Integer.toString(bp.getLineNumber())
                    + "]");
            }
            catch (CoreException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (bp.isInvalidBreakpointPosition())
                erg.append(" - <invalid position>");
            return (erg.toString());
        }
        return (null);
    }

    public void computeDetail(IValue value, IValueDetailListener listener)
    {
        try
        {
        	if (value instanceof PerlValue)
        	{
	            listener.detailComputed(
	                value,
	                ((PerlValue) value).getDetailValue());
        	}
        	else listener.detailComputed(value, "");
        }
        catch (DebugException e)
        {
            PerlDebugPlugin.log(e);
        }
    }

    public IEditorInput getEditorInput(Object element)
    {
        if (element instanceof StackFrame)
        {
            // TODO: shouldn't we return IEditorInput of an already
            // open editor, if possible?
            StackFrame frame = (StackFrame) element;
            if (frame.getLocalPath() == null) return null;
            return FileUtilities.getFileEditorInput(frame.getLocalPath());
        }
        else if (element instanceof PerlBreakpoint)
        {
            return ((PerlBreakpoint) element).getEditorInput();
        }
        else return null;
    }

    public String getEditorId(IEditorInput input, Object element)
    {
        IEditorRegistry registry =
            PlatformUI.getWorkbench().getEditorRegistry();
        IEditorDescriptor descriptor =
            registry.getDefaultEditor(input.getName());
        
        return descriptor != null ? descriptor.getId() : null;
    }

    public void addListener(ILabelProviderListener listener)
    {
    }

    public void dispose()
    {
    }

    public boolean isLabelProperty(Object element, String property)
    {
        return false;
    }

    public void removeListener(ILabelProviderListener listener)
    {
    }

    /**
     * Returns the adornment flags for the given breakpoint. These flags are
     * used to render appropriate overlay icons for the breakpoint.
     */
    private int computeBreakpointAdornmentFlags(PerlBreakpoint breakpoint)
    {
        int flags = 0;
        if (breakpoint.isInvalidBreakpointPosition()) return 0;

        try
        {
            if (breakpoint.isEnabled())
            {
                flags |= PerlImageDescriptor.ENABLED;
            }
            /*if (breakpoint.isInstalled()) // TODO restore this functionality?
            {
                flags |= PerlImageDescriptor.INSTALLED;
            }*/
        }
        catch (CoreException e)
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
