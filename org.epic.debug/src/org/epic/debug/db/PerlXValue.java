package org.epic.debug.db;

import java.io.IOException;
import java.util.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.ui.action.ShowVarAddressActionDelegate;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * This is similar to PerlValue but is populated by a call to 'x' in the dubugger and cannot have any unresolved subVariables
 * This allows it to store results of function calls etc which can't be reliably requeried for details
 * 
 * @author jrichard
 */
public abstract class PerlXValue extends DebugElement implements IValue
{
	List<PerlXVar> vars=new ArrayList<PerlXVar>();
	public StackFrame stackFrame;
	public String expression;
	public String name;
	protected PerlXValue(String name, StackFrame f)
		throws DebugException
	{
		super(f.getDebugTarget());
		this.stackFrame = f;
		this.name = name;
	}

	@Override
	public String getValueString() throws DebugException {
		return getDetailValueList(getBraces());
	}
	
	/**
	 * @return the value to be displayed in the detail section
	 *         of the Variables view
	 */
	public String getDetailValue() throws DebugException
	{
		return getDetailValueList(getBraces());
	}
	
    public IVariable[] getVariables() throws DebugException
    {
        return vars.toArray(new IVariable[]{});
    }

    public boolean hasVariables() throws DebugException
    {
        return vars.size() > 0;
    }
    
	public boolean isAllocated() throws DebugException
	{
		return true;
	}

	public String getModelIdentifier()
	{
		return getDebugTarget().getModelIdentifier();
	}
	
	public abstract boolean add(
			PerlXValue e) throws DebugException;
	public abstract String getPrefix();
	public String getBraces(){
		return "  ";
	};
	
	private String getDetailValueList(String braces) throws DebugException
	{
        IVariable[] ivarArr = getVariables();
        if (ivarArr.length == 0) return braces;
        if (ivarArr[0] instanceof ArraySlice) return "...";
        
        boolean isHash = this instanceof HashXValue;
        StringBuilder result = new StringBuilder();
        result.append(braces.charAt(0));
        result.append(' ');
        
        if (isHash) HashKeySorter.sort(ivarArr);
        
        int i;
        int maxI = Math.min(ivarArr.length, 10); // max 10 elements
        
        for (i = 0; i < maxI; i++)
        {
            IVariable iv = ivarArr[i];
            IValue value = iv.getValue();
            
            if (isHash) // also display hash keys
            {
                result.append(iv.getName());
                result.append(" => ");
            }
            
            if ("SCALAR".equals(iv.getReferenceTypeName())) result.append(value.getValueString());
            else if (value instanceof PerlXValue) result.append(((PerlXValue) value).getDetailValueList(((PerlXValue) value).getBraces()));
            
            if (i < maxI-1) result.append(',');
            result.append(' ');
        }
        
        if (i < ivarArr.length) result.append("...");
        result.append(braces.charAt(1));
        return result.toString();
	}
}
