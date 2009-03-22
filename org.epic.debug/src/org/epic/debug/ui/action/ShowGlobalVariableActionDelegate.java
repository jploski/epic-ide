package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;
import org.epic.perleditor.preferences.PreferenceConstants;

public class ShowGlobalVariableActionDelegate extends VariablesViewActionDelegate
{
    public static String ACTION_ID = "org.epic.debug.showGlobalVariablesAction";
    public static String KEY = PreferenceConstants.DEBUG_SHOW_GLOBAL_VARS;
    private static IAction action;

    public void init(IAction action)
    {
        super.init(action);
        ShowGlobalVariableActionDelegate.action = action;
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