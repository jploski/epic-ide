package org.epic.perleditor.actions;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.epic.core.util.MarkerUtilities;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceCritic;


/**
 */
public abstract class ClearMarkerAction extends PerlEditorAction
{
    //~ Constructors

    protected ClearMarkerAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    public void selectionChanged(IAction action, ISelection selection)
    {
        // empty impl
    }

    protected abstract String getMarkerType();

    protected final void doRun()
    {
        scheduleJob(getMarkerType());
    }

    private void scheduleJob(final String markerType)
    {
        Job job =
            new Job("Clear EPIC Marker(s)")
        {
            protected IStatus run(IProgressMonitor monitor)
            {
                MarkerUtilities factory = new MarkerUtilities(getLog(), getPluginId());
                factory.deleteMarkers(getResource(), markerType);

                return Status.OK_STATUS;
            }
        };

        job.setSystem(true);
        job.schedule();
    }

    //~ Inner Classes

    /**
     */
    public static class AllMarkers extends ClearMarkerAction
    {

        public AllMarkers(PerlEditor editor)
        {
            super(editor);
        }

        protected String getMarkerType()
        {
            return "org.epic.perleditor.markers.epic";
        }

        protected String getPerlEditorActionId()
        {
            return PerlEditorActionIds.CLEAR_ALL_MARKERS;
        }
    }

    /**
     */
    public static class Critic extends ClearMarkerAction
    {
        public Critic(PerlEditor editor)
        {
            super(editor);
        }

        protected String getMarkerType()
        {
            return "org.epic.perleditor.markers.critic";
        }

        protected String getPerlEditorActionId()
        {
            return PerlEditorActionIds.CLEAR_CRITIC_MARKERS;
        }
    }

    /**
     */
    public static class PodChecker extends ClearMarkerAction
    {
        public PodChecker(PerlEditor editor)
        {
            super(editor);
        }

        protected String getMarkerType()
        {
            return "org.epic.perleditor.markers.podChecker";
        }

        protected String getPerlEditorActionId()
        {
            return PerlEditorActionIds.CLEAR_POD_MARKERS;
        }
    }
}
