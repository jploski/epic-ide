package org.epic.debug.ui.action;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;


/**
 *@deprecated direct cut and paste of
 *            org.eclipse.debug.ui.actions.RulerEnableDisableBreakpointAction to enable support for
 *            3.1 users. if/when 3.1 support is no longer required, this class can be removed.
 */
public class RulerEnableDisableBreakpointAction extends RulerBreakpointAction implements IUpdate
{
    //~ Instance fields

    private IBreakpoint fBreakpoint;

    //~ Constructors

    public RulerEnableDisableBreakpointAction(ITextEditor editor, IVerticalRulerInfo info)
    {
        super(editor, info);
    }

    //~ Methods

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run()
    {
        if (fBreakpoint != null)
        {
            try
            {
                fBreakpoint.setEnabled(! fBreakpoint.isEnabled());
            }
            catch (CoreException e)
            {
                DebugUIPlugin.errorDialog(getEditor().getSite().getShell(),
                    "error enabling/disabling breakpoint",
                    "error enabling/disabling breakpoint", e.getStatus());
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    public void update()
    {
        fBreakpoint = getBreakpoint();
        setEnabled(fBreakpoint != null);
        if (fBreakpoint != null)
        {
            try
            {
                if (fBreakpoint.isEnabled())
                {
                    setText("Disable Breakpoint");
                }
                else
                {
                    setText("Enable Breakpoint");
                }
            }
            catch (CoreException e)
            {
            }
        }
        else
        {
            setText("Disable Breakpoint");
        }
    }

}
