package org.epic.core.util;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.*;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * An implementation of IPathMapper which translates Cygwin paths into
 * Windows paths and vice versa.
 */
class CygwinMount
{
	private static String cygDrivePrefix = "/cygdrive/";

	private CygwinMount()
    {
    }
    
    static public String drivePrefix()
    {
    	if (cygDrivePrefix == null)
    	{
    		try 
    		{
				setDrivePrefix();
			} 
    		catch (CoreException e) 
    		{	// Ignore
			}
    	}
    	return cygDrivePrefix;
    }

    static private String setDrivePrefix() throws CoreException
    {
    	try
        {        
            ProcessExecutor executor = new ProcessExecutor();
            String cmd[] = new String[] { "mount", "-p" };
            File	wrkDir = new File(".");

            // Select the user version if available -- otherwise system default
        	Pattern p = Pattern.compile("^([^ \t]+).*user|system", Pattern.MULTILINE);
            Matcher m = p.matcher(executor.execute(cmd, "", wrkDir, null).stdout);
            
            if (m.find())
            {	// user setting provided
                cygDrivePrefix = m.group(1);
            }
        }
        catch (InterruptedException e) { /* can't occur */ }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlEditorPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not execute 'mount' to find out path mappings.\n" +
                "Add Cygwin's 'bin' directory (which contains mount.exe) to your PATH.",
                e));
        }
        return cygDrivePrefix;
    }
}
