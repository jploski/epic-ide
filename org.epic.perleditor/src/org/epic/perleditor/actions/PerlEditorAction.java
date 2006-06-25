package org.epic.perleditor.actions;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;


/**
 * Abstract base class for actions operating in context of a PerlEditor.
 *
 * @author jploski
 */
public abstract class PerlEditorAction extends Action
{
    //~ Instance fields

    private PerlEditor editor;

    //~ Constructors

    protected PerlEditorAction(PerlEditor editor)
    {
        assert editor != null;
        this.editor = editor;

        setId(getPerlActionId());
    }

    //~ Methods

    public final void run()
    {
        doRun();
    }

    protected abstract void doRun();

    /**
     * Invoked by PerlEditor to dispose of the action instance. Subclasses may override this method
     * to provide clean-up.
     */
    public void dispose()
    {
        // empty impl
    }

    /**
     * @return a constant from PerlEditorActionIds which identifies this action
     */
    protected abstract String getPerlActionId();

    /**
     * @return the PerlEditor in which the action operates
     */
    protected final PerlEditor getEditor()
    {
        return editor;
    }

    protected void log(IStatus status)
    {
        getLog().log(status);
    }

    /**
     * @return the log that could be used for reporting problems during the action
     */
    protected ILog getLog()
    {
        return PerlEditorPlugin.getDefault().getLog();
    }
}
