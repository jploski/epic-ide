package org.epic.core.model;

/**
 * An ISourceElement which spans multiple lines.
 * 
 * @author jploski
 */
public interface IMultilineElement extends ISourceElement
{
    /**
     * @return last line of the element (&lt;= startLine)
     */
    public int getEndLine();

    /**
     * @return first line of the element (first line of the document = 0)
     */
    public int getStartLine();
}