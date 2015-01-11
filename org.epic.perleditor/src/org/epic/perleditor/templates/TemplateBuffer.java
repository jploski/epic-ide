/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates;

import org.eclipse.core.runtime.CoreException;

//import org.eclipse.jdt.internal.corext.Assert;

/**
 * A template buffer is a container for a string and variables.
 */
public final class TemplateBuffer {
	
	/** The string of the template buffer */ 
	private String fString;
	/** The variable positions of the template buffer */
	private TemplatePosition[] fVariables;
	
	/**
	 * Creates a template buffer.
	 * 
	 * @param string the string
	 * @param variables the variable positions
	 * @throws CoreException for illegal variable positions
	 */
    public TemplateBuffer(String string, TemplatePosition[] variables) throws CoreException {
		setContent(string, variables);
    }

	/**
	 * Sets the content of the template buffer.
	 * 
	 * @param string the string
	 * @param variables the variable positions
	 * @throws CoreException for illegal variable positions
	 */
	public final void setContent(String string, TemplatePosition[] variables) throws CoreException {
	//	Assert.isNotNull(string);
	//	Assert.isNotNull(variables);

		// XXX assert non-overlapping variable properties

		fString= string;
		fVariables= variables;
	}

	/**
	 * Returns the string of the template buffer.
	 */
	public final String getString() {
		return fString;
	}
	
	/**
	 * Returns the variable positions of the template buffer.
	 */
	public final TemplatePosition[] getVariables() {
		return fVariables;
	}
    
    /**
     * Indents all lines after the first line in this TemplateBuffer
     * by inserting <code>indentChars</code> at their beginning. This
     * is to preserve the current block's indentation level when
     * inserting a multiline template.
     */
    public void indent(String indentChars)
    {        
        int j = 0;
        int indentLen = indentChars.length();        
        if (indentLen == 0) return; // nothing to do
        
        StringBuffer buf = new StringBuffer();
        
        for (int k = 0;; k++)
        {
            int i = skipToNextLine(j);
            if (i == -1)
            {
                buf.append(fString.substring(j));
                fString = buf.toString();
                return;
            }            
            buf.append(fString.substring(j, i));
            buf.append(indentChars);
            indentVars(i+indentLen*k, indentLen);
            j = i;
        }
    }
    
    private void indentVars(int fromOffset, int delta)
    {
        for (int i = 0; i < fVariables.length; i++)
        {
            int[] offsets = fVariables[i].getOffsets();
            for (int j = 0; j < offsets.length; j++)
            {
                if (offsets[j] >= fromOffset) offsets[j] += delta;
            }
            fVariables[i].setOffsets(offsets);
        }
    }
    
    private int skipToNextLine(int fromOffset)
    {
        for (int i = fromOffset; i < fString.length(); i++)
        {
            char c = fString.charAt(i);
            if (c == '\r')
            {
                if (i < fString.length()-1 && fString.charAt(i+1) == '\n')
                    return i+2;
                else
                    return i+1;
            }
            else if (c == '\n')
            {
                return i+1;
            }
        }
        return -1;
    }
}
