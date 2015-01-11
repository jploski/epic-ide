package org.epic.perleditor.editors.util;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.epic.core.util.ScriptExecutor;

public class PodChecker extends ScriptExecutor
{
    private static Violation[] EMPTY_ARRAY = new Violation[0];

    protected PodChecker(ILog log)
    {
        super(log);
    }

    protected String getExecutable()
    {
        return "epicPodChecker.pl";
    }

    protected String getScriptDir()
    {
        return "perlutils/epicScripts";
    }

    public static Violation[] podchecker(IResource resource, ILog log)
    {
        ArrayList<String> args = new ArrayList<String>(1);
        args.add(((IFile) resource).getRawLocation().toOSString());

        try
        {
            PodChecker checker = new PodChecker(log);

            String output = checker.run(args).stdout;
            return checker.parseViolations(output);
        }
        catch (CoreException e)
        {
            return EMPTY_ARRAY;
        }

    }

    private final Violation[] parseViolations(String toParse)
    {
        if ((toParse == null) || "".equals(toParse))
        {
            return EMPTY_ARRAY;
        }

        String[] lines = toParse.split(getLineSeparator(toParse));
        Violation[] violations = new Violation[lines.length];
        for (int i = 0; i < lines.length; i++)
        {
            System.out.println("critic: " + lines[i]);
            violations[i] = parseLine(lines[i]);
        }

        return violations;
    }

    private Violation parseLine(String toParse)
    {
        String[] tmp = toParse.split("~\\|~");

        Violation violation = new Violation();

        violation.file = tmp[0];
        violation.severity = tmp[1];
        violation.lineNumber = parseInt(tmp[2]);
        violation.message = tmp[3];

        return violation;
    }

    public static class Violation
    {
        public String file;
        public int lineNumber;
        public String severity;
        public String message;
    }

}
