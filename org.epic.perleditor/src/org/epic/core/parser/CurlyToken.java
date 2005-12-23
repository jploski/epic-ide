package org.epic.core.parser;

/**
 * Token class for representing opening and closing curly braces ('{' and '}').
 * Adds a <code>level</code> attribute (0 = top-level curlies, 1 = first-level
 * of nesting, etc.) 
 * 
 * @author jploski
 */
public class CurlyToken extends PerlToken
{
    private int level;

    public CurlyToken(int t, String txt, int level)
    {
        super(t, txt);
        this.level = level;
    }
    
    public void setLevel(int level)
    {
        this.level = level;
    }
    
    public int getLevel()
    {
        return level;
    }
    
    public String toString()
    {
        return super.toString() + ",level=" + level;
    }
}
