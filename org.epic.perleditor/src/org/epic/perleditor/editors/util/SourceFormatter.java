package org.epic.perleditor.editors.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.epic.core.util.PerlExecutor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.preferences.SourceFormatterPreferences;

public class SourceFormatter
{
    public String doConversion(String text) throws CoreException
    {
        return doConversion(text, null);
    }

    public String doConversion(String text, List additionalOptions) throws CoreException
    {
        if (text == null) return "";
        
        File workingDir = null;
        
        try { workingDir = getWorkingDir(); }
        catch (IOException e)
        {            
            e.printStackTrace(); // TODO log it
            return text;
        }
        
        PerlExecutor executor = new PerlExecutor();
        try
        {
            return executor.execute(
                workingDir,
                getCommandLineArgs(additionalOptions),
                text).stdout;
        }
        finally { executor.dispose(); }
    }
    
    private List getCommandLineArgs(List additionalOptions)
    {
        IPreferenceStore store =
            PerlEditorPlugin.getDefault().getPreferenceStore();

        int numSpaces = store.getInt(PreferenceConstants.INSERT_TABS_ON_INDENT);
        boolean useTabs = store.getBoolean(PreferenceConstants.SPACES_INSTEAD_OF_TABS);
        int tabWidth = useTabs ? store.getInt(PreferenceConstants.EDITOR_TAB_WIDTH) : numSpaces;
        int pageSize = store.getInt(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);

        boolean cuddleElse = store.getBoolean(SourceFormatterPreferences.CUDDLED_ELSE);
        boolean bracesLeft = store.getBoolean(SourceFormatterPreferences.BRACES_LEFT);
        boolean lineUpParentheses = store.getBoolean(SourceFormatterPreferences.LINE_UP_WITH_PARENTHESES);
        boolean swallowOptionalBlankLines = store.getBoolean(SourceFormatterPreferences.SWALLOW_OPTIONAL_BLANK_LINES);

        // int containerTightnessBraces =
        // store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_BRACES);
        // int containerTightnessParentheses =
        // store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_PARENTHESES);
        // int containerTightnessSquareBrackets =
        // store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_SQUARE_BRACKETS);

        List args = new ArrayList();

        args.add("perltidy");
        args.add("--indent-columns=" + tabWidth);
        args.add("--maximum-line-length=" + pageSize);
        // cmdList.add("--brace-tightness=" + containerTightnessBraces);
        // cmdList.add("--paren-tightness=" +
        // containerTightnessParentheses);
        // cmdList.add("--square-bracket-tightness=" +
        // containerTightnessSquareBrackets);

        if (useTabs) args.add("--entab-leading-whitespace=" + tabWidth);
        if (cuddleElse) args.add("--cuddled-else");
        if (bracesLeft) args.add("--opening-brace-on-new-line");
        if (lineUpParentheses) args.add("--line-up-parentheses");
        if (swallowOptionalBlankLines) args.add("--swallow-optional-blank-lines");

        // Read additional options
        StringTokenizer st = new StringTokenizer(
            store.getString(SourceFormatterPreferences.PERLTIDY_OPTIONS));
        while (st.hasMoreTokens()) args.add(st.nextToken());

        // Add additionally passed options
        if (additionalOptions != null) args.addAll(additionalOptions);

        return args;
    }
    
    private File getWorkingDir() throws IOException
    {
        try
        {
            URL installURL = PerlEditorPlugin.getDefault().getBundle().getEntry("/");
            URL perlTidyURL = Platform.resolve(new URL(installURL, "perlutils/perltidy"));
            return new File(perlTidyURL.getPath());
        }
        catch (MalformedURLException e)
        {
            // TODO log it, should never happen
            e.printStackTrace();
            return null;
        }
    }
}
