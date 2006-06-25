package org.epic.perleditor.editors.util;

import org.eclipse.core.runtime.ILog;
import org.epic.core.util.ScriptExecutor;


/**
 * Runs metrics against perl code using <code>Perl::Critic</code>
 *
 * @see http://search.cpan.org/dist/Perl-Critic/
 */
public class SourceCritic extends ScriptExecutor
{
    public SourceCritic(ILog log)
    {
        super(log);
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getExecutable()
     */
    protected String getExecutable()
    {
        // TODO: install in perlmodules
        return "/usr/bin/perlcritic";
    }

    protected String getScriptDir()
    {
        return "";
    }
    
    protected boolean ignoresBrokenPipe()
    {
        return true;
    }
}
