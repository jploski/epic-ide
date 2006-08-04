package org.epic.perleditor.editors.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;

import org.eclipse.jface.preference.IPreferenceStore;

import org.epic.core.util.ScriptExecutor;

import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.preferences.SourceFormatterPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;


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
    {
        return format(toFormat, Collections.EMPTY_LIST, log);
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
    public static String format(String toFormat, List additionalArgs, ILog log)
    {
        try
        {
            return new SourceFormatter(log).run(toFormat, additionalArgs).stdout;
        }
        catch (CoreException e)
        {
            log.log(e.getStatus());

            // return the original text being formatted
            return toFormat;
        }
    }

    /*
     * @see org.epic.core.util.ScriptExecutor#getCommandLineOpts(java.util.List)
     */
    protected List getCommandLineOpts(List additionalOptions)
    {
        IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();

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

        List args = new ArrayList();

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
        StringTokenizer st =
            new StringTokenizer(store.getString(SourceFormatterPreferences.PERLTIDY_OPTIONS));
        while (st.hasMoreTokens())
        {
            args.add(st.nextToken());
        }

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

}
