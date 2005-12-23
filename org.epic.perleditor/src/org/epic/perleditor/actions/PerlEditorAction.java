package org.epic.perleditor.actions;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;

/**
 * Abstract base class for actions operating in context of a PerlEditor.
 * These actions are instantiated in two contexts:
 * <ol>
 * <li>by each PerlEditor instance (via PerlEditor#createActions),
 *    in which case they are linked to the creator for their life span</li>
 * <li>by the platform (via the action extension point in plugin.xml
 *     and the default constructor), in which case they delegate execution
 *     to the corresponding PerlEditorAction of the active editor</li>
 * </ol>
 * 
 * Subclasses should implement a default public constructor,
 * a constructor taking PerlEditor as argument, and the run method,
 * in which they can obtain the editor via a #getEditor call.
 * 
 * @author jploski
 */
public abstract class PerlEditorAction extends Action
    implements IWorkbenchWindowActionDelegate, IEditorActionDelegate
{
    private PerlEditor editor;
    
    protected PerlEditorAction()
    {
        setId(getPerlActionId());
    }

    protected PerlEditorAction(PerlEditor editor)
    {
        this.editor = editor;
        setId(getPerlActionId());
    }
    
    public final void run(IAction action)
    {
        // Delegate to corresponding action registered in the active PerlEditor

        IEditorPart activeEditor = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        
        if (activeEditor instanceof PerlEditor)
        {
            IAction editorAction =
                ((PerlEditor) activeEditor).getAction(getId());
            editorAction.run();
        }
    }
    
    public void dispose()
    {
    }
    
    public void init(IWorkbenchWindow window)
    {        
    }
    
    public void selectionChanged(IAction action, ISelection selection)
    {
    }
    
    public void setActiveEditor(IAction action, IEditorPart targetEditor)
    {
        // we do nothing here because we just delegate to the editor's
        // own action instances in run(IAction) 
    }
    
    /**
     * @return the PerlEditor on which the action should operate
     */
    protected final PerlEditor getEditor()
    {
        return editor;
    }
    
    /**
     * @return the log that could be used for reporting problems during
     *         the action
     */
    protected ILog getLog()
    {
        return PerlEditorPlugin.getDefault().getLog();
    }
    
    /**
     * @return a constant from PerlEditorActionIds, identifying this action
     */
    protected abstract String getPerlActionId();
}
