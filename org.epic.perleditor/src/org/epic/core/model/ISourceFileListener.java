package org.epic.core.model;

/**
 * Implemented by classes interested in changes of a SourceFile.
 */
public interface ISourceFileListener
{
    /**
     * Invoked by a SourceFile to notify that its contents have
     * (potentially) changed.
     * 
     * @param source the reporting source file
     */
    public void sourceFileChanged(SourceFile source);
}
