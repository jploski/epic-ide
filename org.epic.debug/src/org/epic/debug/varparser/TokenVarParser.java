package org.epic.debug.varparser;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.epic.debug.PerlDebugPlugin;

public class TokenVarParser
{
    private final IDebugTarget target;
    private final Stack varStack;
    private final HashMap varMap;
    private final ILog log;

    private int scope;
    private int pos;
    private List vars;
    private char[] chars;

    public TokenVarParser(IDebugTarget target, ILog log)
    {
        this.target = target;
        this.log = log;
        this.varStack = new Stack();
        this.varMap = new HashMap();
    }

    public List parseVars(String text, int scope)
    {
        return parseVars(text, scope, new ArrayList());
    }

    public List parseVars(String text, int scope, List vars)
    {
        reset(text, scope, vars);
        readVars();

        return vars;
    }

    private void addVar(String name, String value)
    {
        PerlDebugVar var = new PerlDebugVar(
            target, scope, name, new PerlDebugValue(target, " ", value));

        if (!varStack.empty())
        {
            ((PerlDebugVar) varStack.peek()).getPdValue().addVar(var);
        }
        varStack.push(var);

        int pos = value.indexOf(')');
        if (pos > 0)
        {
            varMap.put(value.substring(0, pos + 1), var);
        }
    }

    private void linkVar(String name, String value) throws ParseException
    {
        try
        {
            PerlDebugVar referencedVar = (PerlDebugVar) this.varMap.get(value);
            PerlDebugValue referencedVal = referencedVar.getPdValue();
            PerlDebugValue val = new PerlDebugValue(
                target, " ", referencedVal.getValueString());

            PerlDebugVar var = new PerlDebugVar(target, scope, name, val);

            if (!varStack.empty())
            {
                ((PerlDebugVar) varStack.peek()).getPdValue().addVar(var);
            }
            varStack.push(var);
        }
        catch (DebugException e)
        {
            throw new ParseException(
                "linkVar failed for name={" + name + "}, value={" + value + "}",
                true, e);
        }
    }

    private String readString() throws ParseException
    {
        if (chars[pos] != 'S')
            throw new ParseException("expected token S missing @" + pos, true);

        try
        {
            pos++;
            String temp = new String(chars, pos, 8);
            int length = Integer.parseInt(temp, 16);
            pos += 8;
            temp = new String(chars, pos, length);
            pos += length;
            return temp;
        }
        catch (Exception e) // NumberFormatException, IndexOutOfBoundsException
        {
            throw new ParseException("could not parse string @" + pos, true, e);
        }
    }

    private String readStrings() throws ParseException
    {
        StringBuffer buf = new StringBuffer(readString());
        
        while (pos < chars.length && chars[pos] == 'S')
            buf.append(readString());

        return buf.toString();
    }

    private boolean readVar() throws ParseException
    {
        if (pos >= chars.length) throw new ParseException(
            "unexpected end of stream", false);
        
        if (chars[pos] == 'E') return false;
        if (chars[pos] != 'N')
        {
            throw new ParseException(
                "expected token N missing @" + pos, true);                
        }

        pos++;
        String name = readString();
        String value = readStrings();

        if (chars[pos] == 'R')
        {
            pos++;
            linkVar(name, value);
        }
        else
        {
            addVar(name, value);
        }

        if (chars[pos] == 'I')
        {
            pos++;
            readVars();            
        }
        PerlDebugVar var = (PerlDebugVar) varStack.pop();
        if (varStack.empty()) vars.add(var);
        if (chars[pos] == 'O')
        {
            pos++;
            return false;
        }
        return true;
    }
    
    private void readVars()
    {
        for (;;)
        {
            boolean hasErrors = false;
            
            try { if (!readVar()) break; }
            catch (ParseException e)
            {
                if (!hasErrors)
                {
                    hasErrors = true;
                    log.log(new Status(
                        IStatus.ERROR,
                        PerlDebugPlugin.getUniqueIdentifier(),
                        IStatus.OK,
                        "An error occurred while parsing debugger variables; " +
                        "contents of the Variables view may be inaccurate. " +
                        "Failure caused by string: {" + String.valueOf(chars) + "}",
                        e));
                }
                if (e.canRecover()) recover();
                else break;
            }
        }
    }
    
    private void recover()
    {
        // this recovery algorithm is simplistic and will not recover
        // well from parsing errors encountered in nested variables
        while (
            pos < chars.length &&
            chars[pos] != 'N' &&
            chars[pos] != 'E') pos++;
    }
    
    private void reset(String text, int scope, List vars)
    {
        this.varMap.clear();
        this.varStack.clear();
        this.chars = text.toCharArray();
        this.scope = scope;
        this.vars = vars;
        this.pos = 0;
    }
    
    private static class ParseException extends Exception
    {
        private final boolean canRecover;
        
        public ParseException(String msg, boolean canRecover)
        {
            this(msg, canRecover, null);
        }
        
        public ParseException(String msg, boolean canRecover, Throwable cause)
        {
            super(msg, cause);
            this.canRecover = canRecover;
        }
        
        public boolean canRecover()
        {
            return canRecover;
        }
    }
}