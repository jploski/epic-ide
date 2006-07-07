package org.epic.core.model;

/**
 * Interface implemented by source elements which belong to a Package. 
 */
public interface IPackageElement extends ISourceElement
{
    /**
     * @return the parent package
     */
    public Package getParent(); 
}