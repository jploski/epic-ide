package org.epic.core.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.TextUtilities;
import org.epic.perleditor.PerlEditorPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Base class for classes representing executable "helper" Perl scripts shipped with EPIC and their
 * mandatory command line parameters. A subclass instance can be used to execute its script on
 * various inputs, and possibly with additional command line parameters.
 */
public abstract class ScriptExecutor
{
    //~ Instance fields

    private ILog log;

    //~ Constructors

    protected ScriptExecutor(ILog log)
    {
        this.log = log;
    }

    //~ Methods

    /**
     * Runs the script with no stdin and the given additional command line parameters.
     *
     * @return execution results
     */
    public final ProcessOutput run(List<String> args) throws CoreException
    {
        return run("", args);
    }

    /**
     * Runs the script with the given stdin and no additional command line parameters.
     *
     * @param text text passed to script over stdin
     *
     * @return execution results
     */
    public final ProcessOutput run(String text) throws CoreException
    {
        return run(text, null);
    }

    /**
     * Runs the script with the given stdin and additional command line parameters.
     *
     * @param text text passed to script over stdin
     * @param additionalArgs additional command line arguments passed to the script during execution
     *
     * @return execution results
     */
    public final ProcessOutput run(String text, List<String> additionalArgs) throws CoreException
    {
        File workingDir = getWorkingDir();

        PerlExecutor executor = new PerlExecutor(
            getCharsetName(), ignoresBrokenPipe());
        try
        {
            List<String> cmdArgs = new ArrayList<String>(1);
            cmdArgs.add(getExecutable());
            cmdArgs.addAll(getCommandLineOpts(additionalArgs));

            ProcessOutput output = executor.execute(workingDir, cmdArgs, text);

            /*
             * there are times that stderr and stdout are both set, even though an error has not
             * occurred (Devel::Refactor processes exhibit this behavior).
             *
             * given that, logging the stderr as a warning will help the end user to figure out what
             * is going on (and assist in logging bug reports, etc)
             */
            if ((output.stderr != null) && ! output.stderr.equals(""))
            {
                log.log(StatusFactory.createWarning(PerlEditorPlugin.getPluginId(),
                        "Perl Process stderr: " + output.stderr, null));
            }

            return output;
        }
        finally
        {
            executor.dispose();
        }
    }
    
    /**
     * @return name of a supported
     *         {@link java.nio.charset.Charset </code>charset<code>}
     *         which should be used to encode/decode communication with
     *         the Perl interpreter, or null to use platform default
     */
    protected String getCharsetName()
    {
        return null;
    }

    /**
     * @return the name of the script that will be executed
     */
    protected abstract String getExecutable();

    /**
     * @return the directory (relative to the plugin root) containing the script to be executed
     */
    protected abstract String getScriptDir();

    /**
     * Constructs the list of command line options passed to the script at runtime.
     *
     * <p>Default implementation returns the <code>additionalOptions</code> if specified, otherwise
     * an empty list is returned. Sub-classes are free to override to append (or prepend) other
     * script-specific options not provided by the caller.</p>
     *
     * @param additionalOptions additional cmd line arguments
     *
     * @return complete list of command line arguments
     */
    protected List<String> getCommandLineOpts(List<String> additionalOptions)
    {
        return (additionalOptions != null) ? additionalOptions : Collections.<String>emptyList();
    }

    /**
     * @return the system's line seperator, falling back to "\n" if it can not be determined
     */
    protected String getSystemLineSeparator()
    {
        String seperator = System.getProperty("line.separator");

        // in the off chance this could ever happen...
        if (seperator == null)
        {
            seperator = "\n";
        }

        return seperator;
    }

    /**
     * @return the line seperator used in the passed text string
     */
    protected String getLineSeparator(String text)
    {
        return TextUtilities.determineLineDelimiter(text, getSystemLineSeparator());
    }

    protected String getPluginId()
    {
        return PerlEditorPlugin.getPluginId();
    }

    /**
     * @return true if broken pipe exceptions from the executed script should be ignored, false
     *         otherwise (default)
     *
     * @see org.epic.core.util.ProcessExecutor#ignoreBrokenPipe
     */
    protected boolean ignoresBrokenPipe()
    {
        return false;
    }

    protected void log(IStatus status)
    {
        log.log(status);
    }

    protected int parseInt(String s)
    {
        try
        {
            return Integer.valueOf(s).intValue();
        }
        catch (NumberFormatException e)
        {
        	// stacktrace will provide exact error location
            log.log(StatusFactory.createError(getPluginId(), e.getMessage(), e));
            // results in "Unknown" being displayed in the Problems view
            return -1;
        }
    }

    protected File getWorkingDir() throws CoreException
    {
        try
        {
            File scriptsLocation = extractScripts();

            if (scriptsLocation == null)
            {
                URL url =
                    new URL(PerlEditorPlugin.getDefault().getBundle().getEntry("/"),
                        getScriptDir());
                URL workingURL = Platform.resolve(url);
                return new File(workingURL.getPath());
            }

            return new File(scriptsLocation.getParentFile(), getScriptDir());

        }
        catch (IOException e)
        {
            log.log(StatusFactory.createError(getPluginId(), e.getMessage(), e));
            return null;
        }
    }

    private File extractScripts() throws CoreException
    {
        return ResourceUtilities.extractResources(PerlEditorPlugin.getDefault(), "perlutils/");
    }
}