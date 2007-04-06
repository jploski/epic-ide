package org.epic.perleditor.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.epic.core.util.MarkerUtilities;

import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceCritic;
import org.epic.perleditor.editors.util.SourceCritic.Violation;
import org.epic.perleditor.preferences.PerlCriticPreferencePage;

import java.util.HashMap;
import java.util.Map;


/**
 * Runs Perl::Critic
 *
 * @see http://search.cpan.org/dist/Perl-Critic/
 */
public class PerlCriticAction extends PerlUserJobAction
{
    //~ Static fields/initializers

    private static String METRICS_MARKER = "org.epic.perleditor.markers.critic";

    //~ Constructors

    public PerlCriticAction(PerlEditor editor)
    {
        super(editor);
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#createMarkerAttributes(org.epic.core.util.MarkerUtilities, java.lang.Object)
     */
    protected Map createMarkerAttributes(MarkerUtilities factory, Object violation)
    {
        Map attributes = new HashMap();
        Violation v = (Violation) violation;

        /*
         * XXX: including the violation as part of the message is only temporary
         *
         * future enhancements should have the pbp info displayed as part of the marker annotation, or
         * inside some kind of view that can be spawned to explain the critic warning.
         *
         * inclusion of a link to safari would be useful as well, but that depends on missing
         * Perl::Critic functionality
         */
        factory.setMessage(attributes, v.message + " (" + v.pbp + ")");
        factory.setLineNumber(attributes, v.lineNumber);
        factory.setSeverity(attributes, getSeverity(v.severity));

        attributes.put("pbp", v.pbp);

        return attributes;
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#doJob(org.eclipse.core.resources.IResource)
     */
    protected Object[] doJob(IResource resource)
    {
        return SourceCritic.critique(resource, getLog());
    }

    /*
     * @see
     * org.epic.perleditor.actions.PerlJobAction#getJobTitle(org.eclipse.core.resources.IResource)
     */
    protected String getJobTitle(IResource resource)
    {
        return "Executing Perl::Critic against " + resource.getName();
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#getMarker()
     */
    protected String getMarker()
    {
        return METRICS_MARKER;
    }

    /*
     * @see org.epic.perleditor.actions.PerlEditorAction#getPerlEditorActionId()
     */
    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.PERL_CRITIC;
    }

    private int getSeverity(int severity)
    {
        return PerlCriticPreferencePage.getMarkerSeverity(severity);
    }

}
