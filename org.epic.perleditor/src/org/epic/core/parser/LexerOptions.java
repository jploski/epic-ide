package org.epic.core.parser;

import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * A utility class which allows the lexer to refer to options from the editor's preferences page
 */
public class LexerOptions
{
    private LexerOptions()
    {
    }

    /**
     * Returns whether or not to use the Method::Signature handling for lexing
     *
     */
    public static boolean useMethodSignatures()
    {
		return PerlEditorPlugin.getDefault().getBooleanPreference(PreferenceConstants.DEBUG_METHOD_SIGNATURES);
    }

}
