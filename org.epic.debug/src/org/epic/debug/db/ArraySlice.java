package org.epic.debug.db;

import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.PerlDebugPlugin;

/**
 * A variable representing a continuous range of elements in an array (or list).
 * This does not correspond to any real Perl variable and is meant only for
 * improving presentation and performance.
 * 
 * @author jploski
 */
class ArraySlice extends DebugElement implements IVariable
{
	private final ArraySliceValue value;

	protected ArraySlice(
			PerlVariable array,
			List<ArrayElement> elements,
			int startIndex) throws DebugException 
    {
		super(array.getDebugTarget());
		
		this.value = new ArraySliceValue(array, elements, startIndex);
    }

    public String getName() throws DebugException
    {
        return "[" + value.getStartIndex() + ".." + value.getEndIndex() + "]";
    }

    public String getReferenceTypeName() throws DebugException
    {
        return null;
    }

    public IValue getValue() throws DebugException
    {
        return value;
    }

    public boolean hasValueChanged() throws DebugException
    {
    	IVariable[] vars = value.getVariables();
    	
    	for (int i  = 0; i < vars.length; i++)
    		if (vars[i].hasValueChanged()) return true;
    	
        return false;
    }

    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }

    public void setValue(String expression) throws DebugException
    {
    	throwNotSupported();
    }

    public void setValue(IValue value) throws DebugException
    {
    	throwNotSupported();    
    }

    public boolean supportsValueModification()
    {
        return false;
    }

    public boolean verifyValue(String expression) throws DebugException
    {
    	throwNotSupported();
        return false;
    }

    public boolean verifyValue(IValue value) throws DebugException
    {
    	throwNotSupported();
        return false;
    }
    
    private void throwNotSupported() throws DebugException
    {
        throw new DebugException(new Status(
            Status.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            DebugException.NOT_SUPPORTED,
            "Operation not supported",
            null));
    }
}
