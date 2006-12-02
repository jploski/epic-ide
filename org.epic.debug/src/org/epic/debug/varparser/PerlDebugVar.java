package org.epic.debug.varparser;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;

public class PerlDebugVar extends DebugElement implements IVariable
{
    public static final int LOCAL_SCOPE = 1;
    public static final int GLOBAL_SCOPE = 2;
    
    private final String name;
    private final int scope;

    private PerlDebugValue value;
    private int hasChanged;
    private boolean special;
    private boolean visited;
    private boolean isSet;    

    public PerlDebugVar(
        IDebugTarget parent,
        int scope,
        String name,
        PerlDebugValue value)
    {
        super(parent);

        this.scope = scope;
        this.name = name;
        this.value = value;
        this.hasChanged = PerlDebugValue.VALUE_UNCHANGED;
    }

    public boolean calculateChangeFlags(PerlDebugVar oldVar)
    {
        if (oldVar.visited) return false;
        oldVar.visited = true;

        try
        {
            if (!getName().equals(oldVar.getName())) return false;
    
            int ret = value.calculateChangeFlags(oldVar.getPdValue());

            setChangeFlags(ret, false);
    
            return ret != PerlDebugValue.VALUE_UNCHANGED;    
        }
        catch (DebugException e)
        {
            PerlDebugPlugin.log(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "PerlDebugVar.calculateChangeFlags failed",
                e));
            return false;
        }
    }

    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }

    public String getName() throws DebugException
    {
        return name;
    }

    public PerlDebugValue getPdValue()
    {
        return value;
    }

    public String getReferenceTypeName() throws DebugException
    {
        if (value != null) return value.getReferenceTypeName();
        else return null;
    }

    public IValue getValue() throws DebugException
    {
        return value;
    }

    public boolean hasContentChanged() throws DebugException
    {
        return (hasChanged & PerlDebugValue.CONTENT_HAS_CHANGED) > 0;
    }

    public boolean hasValueChanged() throws DebugException
    {
        return (hasChanged & PerlDebugValue.VALUE_HAS_CHANGED) > 0;
    }

    public boolean isGlobalScope()
    {
        return scope == GLOBAL_SCOPE;
    }

    public boolean isLocalScope()
    {
        return scope == LOCAL_SCOPE;
    }

    public boolean isSpecial()
    {
        return special;
    }

    public boolean matches(PerlDebugVar var)
    {
        if (!var.name.equals(name)) return false;
        if (var.scope != scope) return false;

        return true;
    }

    public void setChangeFlags(int flags, boolean recurse)
    {
        if (isSet == true) return;
        else isSet = true;

        hasChanged = flags;

        if (recurse) value.setChangeFlags(flags, recurse);
    }

    public void setSpecial()
    {
        special = true;
    }

    public void setValue(String expression) throws DebugException
    {
    }

    public void setValue(IValue value) throws DebugException
    {
        this.value = (PerlDebugValue) value;
    }

    public boolean supportsValueModification()
    {
        return false;
    }

    public String toString()
    {
        return name + " = " + value;
    }

    public boolean verifyValue(String expression) throws DebugException
    {
        return false;
    }

    public boolean verifyValue(PerlDebugValue value) throws DebugException
    {
        return false;
    }

    public boolean verifyValue(IValue fVal)
    {
        return false;
    }
}
