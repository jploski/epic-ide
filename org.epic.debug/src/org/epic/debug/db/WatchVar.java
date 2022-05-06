package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;

class WatchVar extends PerlXVar
{
    protected WatchVar(PerlXValue value) throws DebugException 
    {
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
		return "watch "+name+" - "+ value.toString();
	}
}
