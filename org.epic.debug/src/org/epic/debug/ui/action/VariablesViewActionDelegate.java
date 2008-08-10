package org.epic.debug.ui.action;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.*;
import org.epic.debug.DebugTarget;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.db.StackFrame;
import org.epic.perleditor.PerlEditorPlugin;

abstract class VariablesViewActionDelegate
    implements IViewActionDelegate, IActionDelegate2
{
    public void init(IViewPart view)
    {
    }

    public void run(IAction action)
    {
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
    }

    public void dispose()
    {
    }

    public void init(IAction action)
    {
        action.setChecked(getPreferenceValue(getPreferenceKey()));
    }

    public void runWithEvent(IAction action, Event event)
    {
        getPreferenceStore().setValue(
            getPreferenceKey(), action.isChecked());

        updateVariablesView();
    }
    
    /**
     * Key under which the checked/unchecked state of the option
     * is stored in the preferences.
     */
    protected abstract String getPreferenceKey();
    
    protected final static boolean getPreferenceValue(String key)
    {
        return getPreferenceStore().getBoolean(key);
    }
    
    protected final void updateVariablesView()
    {
        IDebugTarget[] targets =
            DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
        
        for (int i = 0; i < targets.length; i++)
        {
            if (!(targets[i] instanceof DebugTarget)) continue;
            DebugTarget target = (DebugTarget) targets[i];
            
            try
            {
                IThread[] threads = target.getThreads();
                IStackFrame[] frames = threads[0].getStackFrames();
    
                for (int j = 0; j < frames.length; j++)
                    ((StackFrame) frames[j]).discardCachedVars();
            }
            catch (DebugException e)
            {
                PerlDebugPlugin.log(e);
            }
        }
    }
    
    private static IPreferenceStore getPreferenceStore()
    {
        return
            PerlEditorPlugin.getDefault().getPreferenceStore();
    }
}
