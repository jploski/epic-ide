package org.epic.core.model;

import org.epic.core.parser.PerlToken;

/**
 * An ISourceElement representing a POD comment.
 * 
 * @author jploski
 */
public class PODComment implements IMultilineElement
{
    private final PerlToken podStart;
    private final PerlToken podEnd;

    public PODComment(PerlToken podStart, PerlToken podEnd)
    {
        this.podStart = podStart;
        this.podEnd = podEnd;
    }
    
    public int getLength()
    {
        return podStart.getLength();
    }

    public String getName()
    {
        return podStart.getText();
    }

    public int getOffset()
    {
        return podStart.getOffset();
    }

    public int getEndLine()
    {
        return podEnd.getLine()-1;
    }

    public int getStartLine()
    {
        return podStart.getLine()-1;
    }
}
