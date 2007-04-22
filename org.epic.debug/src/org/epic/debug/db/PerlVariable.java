package org.epic.debug.db;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;

/**
 * Abstract base class for objects that can appear in the Variables
 * view of the debugger. Note that this category includes array
 * elements and hash keys besides of real Perl variables.
 * 
 * @author jploski
 */
public abstract class PerlVariable extends DebugElement implements IVariable
{
    private final DebuggerInterface db;
    private final StackFrame2 frame;
    private final DumpedEntity entity;
    private PerlValue value;
    private String quotedName;
    
    /**
     * @param db        interface to the Perl debugger
     * @param frame     stack frame which contains this variable
     * @param entity    variable's contents received from the debugger
     */
    protected PerlVariable(
        DebuggerInterface db,
        StackFrame2 frame,
        DumpedEntity entity) throws DebugException
    {
        super(frame.getDebugTarget());
        
        this.db = db;
        this.frame = frame;
        this.entity = entity;
    }
    
    /**
     * @return interface to the Perl debugger which can be used
     *         to obtain further details about the variable (such
     *         as the contained subvariables)
     */
    public DebuggerInterface getDebuggerInterface()
    {
        return db;
    }
    
    /**
     * @return variable's contents received from the debugger
     */
    public DumpedEntity getDumpedEntity()
    {
        return entity;
    }
    
    /**
     * @return a Perl expression which resolves to a reference to
     *         this variable when evaluated in context containing
     *         the enclosing stack frame (as provided by PadWalker)
     *         in hash reference $h 
     */
    public abstract String getExpression() throws DebugException;
    
    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }

    /**
     * @return variable name; by default it follows the naming
     *         convention described in
     *         {@link org.epic.debug.db.DumpedEntity#getName()};
     *         however, subclasses may override this convention
     */
    public String getName() throws DebugException
    {
        return entity.getName();
    }

    /**
     * @see org.epic.debug.db.DumpedEntity#getReferenceTypeName
     */
    public String getReferenceTypeName() throws DebugException
    {
        return entity.getReferenceType();
    }
    
    /**
     * @return stack frame which contains this variable
     */
    public StackFrame2 getStackFrame()
    {
        return frame;
    }

    /**
     * @return the hash, array, scalar or other value referred to
     *         by this variable. Note that this reference may be
     *         indirect. That is, the real Perl variable might refer
     *         to another variable and so on, until a value is
     *         eventually reached or a circular reference is formed. 
     */
    public IValue getValue() throws DebugException
    {
        // Note: this method, in general, lazily retrieves
        // the variable's value by querying the Perl debugger
        
        if (value != null) return value;
        
        if (isHash())
        {
            value = new HashValue(getDebugTarget(), this);
        }
        else if (isArray())
        {
            value = new ArrayValue(getDebugTarget(), this);
        }
        else // if (isScalar()) and "unsupported" types such as CODE, GLOB, ...
        {
            value = new ScalarValue(getDebugTarget(), this);
        }
        return value;
    }

    public boolean hasValueChanged() throws DebugException
    {
        // TODO: reimplement change tracking and add a preference
        // to disable it for speed
        return false;
    }

    /**
     * @return true if this variable refers (possibly indirectly)
     *         to a list
     */
    public boolean isArray() throws DebugException
    {
        return "ARRAY".equals(getReferenceTypeName());
    }
    
    /**
     * @return true if this variable refers (possibly indirectly)
     *         to a hash
     */
    public boolean isHash() throws DebugException
    {
        return "HASH".equals(getReferenceTypeName());
    }

    /**
     * @return true if this variable refers (possibly indirectly)
     *         to a scalar
     */
    public boolean isScalar() throws DebugException
    {
        return "SCALAR".equals(getReferenceTypeName());
    }

    /**
     * This operation is not supported.
     */
    public void setValue(String expression) throws DebugException
    {
        throwNotSupported();
    }

    /**
     * This operation is not supported.
     */
    public void setValue(IValue value) throws DebugException
    {
        throw new DebugException(new Status(
            Status.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            DebugException.NOT_SUPPORTED,
            "Operation not supported",
            null));
    }

    /**
     * @return false, we do not support value modification through
     *         the usual interface; users may modify variables by
     *         evaluating expressions, but the effects of such
     *         modifications won't be noticed by the front-end
     *         until the next suspend event
     */
    public boolean supportsValueModification()
    {
        return false;
    }

    /**
     * This operation is not supported.
     */
    public boolean verifyValue(String expression) throws DebugException
    {
        throwNotSupported();
        return false;
    }

    /**
     * This operation is not supported.
     */
    public boolean verifyValue(IValue value) throws DebugException
    {
        throwNotSupported();
        return false;
    }
    
    /**
     * @return a Perl expression which yields the name of this variable
     */
    protected String getQuotedName() throws DebugException
    {
        if (quotedName == null) // lazy initialization
        {
            String name = getName();
            boolean clean = true;
            for (int i = 0; i < name.length(); i++)
            {
                char c = name.charAt(i);
                if (c >= 256 || c == '\'') { clean = false; break; }
            }
            
            if (clean) quotedName = "'" + name + "'";
            else // pack those pesky Unicode characters
            {
                StringBuffer buf = new StringBuffer("pack('");
                for (int i = 0; i < name.length(); i++) buf.append('U');
                buf.append("',");
                for (int i = 0; i < name.length(); i++)
                {
                    buf.append((int) name.charAt(i));
                    buf.append(',');
                }
                buf.append(')');
                quotedName = buf.toString();
            }
        }
        return quotedName;
    }
    
    private void throwNotSupported() throws DebugException
    {
        throw new DebugException(new Status(
            Status.ERROR,
            PerlDebugPlugin.getUniqueIdentifier(),
            DebugException.NOT_SUPPORTED,
            "Operation not supported",
            null));
    }
}
