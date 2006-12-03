package org.epic.debug.varparser;

import java.util.*;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;
import org.epic.debug.PerlDebugPlugin;

public class PerlDebugValue extends DebugElement implements IValue
{
    private static final IVariable[] NO_VARS = new IVariable[0];
    
    public static final int CONTENT_HAS_CHANGED = 1;
    public static final int VALUE_HAS_CHANGED = 2;
    public static final int VALUE_UNCHANGED = 0;

    private final List vars;
    private final String value;
    private final String type;

    public PerlDebugValue(IDebugTarget parent, String type, String value)
    {
        this(parent, type, value, NO_VARS);
    }
    
    public PerlDebugValue(
        IDebugTarget parent,
        String type,
        String value,
        IVariable[] vars)
    {
        super(parent);

        this.type = type;
        this.value = value;
        this.vars = new ArrayList(Arrays.asList(vars));
    }
    
    public void addVar(PerlDebugVar var)
    {
        vars.add(var);
    }

    public int calculateChangeFlags(PerlDebugValue previous)
    {
        int ret = 0;
        try
        {
            if ((value == null && previous.getValueString() != null)
                || !value.equals(previous.getValueString()))
            {
                ret = VALUE_HAS_CHANGED;
            }

            if ((type == null && previous.type != null)
                || (type != null && !type.equals(previous.type)))
            {
                ret = VALUE_HAS_CHANGED;
            }

            int countOld = previous.vars.size();
            int countNew = vars.size();           

            if (countOld != countNew) ret |= CONTENT_HAS_CHANGED;

            for (int newPos = 0; newPos < countNew; ++newPos)
            {
                boolean found = false;
                PerlDebugVar varNew = (PerlDebugVar) vars.get(newPos);

                for (int oldPos = 0; oldPos < countOld && !found; ++oldPos)
                {
                    PerlDebugVar varOld = (PerlDebugVar) previous.vars.get(oldPos);
                    if (varOld.getName().equals(varNew.getName()))
                    {
                        found = true;
                        if (varNew.calculateChangeFlags(varOld))
                            ret |= CONTENT_HAS_CHANGED;
                    }
                }

                if (!found)
                {
                    varNew.setChangeFlags(VALUE_HAS_CHANGED, true);
                    ret |= CONTENT_HAS_CHANGED;
                }
            }
            return ret;
        }
        catch (DebugException e)
        {
            PerlDebugPlugin.log(e);
            return VALUE_UNCHANGED;
        }        
    }

    public String getModelIdentifier()
    {
        return getDebugTarget().getModelIdentifier();
    }

    public String getReferenceTypeName() throws DebugException
    {
        return type;
    }

    public String getValueString() throws DebugException
    {
        return value;
    }

    public IVariable[] getVariables() throws DebugException
    {
        return (IVariable[]) vars.toArray(new IVariable[vars.size()]);
    }

    public boolean hasVariables() throws DebugException
    {
        return !vars.isEmpty();
    }

    public boolean isAllocated() throws DebugException
    {
        return true;
    }
    
    public String toString()
    {
        return
            "T" + (type != null ? type : "<null>") + "{" + value +
            "} with " + vars.size() + " vars";
    }
    
    void setChangeFlags(int value, boolean recurse)
    {
        Iterator i = vars.iterator();

        while (i.hasNext())
        {
            ((PerlDebugVar) i.next()).setChangeFlags(value, recurse);
        }
    }
}
