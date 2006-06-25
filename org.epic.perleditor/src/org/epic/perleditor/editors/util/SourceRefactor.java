package org.epic.perleditor.editors.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;

import org.epic.core.util.ScriptExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Refactors perl code using <code>perlutils/refactor/refactor.pl</code>
 */
public class SourceRefactor extends ScriptExecutor
{
    //~ Static fields/initializers

    public static final String DELIMITER = "<CODE_CALL>";

    //~ Constructors

    protected SourceRefactor(ILog log)
    {
        super(log);
    }

    //~ Methods

    /**
     * extract a method (subroutine) from a code snippet
     *
     * @param subName method name to create
     * @param codeSnippet method body
     * @param ILog log instance
     *
     * @return string array containing the method call in [0] and the method body in [1]
     */
    public static String[] extractMethod(String subName, String codeSnippet, ILog log)
    {
        try
        {
            String result = new SourceRefactor(log).run(buildArgs(subName, codeSnippet)).stdout;
            return result.split(DELIMITER);
        }
        catch (CoreException e)
        {
            log.log(e.getStatus());
            return new String[] {};
        }
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getExecutable()
     */
    protected String getExecutable()
    {
        return "refactor.pl";
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getScriptDir()
     */
    protected String getScriptDir()
    {
        return "perlutils/refactor";
    }

    private static List buildArgs(String subName, String codeSnippet)
    {
        ArrayList cmdArgs = new ArrayList(3);
        cmdArgs.add(subName.replaceAll("'", "\\\\'"));
        cmdArgs.add(codeSnippet.replaceAll("'", "\\\\'"));
        cmdArgs.add(DELIMITER);

        return cmdArgs;
    }

}
