package org.epic.perleditor.actions;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.epic.core.ResourceMessages;
import org.epic.core.builders.PerlCriticBuilderHelper;
import org.epic.core.util.MarkerUtilities;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.preferences.PerlCriticPreferencePage;


/**
 * Runs Perl::Critic
 *
 * @see http://search.cpan.org/dist/Perl-Critic/
 */
public class PerlCriticAction extends PerlUserJobAction
{
    //~ Static fields/initializers

    private PerlCriticBuilderHelper perlCriticBuilderHelper;

    //~ Constructors
    
    public PerlCriticAction(PerlEditor editor)
    {
        super(editor);
        perlCriticBuilderHelper = PerlCriticBuilderHelper.instance();
    }
    
    //~ Methods
    
    protected boolean checkJobPreconditions()
    {
        if (!PerlCriticPreferencePage.isPerlCriticEnabled())
        {
            MessageDialog.openInformation(
                getEditor().getSite().getShell(),
                ResourceMessages.getString(
                    "PerlCriticAction.perlCriticNotEnabledMessage"), //$NON-NLS-1$
                ResourceMessages.getString(
                    "PerlCriticAction.perlCriticNotEnabledMessage.descr")); //$NON-NLS-1$
            return false;
        }
        
        File perlCriticScript = new File(PerlCriticPreferencePage.getPerlCritic());
        if (!perlCriticScript.exists() || !perlCriticScript.isFile())
        {
            MessageDialog.openError(
                getEditor().getSite().getShell(),
                ResourceMessages.getString(
                    "PerlCriticAction.perlCriticMissingMessage"), //$NON-NLS-1$
                ResourceMessages.getString(
                    "PerlCriticAction.perlCriticMissingMessage.descr")); //$NON-NLS-1$
            return false;
        }
        else return true;
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#createMarkerAttributes(org.epic.core.util.MarkerUtilities, java.lang.Object)
     */
    protected Map<String, Serializable> createMarkerAttributes(MarkerUtilities factory, Object violation)
    {
        return perlCriticBuilderHelper.createMarkerAttributes(factory, violation);
    }

    /*
     * @see org.epic.perleditor.actions.PerlJobAction#doJob(org.eclipse.core.resources.IResource)
     */
    protected Object[] doJob(IResource resource)
    {
        return perlCriticBuilderHelper.doJob(resource);
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
        return PerlCriticBuilderHelper.METRICS_MARKER;
    }

    /*
     * @see org.epic.perleditor.actions.PerlEditorAction#getPerlEditorActionId()
     */
    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.PERL_CRITIC;
    }
}
