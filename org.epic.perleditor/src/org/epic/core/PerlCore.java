package org.epic.core;

import org.eclipse.core.resources.IProject;

/**
 * Provides access to PerlProject instances.
 * 
 * @author jploski
 */
public class PerlCore
{
    private PerlCore() { }
    
    /**
     * @return a PerlProject instance wrapping the given project;
     *         note that no check that the project has a Perl nature
     *         occurs during this invocation
     */
    public static PerlProject create(IProject project)
    {
        return new PerlProject(project);
    }
}