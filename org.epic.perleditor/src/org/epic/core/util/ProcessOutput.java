package org.epic.core.util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The collected output of an executed process.
 * 
 * @author jploski
 */
public class ProcessOutput
{
    /**
     * Output provided through stdout.
     */
    public final String stdout;
    
    /**
     * Output provided through stderr.
     */
    public final String stderr;
    
    public ProcessOutput(String stdout, String stderr)
    {
        this.stdout = stdout;
        this.stderr = stderr;
    }
 
    public List<String> getStderrLines()
    {
        return getLines(stderr);
    }
    
    public List<String> getStdoutLines()
    {
        return getLines(stdout);
    }
    
    private List<String> getLines(String str)
    {
        BufferedReader r = new BufferedReader(new StringReader(str));
        List<String> lines = new ArrayList<String>();
        String l;
        
        try { while ((l = r.readLine()) != null) lines.add(l); }
        catch (java.io.IOException e) { /* can't occur */ }
        return lines;
    }
}