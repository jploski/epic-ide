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
        
        if (refCount == 0)
        {
            if (isHash())
            {
                // we want the whole expr to be a reference,
                // but beware that the \%main::hash->{key} does not work
                // +{%main::hash}->{key} does the trick nicely
                buf.append("+{"); 
                buf.append(name.substring(0, 1));
                buf.append("main::");
                buf.append(name.substring(1));
                buf.append('}');
            }
            else
            {
                if ("@_".equals(name))
                {
                    buf.append("\\@_");
                }
                else
                {
                    buf.append('\\'); // we want the whole expr to be a reference
                    buf.append(name.substring(0, 1));
                    buf.append("main::");
                    buf.append(name.substring(1));
                }
            }
        }
        else
        {
            for (int i = 0; i < refCount-1; i++)
            {
                buf.append("${");
            }
            buf.append("$main::");
            buf.append(name.substring(1));
            for (int i = 0; i < refCount-1; i++) buf.append("}");
        }
        return buf.toString();
    }
    
    public boolean isPackageScope()
    {
        return true;
    }
}
