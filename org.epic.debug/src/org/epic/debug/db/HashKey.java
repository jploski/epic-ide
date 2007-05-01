package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;

/**
 * A PerlVariable representing a key from a hash.
 * 
 * @author jploski
 */
class HashKey extends PerlVariable
{
    private final PerlVariable hash;
    
    protected HashKey(
        DebuggerInterface db,
        PerlVariable hash,
        DumpedEntity entity) throws DebugException 
    {
        super(db, hash.getStackFrame(), entity);
        
        this.hash = hash;
    }
    
    public String getExpression() throws DebugException
    {
        StringBuffer buf = new StringBuffer();
        int refCount = getDumpedEntity().getReferenceCount() - 1;
        
        for (int i = 0; i < refCount; i++) buf.append("${");
        buf.append(hash.getExpression());
        buf.append("->{");
        buf.append(getQuotedName());
        buf.append("}");
        for (int i = 0; i < refCount; i++) buf.append("}");
        
        return buf.toString();
    }
    
    public boolean hasValueChanged() throws DebugException
    {
        // see remarks in PerlVariable.hasContentChanged on
        // why keys of %! require special treatment
        return hash.getName().equals("%!") ? false : super.hasValueChanged();
    }
}
