package org.epic.perleditor.actions;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.epic.core.util.MarkerUtilities;
import org.epic.perleditor.editors.PerlEditor;


/**
 * Abstract base class that provides a workflow to run an editor action in a background user thread
 * that may be cancelled, and optionally create markers on the resource the action was invoked on.
 */
public abstract class PerlUserJobAction extends PerlEditorAction
{
    //~ Constructors

    protected PerlUserJobAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods
    
    /**
     * Checks preconditions (e.g., related to configuration settings)
     * that must be true in order to run this action. This is run in
     * foreground. If a broken precondition is detected, the subclass
     * should display an informative error message for the user and
     * return false to prevent the background job from running.
     */
    protected boolean checkJobPreconditions()
    {
        return true;
    }

    /**
     * Performs the actual job work.
     *
     * <p>If markers should be created on the resource, an array of objects containing the marker
     * data should be returned. The individual objects in the array will be passed into
     * {@link #createMarkerAttributes(MarkerUtilities, Object)} so the marker attributes can be
     * created.</p>
     *
     * <p>If no markers need to be created, an empty array should be returned.</p>
     *
     * @param resource resource the job is performed on
     *
     * @return array of objects containing data to create markers or an empty array if no markers
     *         are to be created.
     */
    protected abstract Object[] doJob(IResource resource);

    /**
     * @return the job title - the passed IResource object can be used to obtain information (ie:
     *         name) that can be included in the job title
     */
    protected abstract String getJobTitle(IResource resource);

    /**
     * Creates the marker attribute map.
     *
     * <p>Sub-classes may override if markers need to be created. The <code>set</code> methods in
     * the <code>MarkerUtilities</code> class should be used to set any atttribute data that will
     * appear in the Problems view (line number, message, etc).</p>
     *
     * <p>The default implementation returns an empty map.</p>
     *
     * @param factory
     * @param violation object containing marker data
     *
     * @return marker attribute map
     */
    protected Map<String, Serializable> createMarkerAttributes(MarkerUtilities factory, Object violation)
    {
        return Collections.emptyMap();
    }

    /*
     * @see org.epic.perleditor.actions.PerlEditorAction#doRun()
     */
    protected final void doRun()
    {
        if (!checkJobPreconditions()) return;
        
        // TODO: check if editor is dirty before running
        final String marker = getMarker();
        final IResource resource = getResource();

        final MarkerUtilities factory = new MarkerUtilities(getLog(), getPluginId());

        Job job =
            new Job(getJobTitle(resource))
        {
            protected IStatus run(IProgressMonitor monitor)
            {
                // cancelled while sitting in queue
                if (monitor.isCanceled()) { return Status.CANCEL_STATUS; }

                factory.deleteMarkers(resource, marker);

                /*
                 * this should be able to support multiple resources as well in the future - the
                 * loop would just need to include a check against the monitor for cancellation
                 */
                Object[] violations = doJob(resource);

                // check if we were cancelled while the thread was running
                if (monitor.isCanceled()) { return Status.CANCEL_STATUS; }

                for (int i = 0; i < violations.length; i++)
                {
                    Map<String, Serializable> attributes = createMarkerAttributes(factory, violations[i]);
                    factory.createMarker(resource, marker, attributes);
                }

                return Status.OK_STATUS;
            }
        };

        job.setUser(true);
        job.schedule();
    }

    /**
     * @return name of marker to be created - only override if markers are to be created.
     */
    protected String getMarker()
    {
        return "";
    }
}
