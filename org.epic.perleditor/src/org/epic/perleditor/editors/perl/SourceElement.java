package org.epic.perleditor.editors.perl;

import org.epic.core.model.ISourceElement;

public class SourceElement implements ISourceElement
{
    private final String name;
    private final int offset;
    private final int length;
    
    public SourceElement(String name, int offset, int length)
    {
        this.name = name;
        this.offset = offset;
        this.length = length;
    }

    public int getLength()
    {
        return length;
    }

    public String getName()
    {
        return name;
    }

    public int getOffset()
    {
        return offset;
    }
}