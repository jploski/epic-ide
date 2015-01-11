package org.epic.debug.db;

import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class ArraySliceValue extends DebugElement implements IValue
{
	private final IVariable[] elements;
	private final int startIndex;
	
	public ArraySliceValue(PerlVariable array, List<ArrayElement> elements, int startIndex)
	{
		super(array.getDebugTarget());
		
		this.elements = elements.toArray(new IVariable[elements.size()]);
		this.startIndex = startIndex;
	}
	
	public int getEndIndex()
	{
		return startIndex + elements.length - 1;
	}
	
	public int getStartIndex()
	{
		return startIndex;
	}
	
	public String getReferenceTypeName() throws DebugException
	{
        return null;
    }

    public String getValueString() throws DebugException
    {
        return "...";
    }

    public IVariable[] getVariables() throws DebugException
    {
        return elements;
    }

    public boolean hasVariables() throws DebugException
    {
        return true;
    }

    public boolean isAllocated() throws DebugException
    {
        return true;
    }

    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }
}
