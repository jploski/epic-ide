package org.epic.perleditor.actions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.epic.core.util.MarkerUtilities;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.PodChecker;
import org.epic.perleditor.editors.util.PodChecker.Violation;


/**
 * Runs Pod::Checker
 *
 * @see http://search.cpan.org/dist/Pod-Parser/lib/Pod/Checker.pm
 */
public class PodCheckerAction extends PerlUserJobAction
{
    //~ Static fields/initializers

    private static final String POD_MARKER = "org.epic.perleditor.markers.podChecker";

    //~ Constructors

    public PodCheckerAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    /*
     * @see
     * org.epic.perleditor.actions.PerlJobAction#createMarkerAttributes(org.epic.core.util.MarkerUtilities,
     * java.lang.Object)
     */
    protected Map<String, Serializable> createMarkerAttributes(MarkerUtilities factory, Object violation)
    {
        Map<String, Serializable> attributes = new HashMap<String, Serializable>();
        Violation v = (Violation) violation;

        int severity;
        if ("error".equalsIgnoreCase(v.severity))
        {
            severity = IMarker.SEVERITY_ERROR;
        }
        else
        {
            severity = IMarker.SEVERITY_WARNING;
        }

        factory.setMessage(attributes, v.message);
        factory.setSeverity(attributes, severity);
        factory.setLineNumber(attributes, v.lineNumber);

        return attributes;
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#doJob(org.eclipse.core.resources.IResource)
     */
    protected Object[] doJob(IResource resource)
    {
        return PodChecker.podchecker(resource, getLog());
    }

    /*
     * @see
     * org.epic.perleditor.actions.PerlJobAction#getJobTitle(org.eclipse.core.resources.IResource)
     */
    protected String getJobTitle(IResource resource)
    {
        return "Executing Pod::Checker against " + resource.getName();
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#getMarker()
     */
    protected String getMarker()
    {
        return POD_MARKER;
    }

    /*
     * @see org.epic.perleditor.actions.PerlEditorAction#getPerlEditorActionId()
     */
    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.POD_CHECKER;
    }

}
