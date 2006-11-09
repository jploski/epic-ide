package org.epic.perleditor.editors.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.epic.core.util.ScriptExecutor;
import org.epic.core.util.StatusFactory;
import org.epic.perleditor.preferences.SourceCriticPreferencePage;


/**
 * Runs metrics against perl code using <code>Perl::Critic</code>
 *
 * @see http://search.cpan.org/dist/Perl-Critic/
 */
public class SourceCritic extends ScriptExecutor
{
    //~ Static fields/initializers

    private static Violation[] EMPTY_ARRAY = new Violation[0];

    //~ Constructors

    protected SourceCritic(ILog log)
    {
        super(log);
    }

    //~ Methods

    public static Violation[] critique(IFile file, ILog log)
    {
        /*
         * it seems that Perl::Critic does not like receiving the editor input when invoked via the
         * perl executor (although it works fine from the command line outside of java land).
         *
         * this work around is ok for now b/c metrics are only run against a single file, but this
         * won't work for entire directories at a time - perhaps a background job that processes
         * each one?
         */
        ArrayList args = new ArrayList(1);
        args.add(file.getRawLocation().toOSString());

        try
        {
            SourceCritic critic = new SourceCritic(log);

            String output = critic.run(args).stdout;
            return critic.parseViolations(output);
        }
        catch (CoreException e)
        {
            log.log(e.getStatus());
            // nothing more we can do
            return EMPTY_ARRAY;
        }
    }

    protected List getCommandLineOpts(List additionalOptions)
    {
        if (additionalOptions == null || additionalOptions.isEmpty())
        {
            additionalOptions = new ArrayList(2);
        }

        additionalOptions.add("-verbose");
        additionalOptions.add("%f~|~%s~|~%l~|~%c~|~%m~|~%e" + getSystemLineSeparator());

        return additionalOptions;
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getExecutable()
     */
    protected String getExecutable()
    {
        return SourceCriticPreferencePage.getPerlCritic();
    }

    protected String getScriptDir()
    {
        return "";
    }

    private final Violation parseLine(String toParse)
    {
        String[] tmp = toParse.split("~\\|~");

        Violation violation = new Violation();

        violation.file = tmp[0];
        violation.severity = parseInt(tmp[1]);
        violation.lineNumber = parseInt(tmp[2]);
        violation.column = parseInt(tmp[3]);
        violation.message = tmp[4];
        violation.pbp = tmp[5];

        return violation;
    }

    private final Violation[] parseViolations(String toParse)
    {
        String separator = getLineSeparator(toParse);

        if ((toParse == null) || "".equals(toParse) || toParse.endsWith("OK" + separator))
        {
            return EMPTY_ARRAY;
        }

        String[] lines = toParse.split(separator);
        Violation[] violations = new Violation[lines.length];
        for (int i = 0; i < lines.length; i++)
        {
            // System.out.println("critic: " + lines[i]);
            violations[i] = parseLine(lines[i]);
        }

        if (violations.length == 0)
        {
            log(StatusFactory.createWarning(getPluginId(),
                    "Perl::Critic violations.length == 0, output change?"));
        }

        return violations;
    }

    public static class Violation
    {
        public int column;
        public String file;
        public int lineNumber;
        public String message;
        public String pbp;
        public int severity;
    }

}