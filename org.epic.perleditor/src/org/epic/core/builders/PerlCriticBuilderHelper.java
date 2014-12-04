package org.epic.core.builders;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.epic.core.util.MarkerUtilities;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.SourceCritic;
import org.epic.perleditor.editors.util.SourceCritic.Violation;
import org.epic.perleditor.preferences.PerlCriticPreferencePage;


public class PerlCriticBuilderHelper {

    //~ Static fields/initializers

    public static String METRICS_MARKER = "org.epic.perleditor.markers.critic";
	private static PerlCriticBuilderHelper instance;
	private boolean criticResourcePreconditions;
	private boolean criticAutoRunPreconditions;
	
	public PerlCriticBuilderHelper () {
		setCriticResourcePreconditions(checkCriticResourcePreconditions());
		setCriticAutoRunPreconditions(checkCriticAutoRunPreconditions());
	}
	
	private boolean checkCriticAutoRunPreconditions() {
		return (isCriticResourcePreconditions() && PerlCriticPreferencePage.isPerlCriticJobEnabled());
	}

	private boolean checkCriticResourcePreconditions() {
		if (!PerlCriticPreferencePage.isPerlCriticEnabled()) {
			return false;
		}

		File perlCriticScript = new File(PerlCriticPreferencePage
				.getPerlCritic());
		if (!perlCriticScript.exists() || !perlCriticScript.isFile()) {
			return false;
		} else
			return true;
	}

    /**
     * @return the PerlCricitBuilderHelper singleton
     */
    public synchronized static PerlCriticBuilderHelper instance()
    {
        if (instance == null) instance = new PerlCriticBuilderHelper();
        return instance;
    }

    /**
     * invalidates the PerlCricitBuilderHelper singleton
     */
    public synchronized static void destroy()
    {
        instance = null;
    }
    
    /**
     * Attempts to validate a single Perl file, schedules label update
     * for this file and its ancestor folders if validation was performed.
     */
    public void buildResource(IResource resource)
    {
    	
        Object[] violations = doJob(resource);

        final MarkerUtilities factory = new MarkerUtilities(PerlEditorPlugin.getDefault().getLog(), PerlEditorPlugin.getPluginId());
        factory.deleteMarkers(resource, METRICS_MARKER);

        for (int i = 0; i < violations.length; i++)
        {
            Map<String, Serializable> attributes = createMarkerAttributes(factory, violations[i]);
            factory.createMarker(resource, METRICS_MARKER, attributes);
        }
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#doJob(org.eclipse.core.resources.IResource)
     */
    public Object[] doJob(IResource resource)
    {
        return SourceCritic.critique(resource, PerlEditorPlugin.getDefault().getLog());
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#createMarkerAttributes(org.epic.core.util.MarkerUtilities, java.lang.Object)
     */
    public Map<String, Serializable> createMarkerAttributes(MarkerUtilities factory, Object violation)
    {
        Map<String, Serializable> attributes = new HashMap<String, Serializable>();
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
        factory.setMessage(attributes, v.message + " (" + v.pbp + ") [" + v.policy + "]");
        factory.setLineNumber(attributes, v.lineNumber);
        factory.setSeverity(attributes, getSeverity(v.severity));

        attributes.put("pbp", v.pbp);

        return attributes;
    }
    
    public int getSeverity(int severity)
    {
        return PerlCriticPreferencePage.getMarkerSeverity(severity);
    }

	public void setCriticAutoRunPreconditions(boolean criticAutoRunPreconditions) {
		this.criticAutoRunPreconditions = criticAutoRunPreconditions;
	}

	public boolean isCriticAutoRunPreconditions() {
		return criticAutoRunPreconditions;
	}

	public void setCriticResourcePreconditions(boolean criticResourcePreconditions) {
		this.criticResourcePreconditions = criticResourcePreconditions;
	}

	public boolean isCriticResourcePreconditions() {
		return criticResourcePreconditions;
	}

}
