package org.epic.debug.db;

import org.eclipse.core.runtime.CoreException;
import org.epic.debug.PerlDebugPlugin;

/**
 * A simple utility class to load helper scripts from files and
 * customize them by substituting certain sections with values
 * supplied at run time.
 * 
 * @author jploski
 */
class HelperScript
{
    private HelperScript() { }
    
    /**
     * @param scriptName name of a helper script
     * @return source text of the given helper script
     */
    static String load(String scriptName)
    {
        try
        {
            return PerlDebugPlugin.getDefault().loadHelperScript(scriptName);
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return "print \"\"";
        }
    }
    
    /**
     * @param code      source text of a helper script
     * @param tag       tag appearing in the source text which should be replaced
     * @param subst     replacement for the tag
     * @return source text with tag replaced by the provided subst string
     */
    static String replace(String code, String tag, String subst)
    {
        int i = code.indexOf(tag);
        if (i == -1) return code;
        
        StringBuffer buf = new StringBuffer();
        buf.append(code.substring(0, i));
        buf.append(subst);
        buf.append(code.substring(i+tag.length()));
        return buf.toString();
    }
}
