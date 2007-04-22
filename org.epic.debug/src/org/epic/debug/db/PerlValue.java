package org.epic.debug.db;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;

/**
 * Abstract base class for objects representing values of PerlVariables.
 * 
 * @author jploski
 */
public abstract class PerlValue extends DebugElement implements IValue
{
    private static final String DB_DUMP_ENTITY; 
    
    private final PerlVariable holder;
    
    static
    {
        DB_DUMP_ENTITY = HelperScript.load("dump_entity.pl");
    }
    
    protected PerlValue(IDebugTarget target, PerlVariable holder)
        throws DebugException
    {
        super(target);
        
        this.holder = holder;
    }
    
    /**
     * @return the value to be displayed in the detail section
     *         of the Variables view
     */
    public String getDetailValue() throws DebugException
    {
        DumpedEntity entity = holder.getDumpedEntity();
        
        if (entity.isCyclicReference()) return "cyclic reference";
        else if (!entity.isDefined()) return "undef";
        else if (entity.getValue() != null) return "'" + entity.getValue() + "'";
        else if (hasVariables()) return "...";
        else if (holder.isArray()) return "[]";
        else if (holder.isHash()) return "{}";
        else return "";
    }
    
    /**
     * @return the variable which contains this value
     */
    public PerlVariable getHolder()
    {
        return holder;
    }

    /**
     * @see org.epic.debug.db.PerlVariable#getReferenceTypeName
     */
    public String getReferenceTypeName() throws DebugException
    {
        return holder.getReferenceTypeName();
    }

    /**
     * @return the string displayed for this value in the overview
     *         section of the Variables view
     */
    public String getValueString() throws DebugException
    {
        DumpedEntity entity = holder.getDumpedEntity();
        String[] refChain = entity.getReferenceChain();

        boolean suppressSelfAddress = true;
        int start = suppressSelfAddress ? 1 : 0;
        
        StringBuffer buf = new StringBuffer();
        for (int i = start; i < refChain.length; i++)
        {
            if (i > start) buf.append("->");
            buf.append(refChain[i]);
        }
        
        String detail = getDetailValue();
        if (detail.length() > 0)
        {
            if (buf.length() > 0) buf.append('=');
            if (detail.length() > 128)
                detail = detail.substring(0, 128) + "...";
            buf.append(detail);
        }
        return buf.toString();
    }

    public abstract IVariable[] getVariables() throws DebugException;

    public abstract boolean hasVariables() throws DebugException;

    public boolean isAllocated() throws DebugException
    {
        return true;
    }

    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }
    
    /**
     * Dumps subvariables of the variable which holds this value. 
     * 
     * @param subName   name of the dumpvar_epic2.pm subroutine which
     *                  should be invoked
     */
    protected String dumpEntity(String subName) throws DebugException
    {
        try
        {
            PerlVariable holder = getHolder();
            String code = HelperScript.replace(
                DB_DUMP_ENTITY,
                "#SET_OFFSET#",
                "my $offset = " + holder.getStackFrame().getFrameIndex() + ";");

            code = HelperScript.replace(
                code,
                "#SET_VAREXPR#",
                "my $varexpr = <<'EOT';\n" + holder.getExpression() + "\nEOT");

            code = HelperScript.replace(
                code,
                "#SET_SUBREF#",
                "my $subref = \\&dumpvar_epic2::" + subName + ";");
            
            return holder.getDebuggerInterface().eval(code);
        }
        catch (IOException e)
        {
            throw new DebugException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "An error occurred while retrieving variables from the debugger process",
                e));
        }
    }
}
