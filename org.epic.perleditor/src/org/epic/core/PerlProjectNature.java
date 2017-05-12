/*
 * Created on Apr 5, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.core;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * @author luelljoc
 */
public class PerlProjectNature implements IProjectNature {

    private IProject project;

    private static final String PLUGIN_ID = "org.epic.perleditor";

    public void configure() throws CoreException {
        String builderId = PLUGIN_ID + ".perlbuilder";
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();
        boolean found = false;

        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(builderId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            //add builder to project
            ICommand command = desc.newCommand();
            command.setBuilderName(builderId);
            ICommand[] newCommands = new ICommand[commands.length + 1];

            // Add it before other builders.
            System.arraycopy(commands, 0, newCommands, 1, commands.length);
            newCommands[0] = command;
            desc.setBuildSpec(newCommands);
            project.setDescription(desc, null);
        }
    }

    public void deconfigure() throws CoreException {
        // NOOP
    }

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject fProject) {
        project = fProject;
    }

}
