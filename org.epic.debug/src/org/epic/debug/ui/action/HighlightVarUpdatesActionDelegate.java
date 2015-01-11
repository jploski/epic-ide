package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;
import org.epic.perleditor.preferences.PreferenceConstants;

public class HighlightVarUpdatesActionDelegate extends VariablesViewActionDelegate
{
    public static String ACTION_ID = "org.epic.debug.highlightVarUpdatesAction";
    public static String KEY = PreferenceConstants.DEBUG_HIGHLIGHT_UPDATED_VARS;

    private static IAction action;

    public void init(IAction action)
    {
        super.init(action);
        HighlightVarUpdatesActionDelegate.action = action;
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