package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.epic.debug.db.PerlDB;

public class HighlightVarUpdatesActionDelegate implements IViewActionDelegate, org.eclipse.ui.IActionDelegate2
{
    private static IAction action;

    public void init(IViewPart view)
    {
    }

    public void init(IAction action)
    {
        HighlightVarUpdatesActionDelegate.action = action;
    }

    public void dispose()
    {
    }

    public void runWithEvent(IAction action, Event event)
    {
        PerlDB.updateVariablesView();
    }

    public void run(IAction action)
    {
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
    }

    public static boolean getPreferenceValue()
    {
        return action == null ? false : action.isChecked();
    }

    public void update()
    {
    }
}