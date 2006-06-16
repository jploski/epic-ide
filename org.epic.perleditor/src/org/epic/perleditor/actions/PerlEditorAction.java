package org.epic.perleditor.actions;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.action.Action;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;

/**
 * Abstract base class for actions operating in context of a PerlEditor.
 * These actions are created and owned by each PerlEditor instance
 * (through {@link org.epic.perleditor.editors.PerlEditor#createActions}).
 * They are (indirectly) bound to GUI controls by PerlActionContributor.
 * 
 * @author jploski
 */
public abstract class PerlEditorAction extends Action
{
    private final PerlEditor editor;

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
