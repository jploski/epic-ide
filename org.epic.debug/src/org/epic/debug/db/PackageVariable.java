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
        buf.append('\\');
        buf.append(name.charAt(0));
        buf.append("main::");
        buf.append(name.substring(1));
        return buf.toString();
    }
    
    public boolean isPackageScope()
    {
        return true;
    }
}
