package org.epic.debug.util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.*;
import org.epic.core.util.ProcessExecutor;
import org.epic.core.util.ProcessOutput;
import org.epic.debug.PerlDebugPlugin;

/**
 * An implementation of IPathMapper which translates Cygwin paths into
 * Windows paths and vice versa.
 */
public class CygwinPathMapper extends AbstractPathMapper
{
    public CygwinPathMapper() throws CoreException
    {
        try
        {        
            ProcessExecutor executor = new ProcessExecutor();
            ProcessOutput output = 
                executor.execute(new String[] { "mount" }, "", new File("."), null);
            
            initMappings(output.stdout.replaceAll("\n", "\r\n"));
        }
        catch (InterruptedException e) { /* can't occur */ }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not execute 'mount' to find out path mappings.\n" +
                "Add Cygwin's 'bin' directory (which contains mount.exe) to your PATH.",
                e));
        }            
    }
    
    private void initMappings(String mountOutput) throws CoreException
    {
        // e.g. "f:\cygwin\bin on /usr/bin type system (binmode)"
        Pattern p = Pattern.compile("^(.+)\\s+on\\s+(/.*) type", Pattern.MULTILINE);
        Matcher m = p.matcher(mountOutput);
        
        while (m.find())
        {
            String epicPath = m.group(1);
            String dbPath = m.group(2);

            // c: -> C:/ to match absolute paths as they are stored in PerlDB.activeBreakpoints
            if (epicPath.length() == 2 && epicPath.charAt(1) == ':')
                epicPath = epicPath.toUpperCase() + "/";
            
            addMapping(new Path(epicPath), new Path(dbPath));
        }
    }
}
