package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;

public class ShowVarAddressActionDelegate extends VariablesViewActionDelegate
{
    private static IAction action;

    public void init(IAction action)
    {
        ShowVarAddressActionDelegate.action = action;
    }

    public static boolean getPreferenceValue()
    {
        return action == null ? false : action.isChecked();
    }
}