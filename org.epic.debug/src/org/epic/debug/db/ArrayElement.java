package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;

/**
 * A PerlVariable representing an element of an array (list element).
 * 
 * @author jploski
 */
class ArrayElement extends PerlVariable
{
    private final PerlVariable array;
    
    protected ArrayElement(
        DebuggerInterface db,
        PerlVariable array,
        DumpedEntity entity) throws DebugException 
    {
        super(db, array.getStackFrame(), entity);
        
        this.array = array;
    }
    
    /**
     * @return the element's index enclosed in square brackets
     */
    public String getName() throws DebugException
    {
        return "[" + super.getName() + "]";
    }

    public String getExpression() throws DebugException
    {
        StringBuffer buf = new StringBuffer();
        int refCount = getDumpedEntity().getReferenceCount();
        
        for (int i = 0; i < refCount; i++) buf.append("${");
        buf.append(array.getExpression());        
        for (int i = 0; i < refCount; i++) buf.append("}");
        buf.append(getName());
        
        return buf.toString();
    }
}
