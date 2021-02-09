package org.epic.debug.db;

import java.util.*;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.PerlDebugPlugin;

class ArrayXValue extends PerlXValue
{
    public ArrayXValue(String name, StackFrame f)
        throws DebugException
    {
        super(name, f);
    }

    public boolean add(PerlXValue e) throws DebugException{
    	if(e!=null){
    		e.expression=this.expression+"->["+e.name+"]";
    		vars.add(new ArrayXElement(e));
    		return true;
    	}
    	return false;
    }
    
    @Override
    public String getReferenceTypeName() throws DebugException {
    	return "ARRAY";
    }

	@Override
	public String getPrefix() {
		return "@";
	}
	
	@Override
	public String getBraces() {
		return "()";
	}
}
