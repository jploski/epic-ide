package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;
import org.epic.perleditor.preferences.PreferenceConstants;

public class ShowLocalVariableActionDelegate extends VariablesViewActionDelegate
{
    public static String ACTION_ID = "org.epic.debug.showLocalVariablesAction";
    public static String KEY = PreferenceConstants.DEBUG_SHOW_LOCAL_VARS;
    private static IAction action;

    public void init(IAction action)
    {
        super.init(action);
        ShowLocalVariableActionDelegate.action = action;
    }

    protected String getPreferenceKey()
    {
        return KEY;
    }

    public static boolean getPreferenceValue()
    {
        return action == null ? getPreferenceValue(KEY) : action.isChecked();
    }
}