package org.epic.core.parser;

import antlr.*;

/**
 * Base class for expectHereDocEnd lexer.
 * 
 * @author jploski
 */
public abstract class LexExpectHereDocEndBase extends LexerBase
{
    protected String terminator;
    
    protected LexExpectHereDocEndBase()
    {
    }

    protected LexExpectHereDocEndBase(InputBuffer cb)
    {
        super(cb);
    }

    protected LexExpectHereDocEndBase(LexerSharedInputState sharedState)
    {
        super(sharedState);
    }

    void setTerminator(String terminator)
    {
        this.terminator = terminator;
    }
}
