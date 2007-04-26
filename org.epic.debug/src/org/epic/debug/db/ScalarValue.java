package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;

/**
 * Represents contents of a Perl scalar or an unsupported type
 * of variable; anything which we can treat as a flat string on
 * the Java side.
 * 
 * @author jploski
 */
class ScalarValue extends PerlValue
{
    private static final IVariable[] NO_VARIABLES = new IVariable[0];
    
    public ScalarValue(IDebugTarget target, PerlVariable holder)
        throws DebugException
    {
        super(target, holder);
    }

    public IVariable[] getVariables() throws DebugException
    {
        return NO_VARIABLES;
    }

    public boolean hasVariables() throws DebugException
    {
        return false;
    }
}
