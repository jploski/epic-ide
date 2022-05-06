package org.epic.debug.db;

import java.io.IOException;
import java.util.*;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.ui.action.HighlightVarUpdatesActionDelegate;

public abstract class PerlXVar extends DebugElement implements IVariable
{
    private final StackFrame frame;
    protected PerlXValue value;
    protected String name;
    private String quotedName;
    private String expression;
    
    /**
     * @param frame     stack frame which contains this variable
     * @param entity    variable's contents received from the debugger
     */
    protected PerlXVar(
    		PerlXValue value) throws DebugException
    {
        super(value.stackFrame.getDebugTarget());
        
        this.frame = value.stackFrame;
        this.name = value.name;
        this.expression = expression;
        this.value = value;
    }
    
    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }

    /**
     * @return name for this expression in an expression of variable view
     */
    public String getName() throws DebugException
    {
        return name;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
    	return value.getReferenceTypeName();
    }
    
    /**
     * @return stack frame which contains this variable
     */
    public StackFrame getStackFrame()
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
        return value;
    }
    
    public String getExpression(){
    	return this.expression;
    }
    public void setValue(PerlXValue value){
    	this.value=value;
    }
    
    public boolean hasContentChanged() throws DebugException
    {
    	return false;
    }

    public boolean hasValueChanged() throws DebugException
    {
    	return false;
    }

    /**
     * This operation is not supported.
     */
    public void setValue(String expression) throws DebugException
    {
    	String e2=expression;
    	if(e2.contains("\n")){
    		e2="\""+expression.replaceAll("\r?\n", "\\\\n")+"\"";
    	}
    	String setString=this.value.expression + "=" + e2;
        try {
			this.frame.getPerlThread().getDB().eval(setString);
			this.frame.getPerlThread().suspended(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * This operation is not supported.
     */
    public void setValue(IValue value) throws DebugException
    {
    	throwNotSupported();
    }

    public boolean supportsValueModification()
    {
        return true;
    }

    /**
     * This operation is not supported.
     */
    public boolean verifyValue(String expression) throws DebugException
    {
        return true;
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
