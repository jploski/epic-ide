package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;

/**
 * A PerlVariable representing a Perl variable from the symbol table of package 'main'.
 * 
 * @author jploski
 */
class PackageVariable extends PerlVariable
{
    protected PackageVariable(
        DebuggerInterface db,
        StackFrame frame,
        DumpedEntity entity) throws DebugException 
    {
        super(db, frame, entity);
    }
    
    public String getExpression() throws DebugException
    {
        String name = getName();
        StringBuffer buf = new StringBuffer();
        
        int refCount = getDumpedEntity().getReferenceCount();
        
        char deref; // the outermost dereference operator
        if (isArray()) deref = '@';
        else if (isHash()) deref = '%';
        else deref = '$';

        buf.append('\\'); // we want the whole expr to be a reference
        for (int i = 0; i < refCount; i++)
        {
            buf.append(deref);
            buf.append("{");
            deref = '$'; // inner dereferences are always ${..}
        }
        buf.append(deref);
        buf.append("main::");
        buf.append(name.substring(1));
        for (int i = 0; i < refCount; i++) buf.append("}");
        
        return buf.toString();
    }
    
    public boolean isPackageScope()
    {
        return true;
    }
}
