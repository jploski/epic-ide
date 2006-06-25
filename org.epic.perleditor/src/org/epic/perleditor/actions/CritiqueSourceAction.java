package org.epic.perleditor.actions;

import org.eclipse.core.runtime.CoreException;
import org.epic.core.util.ProcessOutput;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceCritic;


public class CritiqueSourceAction extends PerlEditorAction
{
    //~ Constructors

    public CritiqueSourceAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        SourceCritic critic = new SourceCritic(getLog());

        try
        {
            ProcessOutput out = critic.run(getEditor().getViewer().getDocument().get());

            System.out.println("error: " + out.stderr);
            System.out.println("stdout: " + out.stdout);
        }
        catch (CoreException e)
        {
            log(e.getStatus());
        }
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.CRITIQUE_SOURCE;
    }

}
