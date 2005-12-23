package org.epic.core.model;

/**
 * Interface implemented by recognised elements of source code,
 * used to select these elements in text and present them in views.
 * 
 * @author jploski
 */
public interface ISourceElement
{
    /**
     * @return length of the text representing the element in a source document
     *         (for selection)
     */
    public int getLength();

    /**
     * @return a human-readable name of the source element intended for
     *         distinguishing it from other source elements in GUI presentations;
     *         for this reason it is not required, yet encouraged to be unique
     */
    public String getName();
    
    /**
     * @return start offset of the text representing the element in
     *         a source document (for selection)
     */
    public int getOffset();
}