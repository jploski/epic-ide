package org.epic.perleditor.editors.util;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.epic.core.util.ScriptExecutor;

public class PodChecker extends ScriptExecutor
{

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

    public static void podchecker(IFile file, ILog log)
    {
        ArrayList args = new ArrayList(1);
        args.add(file.getRawLocation().toOSString());

        try
        {
        PodChecker checker = new PodChecker(log);

            String output = checker.run(args).stdout;
            checker.parseViolations(output);
        }
        catch (CoreException e)
        {
        }

    }

    private void parseViolations(String output)
    {
        System.out.println(output);
    }

}
