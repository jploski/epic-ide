package org.epic.core;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.epic.core.util.PerlExecutor;
import org.epic.core.util.XMLUtilities;

/**
 * Represents a Perl project managed by EPIC, or a project in the workspace
 * about to become a Perl project.
 * <p>
 * The primary responsibility of this class is providing access to
 * the project's include path and preferences needed in context of
 * various plug-in functions.
 * </p>
 * 
 * @author jploski
 */
public class PerlProject extends PlatformObject
{
    private final IProject project; 
    
    /**
     * Clients should use {@link PerlCore#create(IProject)} instead of
     * this constructor.
     */
    PerlProject(IProject project)
    {
        this.project = project;
    }
    
    /**
     * @return a list of File objects representing directories in
     *         the project's effective include path. These are the entries
     *         seen by the Perl interpreter when executing scripts from
     *         this project, a superset of those returned by {@link #getIncPath};
     *         (however, only existing directories are returned)
     */
    public List getEffectiveIncPath() throws CoreException
    {
        PerlExecutor executor = new PerlExecutor();
        
        try
        {
            final String perlCode = "foreach $i(@INC) { print \"$i\n\"; }\n";
            List lines = executor.execute(
                this,
                Collections.EMPTY_LIST,
                perlCode).getStdoutLines();
            
            return makeAbsIncPath(
                (String[]) lines.toArray(new String[lines.size()]));
        }
        finally
        {
            executor.dispose();
        }
    }
    
    /**
     * @return an unmodifiable list of File objects representing directories
     *         in the project's include path. These are the entries explicitly
     *         configured in the project's properties (however, only existing
     *         directories are returned).
     * @see {@link #getEffectiveIncPath}
     */
    public List getIncPath()
    {        
        XMLUtilities xmlUtil = new XMLUtilities();
        return makeAbsIncPath(xmlUtil.getIncludeEntries(project, true));
    }
    
    /**
     * @return the IProject interface for this PerlProject
     */
    public IProject getProject()
    {
        return project;
    }
    
    /**
     * @return the project's directory in the file system
     */
    public File getProjectDir()
    {
        // isn't there a better way to obtain it?...
        return project.getFile(
            new Path("x")).getRawLocation().toFile().getParentFile();
    }
    
    private List makeAbsIncPath(String[] relIncPath)
    {
        List dirs = new ArrayList();
        File projectDir = getProjectDir();

        for (int i = 0; i < relIncPath.length; i++)
        {
            File f = new File(relIncPath[i]);
            if (!f.isAbsolute()) f = new File(projectDir, relIncPath[i]);
            if (f.exists() && f.isDirectory()) dirs.add(f); 
        }        
        return Collections.unmodifiableList(dirs);
    }
}