package org.epic.debug.ui.action;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;

/**
 * Creates adapters for retargettable actions in debug platform.
 * Contributed via <code> org.eclipse.core.runtime.adapters</code>
 * extension point.
 */
public class RetargettableActionAdapterFactory implements IAdapterFactory
{
    //~ Methods

    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (adapterType == IToggleBreakpointsTarget.class)
        {
            return new ToggleBreakpointAdapter();
        }

        return null;
    }

    public Class[] getAdapterList()
    {
        return new Class[] { IToggleBreakpointsTarget.class };
    }

}
