package org.epic.core.parser;

import antlr.*;

/**
 * Base class for component lexers of the PerlMultiLexer.
 * 
 * @author jploski
 */
public abstract class LexerBase extends CharScanner
{
    private static final int RUNAWAY_STRING_LINES_COUNT = 500;
    
    private int endLine;
    private PerlMultiLexer parent;
    
    protected LexerBase()
    {
        _init();
    }

    protected LexerBase(InputBuffer cb)
    {
        super(cb);
        _init();
    }

    protected LexerBase(LexerSharedInputState sharedState)
    {
        super(sharedState);
        _init();
    }
    
    /**
     * @return the parent PerlMultiLexer
     */
    PerlMultiLexer getParent()
    {
        return parent;
    }
    
    public void setInputState(LexerSharedInputState state)
    {
        super.setInputState(state);
        endLine = 0;
    }
    
    public void setTokenObjectClass(String className)
    {
        try
        {
            tokenObjectClass = Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e); // can't do anything
        }
    }
    
    /**
     * Associates this lexer with its parent PerlMultiLexer.
     * This method is intended to be used in PerlMultiLexer's initialisation only.
     */
    void setParent(PerlMultiLexer parent)
    {
        this.parent = parent;
    }
    
    /**
     * Sets the line on which the current lexical state was entered.
     * This can be used to forcibly terminate runaway string tokens.
     */
    void setStartLine(int line)
    {
        this.endLine = line + RUNAWAY_STRING_LINES_COUNT;
    }
    
    public void uponEOF()
        throws TokenStreamException, CharStreamException
    {
        getParent().uponEOF();
    }
    
    protected boolean maxLinesExceeded()
    {
        return getLine() > endLine;
    }
    
    protected Token makeToken(int t)
    {
        PerlToken pt = (PerlToken) super.makeToken(t);
        pt.setOffset(getParent().computeTokenOffset(pt));
        return pt;
    }
    
    private void _init()
    {
        setTokenObjectClass(PerlToken.class.getName());
    }
}
