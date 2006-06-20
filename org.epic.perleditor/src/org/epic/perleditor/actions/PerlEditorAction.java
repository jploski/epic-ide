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
 * Note: Subclasses must implement a default constructor.
 * <p>
 * Instances of subclasses can be created in two different contexts:
 * <ol>
 * <li>by each PerlEditor instance (through
 *     {@link org.epic.perleditor.editors.PerlEditor#createActions})</li>
 * <li>by the Eclipse platform (e.g. in case of popup menu contributions)</li>
 * </ol>
 * When an action is run, the specific work is always performed by the instances
 * owned by PerlEditor. The control flow can reach their {@link #run} method
 * in two ways, which is abstracted away by PerlEditorAction:
 * <ol>
 * <li>from a platform-managed RetargetAction, which uses the id-action
 *     mapping established by PerlActionContributor</li>
 * <li>from a PerlEditorAction instance created by the platform in
 *     the role of {@link org.eclipse.ui.IActionDelegate}</li> 
 * </ol>
 * 
 * @author jploski
 */
public abstract class PerlEditorAction extends Action
    implements IEditorActionDelegate
{
    private PerlEditor editor;
    
    /**
     * Constructor used by the platform, e.g. for popup menu contributions.
     */
    protected PerlEditorAction()
    {
    }

    /**
     * Constructor used by PerlEditor.
     */
    protected PerlEditorAction(PerlEditor editor)
    {
        this.editor = editor;
        setId(getPerlActionId());
    }
    
    /**
     * Invoked by PerlEditor to dispose of the action instance.
     * Subclasses may override this method to provide clean-up.
     */
    public void dispose()
    {
    }
    
    public void run(IAction action)
    {
        // Delegate to the corresponding action registered in the active PerlEditor

        IAction editorAction = editor.getAction(getId());
        editorAction.run();
    }
    
    public void selectionChanged(IAction action, ISelection selection)
    {
    }
    
    public void setActiveEditor(IAction action, IEditorPart targetEditor)
    {
        editor = (PerlEditor) targetEditor;
    }
    
    /**
     * @return the PerlEditor in which the action operates
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
     * @return a constant from PerlEditorActionIds which identifies this action
     */
    protected abstract String getPerlActionId();
}
