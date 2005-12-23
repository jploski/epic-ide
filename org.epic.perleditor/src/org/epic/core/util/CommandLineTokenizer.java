package org.epic.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a command line into a list of tokens, one for each component.
 * <p>
 * A component is either a string of non-whitespace characters or a string
 * surrounded with double quotes. In the latter case, contained double quote
 * characters can be escaped with a backslash. Moreover, each double-backslash
 * sequence will be translated into a single backslah. Other character sequences
 * will be output without modification.
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
    public static List tokenize(String commandLine)
    {
        List tokens = new ArrayList();
        
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
        
        if (line.charAt(pos) == '"')
        {
            pos++;
            // find the matching " or end-of-line, watch out for escapes
            boolean escape = false;
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
                    if (c == '\\') escape = true;
                    else if (c == '"') { pos++; break; }
                    else buf.append(c);
                }
                pos++;
            }
            if (escape) buf.append('\\'); // trailing backslash
        }
        else            
        {
            // find the next whitespace or end-of-line, don't care about quotes
            while (pos < line.length())                
            {
                char c = line.charAt(pos);
                if (Character.isWhitespace(c)) break;
                buf.append(c);
                pos++;
            }
        }
        posInOut[0] = pos;
        return buf.toString();
    }
}
