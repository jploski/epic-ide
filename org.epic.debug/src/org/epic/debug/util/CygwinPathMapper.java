package org.epic.debug.util;

import gnu.regexp.*;

import java.io.*;

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
                executor.execute(new String[] { "mount" }, "", new File("."));
            
            initMappings(output.stdout.replaceAll("\n", "\r\n"));
        }
        catch (InterruptedException e) { /* can't occur */ }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not execute 'mount' to find out path mappings",
                e));
        }            
    }
    
    private void initMappings(String mountOutput) throws CoreException
    {
        REMatch[] matches = parseMountRegexp().getAllMatches(mountOutput);

        for (int i = 0; i < matches.length; ++i)
        {
            addMapping(
                new Path(matches[i].toString(1)),
                new Path(matches[i].toString(2)));
        }
    }
    
    private RE parseMountRegexp() throws CoreException
    {
        try
        {
            // e.g. "f:\cygwin\bin on /usr/bin type system (binmode)"
            return new RE("^(.+)\\s+on\\s+(/.*) type",
                RE.REG_MULTILINE, RESyntax.RE_SYNTAX_PERL5);
        }
        catch (REException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Failed to create regexp",
                e));
        }
    }
}
