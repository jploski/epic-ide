package org.epic.debug.db;

import gnu.regexp.REException;
import gnu.regexp.RESyntax;

/**
 * Regular expressions used for parsing "perl -d" output
 */
class RE
{
    final gnu.regexp.RE CMD_FINISHED1;
    final gnu.regexp.RE CMD_FINISHED2;
    final gnu.regexp.RE SESSION_FINISHED1;
    final gnu.regexp.RE SESSION_FINISHED2;
    final gnu.regexp.RE IP_POS;
    final gnu.regexp.RE IP_POS_EVAL;
    final gnu.regexp.RE IP_POS_CODE;
    final gnu.regexp.RE SWITCH_FILE_FAIL;
    final gnu.regexp.RE SET_LINE_BREAKPOINT;
    final gnu.regexp.RE STACK_TRACE;
    final gnu.regexp.RE ENTER_FRAME;
    final gnu.regexp.RE EXIT_FRAME;
    
    public RE()
    {
        CMD_FINISHED1 = newRE("\n\\s+DB<+\\d+>+", false);
        CMD_FINISHED2 = newRE("^\\s+DB<\\d+>", false);
        SESSION_FINISHED1 = newRE("Use `q' to quit or `R' to restart", false);
        SESSION_FINISHED2 = newRE("Debugged program terminated.", false);
        IP_POS = newRE("^[^\\(]*\\((.*):(\\d+)\\):[\\n\\t]", false);
        IP_POS_EVAL = newRE("^[^\\(]*\\(eval\\s+\\d+\\)\\[(.*):(\\d+)\\]$", false);
        IP_POS_CODE = newRE("^.*CODE\\(0x[0-9a-fA-F]+\\)\\((.*):(\\d+)\\):[\\n\\t]", false);
        SWITCH_FILE_FAIL = newRE("^No file", false);
        SET_LINE_BREAKPOINT = newRE("^Line \\d+ not breakable", false);
        ENTER_FRAME = newRE("^\\s*entering", false);
        EXIT_FRAME = newRE("^\\s*exited", false);
        STACK_TRACE = newRE(
            "(.)\\s+=\\s+(.*)called from .* [\\`']([^\\']+)\\'\\s*line (\\d+)\\s*",
            false);
    }

    private gnu.regexp.RE newRE(String re, boolean multiline)
    {
        try
        {
            return new gnu.regexp.RE(
                re,
                multiline ? gnu.regexp.RE.REG_MULTILINE : 0,
                RESyntax.RE_SYNTAX_PERL5);
        }
        catch (REException e)
        {
            // we have a bug in the constructor
            throw new RuntimeException(e);
        }
    }
}
