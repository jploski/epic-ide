package org.epic.perleditor.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.epic.core.util.MarkerUtilities;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;

public class ClearMarkerAction implements IEditorActionDelegate
{
    private PerlEditor editor;

    public void setActiveEditor(IAction action, IEditorPart targetEditor)
    {
        editor = (PerlEditor) targetEditor;
    }

    public void run(IAction action)
    {
        // XXX: works ok for specific markers, but needs refactoring to work as a "clear all"
        MarkerUtilities factory = new MarkerUtilities(getLog(), getPluginId());

        System.out.println("action id" + action.getActionDefinitionId());
        factory.deleteProblemMarkers(getResource(), action.getActionDefinitionId());
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
        // empty impl
    }

    protected ILog getLog()
    {
        return PerlEditorPlugin.getDefault().getLog();
    }

    protected String getPluginId()
    {
        return PerlEditorPlugin.getPluginId();
    }

    /**
     * @return returns the resource on which to create the marker, or <code>null</code> if there is
     *         no applicable resource.
     */
    protected IResource getResource()
    {
        return (IResource) ((IAdaptable) editor.getEditorInput()).getAdapter(IResource.class);
    }
}
