package org.epic.perleditor.actions;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionDelegate;

import org.epic.perleditor.editors.PerlEditor;


/**
 * Generic implementation of the <code>IEditorActionDelegate</code>.
 * Delegates its run method to a corresponding action in the active PerlEditor. 
 *
 * <p>Retargetable editor actions implemented as menu items may specify this class as
 * the <code>class</code> attribute in the <code>action</code> configuration element of the
 * plugin.xml file.</p>
 *
 * <p>This class may be sub-classed to alter the behavior of the editor action menu item
 * in the ui.</p>
 */
public class DefaultRetargetEditorDelegate extends ActionDelegate implements IEditorActionDelegate
{
    //~ Instance fields

    private PerlEditor editor;

    //~ Methods

    public final void run(IAction action)
    {
        IAction toRun = editor.getAction(action.getId());
        assert toRun != null;

        toRun.run();
    }

    public void setActiveEditor(IAction action, IEditorPart targetEditor)
    {
        if (targetEditor == null) { return; }

        assert targetEditor instanceof PerlEditor;
        editor = (PerlEditor) targetEditor;
    }

}
