package org.epic.perleditor.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IFileEditorInput;
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

        /*
         * it seems that Perl::Critic does not like receiving the editor input when invoked via the
         * underlying perl executor (although it works fine from the command line).
         *
         * this work around should be ok b/c metrics will only ever be run against the entire source
         * file
         */
        IFile file = ((IFileEditorInput) getEditor().getEditorInput()).getFile();
        ArrayList args = new ArrayList(1);
        args.add(file.getRawLocation().toOSString());

        try
        {
            ProcessOutput out = critic.run("", args);

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
