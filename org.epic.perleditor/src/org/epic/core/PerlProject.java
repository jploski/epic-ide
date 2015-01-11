package org.epic.core;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

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
     *         this project, a superset of those returned by {@link #getIncPath}
     */
    public List<File> getEffectiveIncPath() throws CoreException
    {
        PerlExecutor executor = new PerlExecutor();
        
        try
        {
            final String perlCode = "foreach $i(@INC) { print \"$i\n\"; }\n";
            List<String> lines = executor.execute(
                this,
                Collections.<String>emptyList(),
                perlCode).getStdoutLines();
            
            return makeAbsIncPath(
                lines.toArray(new String[lines.size()]));
        }
        finally
        {
            executor.dispose();
        }
    }
    
    /**
     * @return an unmodifiable list of regex Patterns representing paths to be
     *         ignored by syntax checking. These are the entries explicitly configured
     *         in the project's properties.
     */
    public List<Pattern> getIgnoredPaths()
    {
        String[] elems = new XMLUtilities().getIgnoredEntries(project);
        if (elems.length > 0)
        {        
            List<Pattern> patterns = new ArrayList<Pattern>(elems.length);
            
            for (int i = 0; i < elems.length; i++)
            {
                String elem = quotemeta(elems[i].replace('\\', '/')).replaceAll("\\\\\\*", ".*?").trim();
                if (elem.length() > 0) patterns.add(Pattern.compile('^' + elem + ".*", Pattern.DOTALL));
            }
            
            return Collections.unmodifiableList(patterns);
        }
        else return Collections.emptyList();
    }
    
    /**
     * @return an unmodifiable list of File objects representing directories
     *         in the project's include path. These are the entries explicitly
     *         configured in the project's properties.
     * @see {@link #getEffectiveIncPath}
     */
    public List<File> getIncPath()
    {        
        XMLUtilities xmlUtil = new XMLUtilities();
        return makeAbsIncPath(xmlUtil.getIncludeEntries(project, true));
    }

    /**
     * @return an unmodifiable list of Strings representing directories
     *         in the project's include path. These are the exact entries
     *         explicitly configured in the project's properties.
     * @see {@link #getIncPath}
     */
    public List<String> getRawIncPath()
    {
        XMLUtilities xmlUtil = new XMLUtilities();
        return Arrays.asList(xmlUtil.getIncludeEntries(project, true));
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
    
    private List<File> makeAbsIncPath(String[] relIncPath)
    {
        List<File> dirs = new ArrayList<File>();
        File projectDir = getProjectDir();

        for (int i = 0; i < relIncPath.length; i++)
        {
            File f = new File(relIncPath[i]);
            if (!f.isAbsolute()) f = new File(projectDir, relIncPath[i]);
            dirs.add(f); 
        }        
        return Collections.unmodifiableList(dirs);
    }

    /**
     * Roughly equivalent to Perl's quotemeta.
     */
    private static final String quotemeta(String str)
    {
        int len = str.length();
        StringBuffer buf = new StringBuffer(2*len);

        for (int i = 0; i < str.length(); i++)
        {
            char c = str.charAt(i);
            if(!Character.isLetterOrDigit(c)) buf.append('\\');
            buf.append(c);
        }

        return buf.toString();
    }
}