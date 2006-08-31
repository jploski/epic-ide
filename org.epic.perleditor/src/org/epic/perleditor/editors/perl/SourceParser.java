package org.epic.perleditor.editors.perl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;
import org.epic.core.model.ISourceElement;

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
        Pattern.MULTILINE | Pattern.DOTALL);
    
    /**
     * Pattern used to match POD comments.
     */
    public static final Pattern POD_PATTERN = Pattern.compile(
        "^=[^c]?[^u]?[^t]?.*?$\\r?\\n?" +  // match the =something line,
        "^.*?$*" +                         // lines between =something and =cut
        "^=cut$\\r?\\n?",                  // match the =cut line
        Pattern.MULTILINE | Pattern.DOTALL);

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
	public static List getElements(
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
    public static List getElements(
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
    public static List getElements(
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
        List results = new ArrayList();
        
        while (m.find())
        {
            int start, end;
            
            if (m.groupCount() > 0) { start = m.start(1); end = m.end(1); }
            else { start = m.start(); end = m.end(); }
            
            StringBuffer buf = new StringBuffer();
            buf.append(preFix);
            buf.append(text.substring(start, end));
            buf.append(postFix);
            results.add(new SourceElement(buf.toString(), start, end-start));
        }
        return results;           
	}
    
    private static void blankCharRange(char[] text, int start, int end)
    {
        for (int i = start; i < end; i++) text[i] = ' ';
    }
    
    /**
     * Substitutes POD blocks and/or comments with blanks.
     * 
     * @param blankPOD          true, if POD blocks should be blanked
     * @param blankComments     true, if inline comments should be blanked
     * @return text text with substitutions
     */
    private static String blankPODAndComments(
        String text,
        boolean blankPOD,
        boolean blankComments)
    {
        if (!blankPOD && !blankComments) return text;
        
        char[] textChars = text.toCharArray();
        
        if (blankPOD)
        {
            Matcher m = POD_PATTERN.matcher(text);
            while (m.find()) blankCharRange(textChars, m.start(), m.end());
        }
        if (blankComments)
        {
            Matcher m = COMMENT_PATTERN.matcher(text);
            while (m.find()) blankCharRange(textChars, m.start(), m.end());
        }
        return String.valueOf(textChars);
    }
    
    private static class SourceElement implements ISourceElement
    {
        private final String name;
        private final int offset;
        private final int length;
        
        public SourceElement(String name, int offset, int length)
        {
            this.name = name;
            this.offset = offset;
            this.length = length;
        }

        public int getLength()
        {
            return length;
        }

        public String getName()
        {
            return name;
        }

        public int getOffset()
        {
            return offset;
        }
    }
}