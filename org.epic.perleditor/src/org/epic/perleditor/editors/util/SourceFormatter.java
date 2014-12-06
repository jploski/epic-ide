package org.epic.perleditor.editors.util;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.epic.core.util.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.preferences.SourceFormatterPreferences;

/**
 * Formats perl source code using PerlTidy
 *
 * @see http://perltidy.sourceforge.net
 */
public class SourceFormatter extends ScriptExecutor
{
    //~ Constructors

    protected SourceFormatter(ILog log)
    {
        super(log);
    }

    //~ Methods

    /**
     * format perl source code
     *
     * @param toFormat source to format
     * @param log log instance
     *
     * @return newly formatted source code, or the original source code if the source could not be
     *         formatted
     */
    public static String format(String toFormat, ILog log)
        throws CoreException
    {
        return format(toFormat, Collections.<String>emptyList(), log);
    }

    /**
     * format perl source code
     *
     * @param toFormat source to format
     * @param additionalArgs additional arguments that may be passed to the command line
     * @param log log instance
     *
     * @return newly formatted source code, or the original source code if the source could not be
     *         formatted
     */
    public static String format(String toFormat, List<String> additionalArgs, ILog log)
        throws CoreException
    {
        ProcessOutput out = new SourceFormatter(log).run(toFormat, additionalArgs);
        
        if (out.stdout.startsWith("skipping file: "))
        {
            String error = truncateString(out.stdout, 160);
            throw new SourceFormatterException(new Status(
                IStatus.ERROR,
                PerlEditorPlugin.getPluginId(),
                IStatus.OK,
                "perltidy returned unexpected output via stdout, " +
                "possibly due to invalid options in Source Formatter " +
                "preferences:\n\n" + error,
                null),
                out.stdout);
        }
        else if (out.stderr.length() != 0)
        {
            String error = truncateString(out.stderr, 320);
            throw new SourceFormatterException(new Status(
                IStatus.ERROR,
                PerlEditorPlugin.getPluginId(),
                IStatus.OK,
                "perltidy returned unexpected output via stderr, " +
                "possibly due to invalid options in Source Formatter " +
                "preferences:\n\n" + error,
                null),
                out.stdout);
        }
        else return out.stdout;
    }
    
    protected String getCharsetName()
    {
        return "utf8";
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getCommandLineOpts(java.util.List)
     */
    protected List<String> getCommandLineOpts(List<String> additionalOptions)
    {
        IPreferenceStore store = new ChainedPreferenceStore(new IPreferenceStore[] {
            EditorsUI.getPreferenceStore(),
            PerlEditorPlugin.getDefault().getPreferenceStore() });

        int numSpaces = store.getInt(PreferenceConstants.INSERT_TABS_ON_INDENT);
        boolean useSpaces = store.getBoolean(PreferenceConstants.SPACES_INSTEAD_OF_TABS);
        int tabWidth = useSpaces ? numSpaces : store.getInt(PreferenceConstants.EDITOR_TAB_WIDTH);
        int pageSize = store.getInt(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);

        boolean cuddleElse = store.getBoolean(SourceFormatterPreferences.CUDDLED_ELSE);
        boolean bracesLeft = store.getBoolean(SourceFormatterPreferences.BRACES_LEFT);
        boolean lineUpParentheses =
            store.getBoolean(SourceFormatterPreferences.LINE_UP_WITH_PARENTHESES);
        boolean swallowOptionalBlankLines =
            store.getBoolean(SourceFormatterPreferences.SWALLOW_OPTIONAL_BLANK_LINES);

        // int containerTightnessBraces =
        // store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_BRACES);
        // int containerTightnessParentheses =
        // store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_PARENTHESES);
        // int containerTightnessSquareBrackets =
        // store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_SQUARE_BRACKETS);

        List<String> args = new ArrayList<String>();

        // args.add("perltidy");
        args.add("-q");
        args.add("--indent-columns=" + tabWidth);
        args.add("--maximum-line-length=" + pageSize);
        // cmdList.add("--brace-tightness=" + containerTightnessBraces);
        // cmdList.add("--paren-tightness=" + containerTightnessParentheses);
        // cmdList.add("--square-bracket-tightness=" + containerTightnessSquareBrackets);

        if (!useSpaces)
        {
            args.add("--entab-leading-whitespace=" + tabWidth);
        }

        if (cuddleElse)
        {
            args.add("--cuddled-else");
        }

        if (bracesLeft)
        {
            args.add("--opening-brace-on-new-line");
        }

        if (lineUpParentheses)
        {
            args.add("--line-up-parentheses");
        }

        if (swallowOptionalBlankLines)
        {
            args.add("--swallow-optional-blank-lines");
        }

        // Read additional options
        args.addAll(CommandLineTokenizer.tokenize(
            store.getString(SourceFormatterPreferences.PERLTIDY_OPTIONS)));

        // Add additionally passed options
        if (additionalOptions != null)
        {
            args.addAll(additionalOptions);
        }

        return args;
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getExecutable()
     */
    protected String getExecutable()
    {
        return "perltidy";
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getScriptDir()
     */
    protected String getScriptDir()
    {
        return "perlutils/perltidy";
    }
    
    private static String truncateString(String str, int maxLen)
    {
        if (maxLen < str.length())
            return str.substring(0, Math.min(str.length(), maxLen)) + "...";
        else
            return str;
    } 
}
