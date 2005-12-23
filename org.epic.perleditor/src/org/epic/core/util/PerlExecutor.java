package org.epic.core.util;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.core.PerlCore;
import org.epic.core.PerlProject;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Responsible for execution of external, non-interactive Perl processes
 * which expect input in form of command-line parameters and stdin and
 * provide output through stdout and/or stderr.
 * 
 * @author jploski
 */
public class PerlExecutor
{
    private boolean disposed;
    private final ProcessExecutor executor;
    
    /**
     * Creates a new PerlExecutor.
     */
    public PerlExecutor()
    {
        this(false);
    }
    
    /**
     * This constructor helps with testing, other clients should not use it.
     */
    public PerlExecutor(ProcessExecutor executor)
    {
        this.executor = executor;
    }
    
    /**
     * This constructor is for PerlValidator's use, other clients should
     * have no need to use it.
     * 
     * @param ignoreBrokenPipe
     *        see {@link ProcessExecutor#ignoreBrokenPipe}
     */
    public PerlExecutor(boolean ignoreBrokenPipe)
    {
        executor = new ProcessExecutor(); // TODO: charset?
        if (ignoreBrokenPipe) executor.ignoreBrokenPipe();
    }
    
    /**
     * Releases resources held by this PerlExecutor.
     * This PerlExecutor must no longer be used after dispose. 
     */
    public void dispose()
    {
        disposed = true;
        executor.dispose();
    }
    
    /**
     * Executes the Perl interpreter in the given working directory
     * with the given command-line parameters and input.
     * The execution is project-neutral, controlled only by global preferences.
     * 
     * @param workingDir    working directory for the interpreter
     * @param args          command-line arguments (Strings)
     * @param input         input passed to the interpreter via stdin
     */
    public ProcessOutput execute(File workingDir, List args, String input)
        throws CoreException
    {
        if (disposed) throw new IllegalStateException("PerlExecutor disposed");
        
        List commandLine = PerlExecutableUtilities.getPerlCommandLine();
        if (args != null) commandLine.addAll(args);
        
        try
        {
            return executor.execute(commandLine, input, workingDir);
        }
        catch (InterruptedException e) { throwCoreException(e); return null;}
        catch (IOException e) { throwCoreException(e, commandLine); return null; }
    }
    
    /**
     * Executes a Perl script within the context of the given Perl project.
     * Project settings control the execution.
     * 
     * @param project     project
     * @param args        additional command-line arguments for the Perl interpreter,
     *                    or null if none
     * @param sourceCode  source code of the script
     */
    public ProcessOutput execute(PerlProject project, List args, String sourceCode)
        throws CoreException
    {
        return execute(project.getProject(), args, sourceCode);
    }
    
    /**
     * Executes a Perl script contained in the given resource.
     * This resource is asusmed to be contained in a Perl project.
     * Project settings control the execution.
     * 
     * @param resource    script resource
     * @param args        additional command-line arguments for the Perl interpreter,
     *                    or null if none
     * @param sourceCode  source code of the script
     */
    public ProcessOutput execute(IResource resource, List args, String sourceCode)
        throws CoreException
    {
        if (disposed) throw new IllegalStateException("PerlExecutor disposed");       
        if (sourceCode.length() < 1) return new ProcessOutput("", "");
        
        PerlProject project = PerlCore.create(resource.getProject());
        List commandLine = getPerlCommandLine(project);
        if (args != null) commandLine.addAll(args);

        try
        {
            return executor.execute(
                commandLine, sourceCode, getPerlWorkingDir(resource));
        }
        catch (InterruptedException e) { throwCoreException(e); return null;}
        catch (IOException e) { throwCoreException(e, commandLine); return null; }
    }
    
    /**
     * Executes a Perl script within the context of the given text editor.
     * This method is a shorthand for {@link #execute(IResource, List, String)}.
     * 
     * @param editor      parent folder of the edited file is used
     *                    as working directory for the Perl interpreter
     * @param args        additional command-line arguments for the Perl interpreter,
     *                    or null if none
     * @param sourceCode  source code of the script
     */
    public ProcessOutput execute(ITextEditor editor, List args, String sourceCode)
        throws CoreException
    {
        return execute(
            ((IFileEditorInput) editor.getEditorInput()).getFile(),
            args,
            sourceCode);
    }

    protected List getPerlCommandLine(PerlProject project)
    {
        return PerlExecutableUtilities.getPerlCommandLine(project);
    }
    
    protected File getPerlWorkingDir(IPath resourceLocation)
    {
        return new File(
            resourceLocation
                .makeAbsolute()
                .removeLastSegments(1)
                .toString());
    }
    
    protected File getPerlWorkingDir(IResource resource)
    {
        return getPerlWorkingDir(resource.getLocation());        
    }
    
    private void throwCoreException(InterruptedException e)
        throws CoreException
    {
        // InterruptedExceptions during execute happen when the operation
        // is aborted (and therefore fails). They should not occur during
        // normal operation, but do not necessarily indicate misconfigurations
        // or bugs.
        
        Status status = new Status(
            Status.WARNING,
            PerlEditorPlugin.getPluginId(),
            IStatus.OK,
            "Execution of a Perl process was aborted.",
            e);
        throw new CoreException(status);
    }
    
    private void throwCoreException(IOException e, List commandLine)
        throws CoreException
    {
        // An IOException during execute means that the Perl process could
        // either not start (most likely misconfiguration) or aborted in
        // an unexpected manner (which is beyond our control). We report
        // this as a severe error.
        
        Status status = new Status(
            Status.ERROR,
            PerlEditorPlugin.getPluginId(),
            IStatus.OK,
            "Failed to execute command line: " + getCommandLineString(commandLine),
            e);
        throw new CoreException(status);
    }
    
    private String getCommandLineString(List commandLine)
    {
        StringBuffer buf = new StringBuffer();
        
        for (Iterator i = commandLine.iterator(); i.hasNext();)
        {
            if (buf.length() > 0) buf.append(' ');            
            String str = (String) i.next();
            str = '"' + str.replaceAll("\"", "\\\"") + '"';
            buf.append(str);
        }
        return buf.toString();
    }
}