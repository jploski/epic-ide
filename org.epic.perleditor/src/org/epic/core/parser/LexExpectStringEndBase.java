package org.epic.core.parser;

import antlr.*;

/**
 * Base class for StringLexer.
 * 
 * @author jploski
 */
public abstract class LexExpectStringEndBase extends LexerBase
{
    protected char quoteEndChar;
    
    protected LexExpectStringEndBase()
    {
    }

    protected LexExpectStringEndBase(InputBuffer cb)
    {
        super(cb);
    }

    protected LexExpectStringEndBase(LexerSharedInputState sharedState)
    {
        super(sharedState);
    }

    void setQuoteEndChar(char quoteEndChar)
    {
        this.quoteEndChar = quoteEndChar;
    }
}
