package org.epic.core.util;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.core.PerlCore;
import org.epic.core.PerlProject;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Responsible for the basic command lines used to invoke the Perl interpreter.
 * 
 * @author luelljoc
 * @author jploski
 */
public class PerlExecutableUtilities
{
    private static final Pattern CYGWIN_PATH_TRANSLATION =
        Pattern.compile("^([a-z]):(.*)$"); 
    
    private PerlExecutableUtilities() { }
    
    /**
     * @return a list of Strings representing the command line used to invoke
     *         the Perl interpreter, according to EPIC's global preferences;
     *         if EPIC is set up properly, the returned list should at
     *         the very least contain a path to the interpreter's executable 
     */
    public static List<String> getPerlCommandLine()
    {
        String perlExe = PerlEditorPlugin.getDefault().getPerlExecutable();
        
        try
        {
            IStringVariableManager varMgr = 
                VariablesPlugin.getDefault().getStringVariableManager();
            
            perlExe = varMgr.performStringSubstitution(perlExe);
        }
        catch (CoreException e)
        {
            PerlEditorPlugin.getDefault().getLog().log(
                new Status(Status.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Variable substitution failed for Perl executable. " +
                    "The literal value \"" + perlExe + "\" will be used. " +
                    "Check Perl executable in EPIC Preferences.",
                    e));
        }
        
        return new ArrayList<String>(CommandLineTokenizer.tokenize(perlExe));
    }

    /**
     * @param textEditor    editor for a resource within a Perl project
     * @return a list of Strings representing the command line used to invoke
     *         the Perl for scripts belonging to the enclosing project 
     */
    public static List<String> getPerlCommandLine(ITextEditor textEditor)
    {
        IProject project = 
            ((IFileEditorInput) textEditor.getEditorInput())
            .getFile().getProject();

        return getPerlCommandLine(PerlCore.create(project));
    }
    
    /**
     * @param project       a Perl project
     * @return a list of Strings representing the command line used to invoke
     *         the Perl interpreter for scripts belonging to this project.
     *         This equals the command line returned by
     *         {@link #getPerlCommandLine()} with the project-specific
     *         include path appended to it. 
     */
    public static List<String> getPerlCommandLine(PerlProject project)
    {
        List<String> commandLine = getPerlCommandLine();        
        commandLine.addAll(getPerlIncArgs(project));
        return commandLine;
    }

    /**
     * @param project   a Perl project
     * @return a list of command-line parameters (Strings) representing
     *         the project's include path; the directories are
     *         translated for Cygwin if necessary
     */
    public static List<String> getPerlIncArgs(PerlProject project)
    {    
        boolean cygwin = isCygwinInterpreter();
        List<String> args = new ArrayList<String>();
        
        for (Iterator<File> i = project.getIncPath().iterator(); i.hasNext();)
        {
            String path = i.next().getAbsolutePath();
            // replace '\\' by '/' due to problems with Brazil
            path = path.replace('\\', '/');
            if (cygwin) path = translatePathForCygwin(path);
            args.add("-I" + path);
        }
        return args;
    }

    /**
     * @return path to the Perl interpreter's executable,
     *         according to EPIC's global preferences,
     *         or null if no path has been configured yet
     */
    public static String getPerlInterpreterPath()
    {
        List<String> commandLine = getPerlCommandLine();
        if (commandLine.isEmpty()) return null;
        return (String) commandLine.get(0).toString().replace('\\', '/');
    }
    
    private static boolean isCygwinInterpreter()
    {
        String type = PerlEditorPlugin.getDefault()
            .getPreferenceStore().getString(
                PreferenceConstants.DEBUG_INTERPRETER_TYPE);
        
        return type.equals(PreferenceConstants.DEBUG_INTERPRETER_TYPE_CYGWIN);
    }
    
    /**
     * @param absolute path to some directory,
     *        as returned by File.getAbsolutePath 
     * @return the same path normalized to / as separators and
     *         translated for Cygwin, if necessary
     */
    public static String resolveIncPath(String path)
    {
        path = path.replace('\\', '/');
        if (isCygwinInterpreter()) path = translatePathForCygwin(path);
        return path;
    }
    
    // package-scope visibility to enable testing
    static String translatePathForCygwin(String path)
    {
        path = path.replace('\\', '/');
        path = path.toLowerCase();

        Matcher m = CYGWIN_PATH_TRANSLATION.matcher(path);
        
        if (m.matches())
        {
            StringBuffer buf = new StringBuffer();
            buf.append(CygwinMount.drivePrefix());
            buf.append(m.group(1));
            buf.append(m.group(2));
            return buf.toString();
        }
        else return path;
    }
}
