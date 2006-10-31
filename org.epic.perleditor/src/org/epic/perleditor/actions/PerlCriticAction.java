package org.epic.perleditor.actions;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.ui.IFileEditorInput;

import org.epic.core.util.MarkerUtilities;

import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceCritic;
import org.epic.perleditor.editors.util.SourceCritic.Violation;


/**
 * Runs Perl::Critic
 *
 * @see http://search.cpan.org/dist/Perl-Critic/
 */
public class PerlCriticAction extends PerlEditorAction
{
    //~ Static fields/initializers

    private static String METRICS_MARKER = "org.epic.perleditor.markers.critic";

    //~ Constructors

    public PerlCriticAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        // TODO: check if editor is dirty before running
        final IFile file = ((IFileEditorInput) getEditor().getEditorInput()).getFile();
        final MarkerUtilities factory = new MarkerUtilities(getLog(), getPluginId());

        Job job = new Job("Executing Perl::Critic against " + file.getName())
        {
            protected IStatus run(IProgressMonitor monitor)
            {
                // cancelled while sitting in queue
                if (monitor.isCanceled()) { return Status.CANCEL_STATUS; }

                factory.deleteMarkers(getResource(), METRICS_MARKER);
                Violation[] violations = SourceCritic.critique(file, getLog());

                // check if we were cancelled while the thread was running
                if (monitor.isCanceled()) { return Status.CANCEL_STATUS; }

                for (int i = 0; i < violations.length; i++)
                {
                    createMarker(factory, violations[i]);
                }

                return Status.OK_STATUS;
            }
        };

        job.setUser(true);
        job.schedule();
    }

    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.PERL_CRITIC;
    }

    private void createMarker(MarkerUtilities factory, Violation violation)
    {
        HashMap attributes = new HashMap();

        /*
         * XXX: including the violation as part of the message is only temporary
         *
         * future enhancements should have the pbp info displayed as part of the marker annotation,
         * or inside some kind of view that can be spawned to explain the critic warning.
         *
         * inclusion of a link to safari would be useful as well, but that depends on missing
         * Perl::Critic functionality
         */
        factory.setMessage(attributes, violation.message + " (" + violation.pbp + ")");
        factory.setLineNumber(attributes, violation.lineNumber);
        factory.setSeverity(attributes, IMarker.SEVERITY_WARNING);

        attributes.put("pbp", violation.pbp);

        factory.createMarker(getResource(), METRICS_MARKER, attributes);
    }

}
