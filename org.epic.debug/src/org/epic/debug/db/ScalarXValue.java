package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;

class ScalarXValue extends PerlXValue
{
    private static final IVariable[] NO_VARIABLES = new IVariable[0];
    String val=null;
    public ScalarXValue(String name, StackFrame f, String value)
        throws DebugException
    {
        super(name, f);
        this.val=value;
    }

    public IVariable[] getVariables() throws DebugException
    {
        return NO_VARIABLES;
    }

    public boolean hasVariables() throws DebugException
    {
        return false;
    }
    
    @Override
    public String getReferenceTypeName() throws DebugException {
    	return "SCALAR";
    }
    
    @Override
    public String getValueString() throws DebugException {
    	return val;
    }

	@Override
	public boolean add(PerlXValue e) throws DebugException {
		return false;
	}

	@Override
	public String getPrefix() {
		return "$";
	}
}
