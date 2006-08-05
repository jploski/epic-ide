package org.epic.debug.ui.action;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;


/**
 * Creates adapters for retargettable actions in debug platform. Contributed via <code>
 * org.eclipse.core.runtime.adapters</code> extension point.
 */
public class RetargettableActionAdapterFactory implements IAdapterFactory
{
    //~ Methods

    /*
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
     */
    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (adapterType == IToggleBreakpointsTarget.class) { return new ToggleBreakpointAdapter(); }

        return null;
    }

    /*
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
     */
    public Class[] getAdapterList()
    {
        return new Class[] { ToggleBreakpointAdapter.class };
    }

}
