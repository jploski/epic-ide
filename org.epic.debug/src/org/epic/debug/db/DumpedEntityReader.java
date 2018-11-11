package org.epic.debug.db;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.eclipse.core.runtime.Assert;

/**
 * Used to read entities dumped by dumpvar_epic.pm.
 * 
 * @see POD section in dumpvar_epic.pm, which explains the dump format
 * @author jploski
 */
class DumpedEntityReader
{
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private final String input;
    private int i;
    
    /**
     * @param input     a series of entities dumped by dumpvar_epic
     */
    public DumpedEntityReader(String input)
    {
        Assert.isNotNull(input);
        this.input = input;
        this.i = 0;
    }   
    
    public boolean hasMoreEntities()
    {
        return i < input.length();
    }
    
    public DumpedEntity nextEntity()
    {
        String name = token();
        int refChainLength = Integer.parseInt(token());            
        String[] refChain = new String[refChainLength];
        for (int i = 0; i < refChainLength; i++) refChain[i] = token();
        String value = token();
        int valueLength = Integer.parseInt(token());
        
        return new DumpedEntity(name, refChain, value, valueLength);
    }
    
    private String token()
    {
        int j = i;
        while (input.charAt(i) != '|') i++;
        
        int tokenLength = Integer.parseInt(input.substring(j, i));
        j = i;
        i = j+tokenLength+1;
        if (i < input.length() &&
            (input.charAt(i) == '\n' || input.charAt(i) == '|')) i++;

        String token = input.substring(j+1, j+tokenLength+1);
        try
        {
            return UTF8_CHARSET.decode(ByteBuffer.wrap(token.getBytes("ISO-8859-1"))).toString();
        }
        catch (Exception e)
        {
            return token;
        }
    }
}
