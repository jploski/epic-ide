package org.epic.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a command line into a list of tokens, one for each component.
 * <p>
 * A component is a sequence of
 * 1) consecutive non-whitespace characters or
 * 2) non-double-quote characters surrounded by double quotes or
 * 3) an arbitrary sequence of 1) and/or 2)
 * </p>
 * <p>
 * Double quote characters can be escaped with a backslash to appear
 * literally in the component. Moreover, each double-backslash will
 * be appear as a literal single backslah. Other two-character
 * sequences will be output without modification.
 * </p>
 * <p>
 * This tokenizing rules are needed to sensibly interpret
 * a command line specification such as: --foo="one two three",
 * just like a Unix shell would (but without considering special
 * characters known to the shell).
 * </p>
 * 
 * @author jploski
 */
public class CommandLineTokenizer
{
    private CommandLineTokenizer() { }
    
    /**
     * See class description.
     * 
     * @return the list of Strings representing command line components
     */
    public static List<String> tokenize(String commandLine)
    {
        List<String> tokens = new ArrayList<String>();
        
        int[] pos = new int[] { 0 };
        String token;
        
        while ((token = readToken(commandLine, pos)) != null) tokens.add(token); 
        return tokens;
    }
    
    private static String readToken(String line, int[] posInOut)
    {
        int pos = posInOut[0];
        
        // skip whitespace
        while (
            pos < line.length() &&
            Character.isWhitespace(line.charAt(pos))) pos++;
        
        if (pos == line.length()) return null;
        
        StringBuffer buf = new StringBuffer();
        boolean escape = false, inQuote = false;

        // find the next whitespace or end-of-line, don't care about quotes
        while (pos < line.length())                
        {
            char c = line.charAt(pos);
            if (escape)
            {
                if (c == '"') buf.append('"'); // escape quote
                else if (c == '\\') buf.append('\\'); // double backslash
                else { buf.append('\\'); buf.append(c); } // other backslash
                escape = false;
            }
            else
            {
                if (inQuote)
                {
                    if (c == '\\') escape = true;
                    else if (c == '"') inQuote = false;
                    else buf.append(c);
                }
                else
                {
                    if (c == '"') inQuote = true;
                    else if (c == '\\') escape = true;
                    else if (Character.isWhitespace(c)) break;
                    else buf.append(c);
                }
            }
            pos++;
        }
        if (escape) buf.append('\\'); // trailing backslash
        posInOut[0] = pos;
        return buf.toString();
    }
}
