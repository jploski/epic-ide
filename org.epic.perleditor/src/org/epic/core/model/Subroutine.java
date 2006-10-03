package org.epic.core.model;

import org.epic.core.parser.CurlyToken;
import org.epic.core.parser.PerlToken;

/**
 * An ISourceElement representing a subroutine definition.
 * 
 * @author jploski
 */
public class Subroutine implements IMultilineElement, IPackageElement
{
    private final Package parent;
    private final int index;
    private final PerlToken subKeyword;
    private final PerlToken name;
    private final CurlyToken openCurly;
    private CurlyToken closeCurly;
    
    public Subroutine(
        Package parent,
        int index,
        PerlToken subKeyword,
        PerlToken name,
        CurlyToken openCurly)
    {
        assert parent != null;
        assert index >= 0;
        assert subKeyword != null;
        assert name != null;
        assert openCurly != null;

        this.parent = parent;
        this.index = index;
        this.subKeyword = subKeyword;
        this.name = name;
        this.openCurly = openCurly;
    }
    
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Subroutine)) return false;
        
        Subroutine sub = (Subroutine) obj;
        return index == sub.index && parent.equals(sub.parent);
    }
    
    public int getBlockLevel()
    {
        return openCurly.getLevel();
    }

    public CurlyToken getCloseCurly()
    {
        return closeCurly;
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

    public CurlyToken getOpenCurly()
    {
        return openCurly;
    }
    
    public Package getParent()
    {
        return parent;
    }

    public PerlToken getSubKeyword()
    {
        return subKeyword;
    }

    public int getEndLine()
    {
        return closeCurly != null ? closeCurly.getLine()-1 : getStartLine();
    }

    public int getStartLine()
    {
        return subKeyword.getLine()-1;
    }
    
    public int hashCode()
    {
        return parent.hashCode() * 37 + index;
    }
    
    public void setCloseCurly(CurlyToken curly)
    {
        assert closeCurly == null;
        assert curly.getLevel() == openCurly.getLevel();
        
        closeCurly = curly;
    }
    
    public String toString()
    {
        return "sub #" + index + " " + getName() + " @" + getOffset(); 
    }
}
