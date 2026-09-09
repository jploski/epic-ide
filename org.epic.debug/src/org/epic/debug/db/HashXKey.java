package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;

class HashXKey extends PerlXVar
{
    protected HashXKey(PerlXValue value) throws DebugException 
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
		return "Key "+name+" - "+ value.toString();
	}
}
