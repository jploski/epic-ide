package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;

/**
 * A PerlVariable representing a Perl lexical variable.
 * 
 * @author jploski
 */
class LexicalVariable extends PerlVariable
{
    protected LexicalVariable(
        DebuggerInterface db,
        StackFrame frame,
        DumpedEntity entity) throws DebugException 
    {
        super(db, frame, entity);
    }
    
    public String getExpression() throws DebugException
    {
        StringBuffer buf = new StringBuffer();
        int refCount = getDumpedEntity().getReferenceCount();
        
        for (int i = 0; i < refCount; i++) buf.append("${");
        buf.append("$h->{");
        buf.append(getQuotedName());
        buf.append("}");
        for (int i = 0; i < refCount; i++) buf.append("}");
        
        return buf.toString();
    }
}
