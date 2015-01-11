package org.epic.perleditor.editors.util;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.epic.core.util.*;
import org.epic.perleditor.preferences.PerlCriticPreferencePage;


/**
 * Runs metrics against perl code using <code>Perl::Critic</code>
 *
 * @see http://search.cpan.org/dist/Perl-Critic/
 */
public class SourceCritic extends ScriptExecutor
{
    //~ Static fields/initializers

    private static Violation[] EMPTY_ARRAY = new Violation[0];

    private IResource resource;

    //~ Constructors

    protected SourceCritic(ILog log)
    {
        super(log);
    }

    //~ Methods   

    public static Violation[] critique(IResource resource, ILog log)
    {
        try
        {
            SourceCritic critic = new SourceCritic(log);
            // meh - not sure if i'm happy w/ this, but it's needed in getCommandLineOpts
            critic.resource = resource;

            String output = critic.run(new ArrayList<String>(4)).stdout;
            return critic.parseViolations(output);
        }
        catch (CoreException e)
        {
            log.log(e.getStatus());
            // nothing more we can do
            return EMPTY_ARRAY;
        }
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getCommandLineOpts(java.util.List)
     */
    protected List<String> getCommandLineOpts(List<String> additionalOptions)
    {
        if (additionalOptions == null || additionalOptions.isEmpty())
        {
            additionalOptions = new ArrayList<String>(2);
        }

        // project specific critic config files
        IFile rc = resource.getProject().getFile(".perlcriticrc");
        File rcFile = new File(rc.getRawLocation().toOSString());
        if (rcFile.exists())
        {
            additionalOptions.add("-profile");
            additionalOptions.add(rc.getRawLocation().toOSString());
        }
        
        String severity = PerlCriticPreferencePage.getSeverity();
        if(!severity.equals("default")) 
        {
        	additionalOptions.add("--" + severity);
        }

        additionalOptions.add("--verbose");
        additionalOptions.add("%f~|~%s~|~%l~|~%c~|~%m~|~%e~|~%p~||~%n");
        
        String otherOptions = PerlCriticPreferencePage.getOtherOptions();
        if(otherOptions.length() > 0)
        {
        	additionalOptions.addAll(CommandLineTokenizer.tokenize(otherOptions));
        }

        IFile file = (IFile) resource;
        /*
         * it seems that Perl::Critic does not like receiving the editor input when invoked via the
         * perl executor (although it works fine from the command line outside of java land).
         *
         * this work around is ok for now b/c metrics are only run against a single file, but this
         * won't work for entire directories at a time - perhaps a background job that processes
         * each one?
         */
        additionalOptions.add(file.getRawLocation().toOSString());
        
        return additionalOptions;
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getExecutable()
     */
    protected String getExecutable()
    {
        return PerlCriticPreferencePage.getPerlCritic();
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getScriptDir()
     */
    protected String getScriptDir()
    {
        return "";
    }

    protected File getWorkingDir() throws CoreException
    {
        // Run perlcritic from project folder, not from the the one which
        // contains the script to be checked, in hope that some violations
        // of the sort "this file is in wrong directory" can be avoided that way.
        return new File(resource.getProject().getLocation().toOSString());
    }

    private final Violation parseLine(String toParse)
    {
        String[] tmp = toParse.split("~\\|~");

        // handle cases where a line returned from critic doesn't have all 7 expected fields
        if (tmp.length != 7)
        {
            return null;
        }

        Violation violation = new Violation();

        violation.file = tmp[0];
        violation.severity = parseInt(tmp[1]);
        // Line number and column are sometimes omitted.
        // Avoid logging this as an error.
        violation.lineNumber = "".equals(tmp[2])?-1:parseInt(tmp[2]);
        violation.column = "".equals(tmp[2])?-1:parseInt(tmp[3]);
        violation.message = tmp[4];
        violation.pbp = tmp[5];
        violation.policy = tmp[6];

        return violation;
    }

    private final Violation[] parseViolations(String toParse)
    {
        String separator = getLineSeparator(toParse);

        if ((toParse == null) || "".equals(toParse) || toParse.endsWith("OK" + separator))
        {
            return EMPTY_ARRAY;
        }

        String[] lines = toParse.split("~\\|\\|~" + separator);
        ArrayList<Violation> violations = new ArrayList<Violation>();
        for (int i = 0; i < lines.length; i++)
        {
            System.out.println("critic: " + lines[i]);

            Violation v = parseLine(lines[i]);
            if (v != null)
            {
                violations.add(v);
            }
        }

        if (violations.size() == 0)
        {
            log(StatusFactory.createWarning(getPluginId(),
                    "Perl::Critic violations.length == 0, output change?"));
        }

        return violations.toArray(new Violation[violations.size()]);
    }

    public static class Violation
    {
        public int column;
        public String file;
        public int lineNumber;
        public String message;
        public String pbp;
        public int severity;
        public String policy;
    }

}