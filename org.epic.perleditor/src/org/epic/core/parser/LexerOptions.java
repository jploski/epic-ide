package org.epic.core.parser;

import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Constants used as keys for preferences in org.epic.perleditor plugin's
 * preference store. This class is also responsible for setting the
 * "factory defaults" for all preferences.
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
