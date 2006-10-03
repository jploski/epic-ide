package org.epic.core.model;

import org.epic.core.parser.PerlToken;

/**
 * An ISourceElement representing a "use Module" statement.
 * 
 * @author jploski
 */
public class ModuleUse implements IPackageElement
{
    private final Package parent;
    private final int index;
    private final PerlToken useKeyword;
    private final PerlToken name;
    
    public ModuleUse(Package parent, int index, PerlToken useKeyword, PerlToken name)
    {
        this.parent = parent;
        this.index = index;
        this.useKeyword = useKeyword;
        this.name = name;
    }
    
    public int getIndex()
    {
        return index;
    }

    public int getLength()
    {
        return name.getLength();
    }

    public String getName()
    {
        return name.getText();
    }
    
    public PerlToken getNameToken()
    {
        return name;
    }

    public int getOffset()
    {
        return name.getOffset();
    }
    
    public Package getParent()
    {
        return parent;
    }
    
    public PerlToken getUseKeyword()
    {
        return useKeyword;
    }
    
    public String toString()
    {
        return "use #" + index + " " + getName() + " @" + getOffset(); 
    }
}
