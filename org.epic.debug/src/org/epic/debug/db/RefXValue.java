package org.epic.debug.db;

import java.util.*;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.PerlDebugPlugin;

/**
 * Represents contents of a Perl array (list).
 * 
 * @author jploski
 */
class RefXValue extends PerlXValue
{
	public PerlXValue ref;
    public RefXValue(String name, StackFrame f)
        throws DebugException
    {
        super(name, f);
    }

    public boolean add(PerlXValue e) throws DebugException{
    	if(e!=null){
    		ref=e;
    		e.expression=e.getPrefix() + "{"+this.expression+"}";
    		vars.clear();
    		vars.add(new RefXVar(e));
    		return true;
    	}
    	return false;
    }
    
    @Override
    public String getReferenceTypeName() throws DebugException {
    	return "REF";
    }
    
    class RefXVar extends PerlXVar
    {
    	public RefXVar(PerlXValue value) throws DebugException{
    		super(value);
    	}
    	
        /**
         * @return the element's index enclosed in square brackets
         */
        public String getName() throws DebugException
        {
            return "" + super.getName() + "";
        }
        
        public String toString(){
    		return "Reference "+name+" - "+ value.toString();
    	}
    }

	@Override
	public String getPrefix() {
		return "$";
	}
}
