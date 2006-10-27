package org.epic.perleditor.actions;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.IFileEditorInput;

import org.epic.core.util.MarkerUtilities;

import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceCritic;
import org.epic.perleditor.editors.util.SourceCritic.Violation;


/**
 */
public class CritiqueSourceAction extends PerlEditorAction
{
    //~ Static fields/initializers

    private static String METRICS_MARKER = "org.epic.perleditor.markers.critic";
    private static String MARKER_OWNER = CritiqueSourceAction.class.getName() + ".owner";

    //~ Constructors

    public CritiqueSourceAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        // TODO: check if editor is dirty before running
        IFile file = ((IFileEditorInput) getEditor().getEditorInput()).getFile();
        MarkerUtilities factory = new MarkerUtilities(getLog(), getPluginId());

        factory.deleteMarkers(getResource(), METRICS_MARKER, MARKER_OWNER);

        Violation[] violations = SourceCritic.critique(file, getLog());
        for (int i = 0; i < violations.length; i++)
        {
            createMarker(factory, violations[i]);
        }
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.CRITIQUE_SOURCE;
    }

    private void createMarker(MarkerUtilities factory, Violation violation)
    {
        HashMap attributes = new HashMap();

        factory.setMessage(attributes, violation.message);
        factory.setLineNumber(attributes, violation.lineNumber);
        factory.setSeverity(attributes, IMarker.SEVERITY_WARNING);

        attributes.put("pbp", violation.pbp);

        factory.createMarker(getResource(), METRICS_MARKER, MARKER_OWNER, attributes);
    }

}
