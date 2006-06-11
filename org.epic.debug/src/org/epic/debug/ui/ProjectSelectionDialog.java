package org.epic.debug.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.epic.debug.PerlDebugPlugin;

/**
 * A dialog used to select a project from a list of all available Perl
 * projects. The list is filtered on the fly according to the user's input.
 */
public class ProjectSelectionDialog extends ElementListSelectionDialog
{
    private static final String PERL_NATURE_ID = "org.epic.perleditor.perlnature";

    public ProjectSelectionDialog(Shell shell)
    {
        super(shell, new ProjectLabelProvider());

        setTitle("Project Selection");
        setMessage("Choose a project");
        setElements(getPerlProjects());
    }

    /**
     * @return list of Perl project names in the current workspace
     */
    private String[] getPerlProjects()
    {
        List projectList = new ArrayList();
        IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        for (int i = 0; i < projects.length; i++)
        {
            IProject project = projects[i];
            try
            {
                if (project.isAccessible() && project.hasNature(PERL_NATURE_ID))
                    projectList.add(project.getName());
            }
            catch (CoreException e)
            {
                PerlDebugPlugin.log(e);
            }
        }
        return (String[]) projectList.toArray(new String[projectList.size()]);
    }

    private static class ProjectLabelProvider extends LabelProvider
    {
        public Image getImage(Object element)
        {
            return AbstractUIPlugin.imageDescriptorFromPlugin(
                PerlDebugPlugin.getDefault().toString(),
                "icons/project_folder.gif").createImage();
        }
    }
}
