package org.epic.debug.ui.action;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.epic.debug.DebugTarget;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.db.StackFrame;
import org.epic.perleditor.PerlEditorPlugin;

public abstract class VariablesViewActionDelegate
    implements IViewActionDelegate, IActionDelegate2
{
    private static Map<String, String> actionIdToPreferenceKey;

    public synchronized static void enableVariablesViewActions()
    {
        initActionIdToPreferenceKey();
            
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow[] window = workbench.getWorkbenchWindows();
        for (int i = 0; i < window.length; i++)
        {
            final IWorkbenchPage page = window[i].getActivePage();
            if (page != null) Display.getDefault().asyncExec(new EnableVariablesViewActions(page));
        }
    }

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

    public void init(final IAction action)
    {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                action.setChecked(getPreferenceValue(getPreferenceKey()));
            }
        });
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
    
    private static void initActionIdToPreferenceKey()
    {
        if (actionIdToPreferenceKey != null) return;
        actionIdToPreferenceKey = new HashMap<String, String>();

        actionIdToPreferenceKey.put(
            HighlightVarUpdatesActionDelegate.ACTION_ID,
            HighlightVarUpdatesActionDelegate.KEY);

        actionIdToPreferenceKey.put(
            ShowGlobalVariableActionDelegate.ACTION_ID,
            ShowGlobalVariableActionDelegate.KEY);

        actionIdToPreferenceKey.put(
            ShowLocalVariableActionDelegate.ACTION_ID,
            ShowLocalVariableActionDelegate.KEY);

        actionIdToPreferenceKey.put(
            ShowPerlInternalVariableActionDelegate.ACTION_ID,
            ShowPerlInternalVariableActionDelegate.KEY);

        actionIdToPreferenceKey.put(
            ShowVarAddressActionDelegate.ACTION_ID,
            ShowVarAddressActionDelegate.KEY);
    }
    
    private static class EnableVariablesViewActions implements Runnable
    {
        private final IWorkbenchPage page;
    
        public EnableVariablesViewActions(IWorkbenchPage page)
        {
            this.page = page;
        }

        public void run()
        {
            IViewPart variablesView = page.findView("org.eclipse.debug.ui.VariableView");
            if (variablesView == null) return;
            
            IContributionItem[] item = variablesView.getViewSite().getActionBars().getMenuManager().getItems();
            for (int i = 0; i < item.length; i++)
            {
                String prefKey = actionIdToPreferenceKey.get(item[i].getId());
                if (prefKey != null && item[i] instanceof ActionContributionItem)
                {
                    IAction action = ((ActionContributionItem) item[i]).getAction();
                    if (action != null)
                    {
                        action.setEnabled(true);
                        action.setChecked(getPreferenceValue(prefKey));
                    }
                }
            }
        }
    }
}