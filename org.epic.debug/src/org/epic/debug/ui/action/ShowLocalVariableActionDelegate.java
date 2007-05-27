package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;

public class ShowLocalVariableActionDelegate extends VariablesViewActionDelegate
{
    private static IAction action;

    public void init(IAction action)
    {
        ShowLocalVariableActionDelegate.action = action;
    }

    public static boolean getPreferenceValue()
    {
        return action == null ? true : action.isChecked();
    }
}