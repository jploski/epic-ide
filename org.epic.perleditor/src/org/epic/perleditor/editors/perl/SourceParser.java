package org.epic.perleditor.editors.perl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;

/**
 * Used to match interesting patterns in source code.
 */
public class SourceParser
{
    /**
     * Pattern used to match line comments.
     */
	public static final Pattern COMMENT_PATTERN = Pattern.compile(
        "#.*?$",
        Pattern.MULTILINE);
    
    public static final int DO_NOT_DELETE_COMMENT_POD = 0;
    public static final int DELETE_COMMENT = 1;
    public static final int DELETE_POD = 2;

    public SourceParser()
    {
    }

    /**
     * Gets elements from sourcecode by using regular expressions.
     * 
     * @param document
     * @param regexp
     * @param preFix
     * @param postFix
     * @param deleteComments
     * @return
     */
    public static List<SourceElement> getElements(
        IDocument document,
        String regexp,
        String preFix,
        String postFix,
        boolean deleteComments)
    {
        return getElements(
            document.get(),
            regexp,
            preFix,
            postFix,
            deleteComments);
    }

    /**
     * Gets elements from sourcecode by using regular expressions.
     * 
     * @param text
     * @param regexp
     * @param preFix
     * @param postFix
     * @param deleteComments
     * @param posWordOnly
     *            (postioning for word-only or for complete line)
     * @return
     */
    public static List<SourceElement> getElements(
        String text,
        String regexp,
        String preFix,
        String postFix,
        boolean deleteComments)
    {
        return getElements(
            text,
            regexp,
            preFix,
            postFix, 
            deleteComments
                ? (DELETE_COMMENT|DELETE_POD)
                : DO_NOT_DELETE_COMMENT_POD);
    }

    /**
     * @param text
     * @param regexp
     * @param preFix
     * @param postFix
     * @param flags
     * @return
     */
    public static List<SourceElement> getElements(
        String text,
        String regexp,
        String preFix,
        String postFix,
        int flags)
    {
        text = blankPODAndComments(
            text,
            (flags & DELETE_POD) == DELETE_POD,
            (flags & DELETE_COMMENT) == DELETE_COMMENT);        

        Pattern p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        List<SourceElement> results = new ArrayList<SourceElement>();
        
        while (m.find())
        {
            int start, end;
            
            if (m.groupCount() > 0) { start = m.start(1); end = m.end(1); }
            else { start = m.start(); end = m.end(); }
            
            StringBuilder buf = new StringBuilder();
            buf.append(preFix);
            buf.append(text.substring(start, end));
            buf.append(postFix);
            results.add(new SourceElement(buf.toString(), start, end-start));
        }
        return results;           
    }
    
    /**
     * Blanks characters in [start, end) with spaces while preserving
     * '\r' and '\n' so that character offsets, line numbers, and '^' / '$'
     * regex anchors remain 100% intact for subsequent full-text searches.
     */
    private static void blankCharRange(char[] text, int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            char c = text[i];
            if (c != '\r' && c != '\n') text[i] = ' ';
        }
    }
    
    private static boolean isAsciiLetter(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    
    /**
     * Substitutes POD blocks and/or comments with blanks.
     * 
     * @param blankPOD          true, if POD blocks should be blanked
     * @param blankComments     true, if inline comments should be blanked
     * @return text text with substitutions
     */
    public static String blankPODAndComments(
        String text,
        boolean blankPOD,
        boolean blankComments)
    {
        if (!blankPOD && !blankComments) return text;

        char[] textChars = text.toCharArray();
        int len = text.length();

        if (blankPOD)
        {
            boolean inPod = false;
            int lineStart = 0;

            while (lineStart < len)
            {
                // Find end of current line (before \r or \n)
                int lineEnd = lineStart;
                while (lineEnd < len && textChars[lineEnd] != '\r' && textChars[lineEnd] != '\n')
                {
                    lineEnd++;
                }

                // Next line start (skipping \r, \n, or \r\n)
                int nextLineStart = lineEnd;
                if (nextLineStart < len && textChars[nextLineStart] == '\r') nextLineStart++;
                if (nextLineStart < len && textChars[nextLineStart] == '\n') nextLineStart++;

                if (!inPod)
                {
                    if (isPodStart(textChars, lineStart, lineEnd))
                    {
                        inPod = true;
                    }
                }

                if (inPod)
                {
                	boolean cut = isCutLine(textChars, lineStart, lineEnd);
                    blankCharRange(textChars, lineStart, nextLineStart);
                    if (cut)
                    {
                        inPod = false;
                    }
                }

                lineStart = nextLineStart;
            }
        }

        if (blankComments)
        {
            Matcher m = COMMENT_PATTERN.matcher(new String(textChars));
            while (m.find())
            {
                blankCharRange(textChars, m.start(), m.end());
            }
        }

        return String.valueOf(textChars);
    }

    /**
     * Checks if a line is a valid Perl '=cut' command.
     * Per perlpodspec: Any line starting with '=cut' ends POD. Trailing text
     * (e.g. '=cut # comment') is ignored, but words like '=cute' are not '=cut'.
     */
    private static boolean isCutLine(char[] chars, int start, int end)
    {
        if (end - start < 4) return false;
        if (chars[start] != '=' || chars[start + 1] != 'c' ||
            chars[start + 2] != 'u' || chars[start + 3] != 't')
        {
            return false;
        }
        if (end - start > 4 && isAsciiLetter(chars[start + 4])) return false;

        return true;
    }

    /**
     * Per perlpodspec: A POD block starts with a line matching m/\A=[a-zA-Z]/
     * that is not a '=cut' command.
     */
    private static boolean isPodStart(char[] chars, int start, int end)
    {
        if (end - start < 2 || chars[start] != '=' || !isAsciiLetter(chars[start + 1])) return false;
        return !isCutLine(chars, start, end);
    }
}