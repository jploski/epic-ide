package org.epic.core.parser;

import antlr.CommonToken;

/**
 * Base class for all token types emitted by PerlMultiLexer.
 * Adds offset attribute and an equality relation to the base class.
 * WARNING: PerlTokens lack fixed identity and are modifiable, for this
 * reason great care must be exercised if they are used as keys in a hash
 * table.
 * 
 * @author jploski
 */
public class PerlToken extends CommonToken
{
    private int offset;
    
    public PerlToken()
    {
    }

    public PerlToken(int t, String txt)
    {
        super(t, txt);
    }

    public PerlToken(String s)
    {
        super(s);
    }
    
    public boolean equals(Object obj)
    {
        if (!(obj instanceof PerlToken)) return false;
        
        PerlToken pt = (PerlToken) obj;
        return
            this.offset == pt.offset &&
            getLength() == pt.getLength() &&
            getType() == pt.getType();
    }

    public int getLength()
    {
        return text != null ? text.length() : 0;
    }
    
    public int getOffset()
    {
        return offset;
    }
    
    public int hashCode()
    {
        return (offset * 31 + getLength()) * 31 + getType();
    }
    
    public boolean includes(int offset)
    {
        return (this.offset <= offset) && (offset < this.offset + getLength());
    }
    
    public void setOffset(int offset)
    {
        this.offset = offset;
    }
    
    public void shift(int offsetDelta, int lineDelta)
    {
        offset += offsetDelta;
        setLine(getLine() + lineDelta);
    }
    
    public String toString()
    {
        return "[\"" + getText() + "\",<" + type + ">,line=" + line + ",col=" + col + ",offset=" + offset + ",length=" + getLength() + "]";
    }
}
