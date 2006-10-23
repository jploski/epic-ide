package org.epic.core.util;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.*;
import org.osgi.framework.Bundle;

/**
 * A utility class which enables a plug-in to map a part of its
 * deployment jar to a working directory in the file system.
 * This mapping is necessary because the Perl intepreter cannot
 * access resources (such as scripts) contained in the plug-in jar.
 * 
 * @author jploski
 */
public class ResourceUtilities
{
    /**
     * Recursively extracts resources from the plug-in archive (jar)
     * to a local state directory. Normally, if the target file or
     * directory already exists, the call has no effect. However,
     * if the archive's modification timestamp is more recent than
     * the target's, the target is replaced with the version from
     * the archive.
     *  
     * If the plug-in is not deployed as jar, the call has no effect. 
     * 
     * @param plugin
     *        plug-in whose local state should be updated
     * @param pathInPlugin
     *        path of a file or directory within the plug-in bundle;
     *        if the path refers to a directory, it must end with /
     * @return path to the extracted resource,
     *         or null if the plug-in is not deployed as jar 
     */
    public static File extractResources(
        Plugin plugin,
        String pathInPlugin)
        throws CoreException
    {
        if (!isDeployedAsJar(plugin.getBundle())) return null;
        
        File pluginJar = getBundleJar(plugin.getBundle());
        File location = new File(
            plugin.getStateLocation().toFile(),
            pathInPlugin);
        
        if (location.exists() &&
            location.lastModified() > pluginJar.lastModified())
        {
            // We assume the extracted resources are up-to-date
            return location;
        }
        
        try
        {
            deleteRecursively(location);
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                plugin.getBundle().getSymbolicName(),
                IStatus.OK,
                "Could not delete directory " +
                location.getAbsolutePath() + "; " +
                "please report this as a bug in EPIC",
                e));
        }
        
        try
        {
            ZipFile zipFile = new ZipFile(pluginJar);
            try
            {
                File destDir = location.getParentFile();
                for (Enumeration e = zipFile.entries(); e.hasMoreElements();)
                {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    if (entry.getName().startsWith(pathInPlugin))
                        extractZipEntry(zipFile, entry, destDir);               
                }
                return location;
            }
            finally
            {
                try { zipFile.close(); } catch (IOException e) { }
            }
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                plugin.getBundle().getSymbolicName(),
                IStatus.OK,
                "Could not extract resources to directory " +
                location.getAbsolutePath() + "; " +
                "please report this as a bug in EPIC",
                e));
        }
    }
    
    /**
     * @return the jar file in which the given plug-in is deployed
     *         or null if !isDeployedAsJar(plugin)
     */
    public static File getBundleJar(Bundle plugin) throws CoreException
    {
        try
        {
            URL url = Platform.resolve(plugin.getEntry("/"));
            
            if (url.getProtocol().equalsIgnoreCase("jar"))
            {
                String path = url.getPath();
                assert path.startsWith("file:");
                assert path.endsWith(".jar!/");
                
                path = path.substring(5, path.length()-2);
                return new File(path);
            }
            else return null;
        }
        catch (Exception e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                plugin.getSymbolicName(),
                IStatus.OK,
                "Could not determine plug-in deployment jar; " +
                "please report this as a bug in EPIC",
                e));
        }
    }
    
    /**
     * @return true if the given plug-in is deployed as a single jar;
     *         false if it is deployed as a directory
     * @throws CoreException if we can't tell (this should never happen)
     */
    public static boolean isDeployedAsJar(Bundle plugin)
        throws CoreException
    {
        try
        {
            return Platform.resolve(plugin.getEntry("/"))
                .getProtocol().equalsIgnoreCase("jar");
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                plugin.getSymbolicName(),
                IStatus.OK,
                "Could not determine plug-in deployment type; " +
                "please report this as a bug in EPIC",
                e));
        }
    }

    private static void deleteRecursively(File dir) throws IOException
    {
        File[] files = dir.listFiles();
        if (files == null) return; // nothing to do

        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory() &&
                !files[i].getName().equals(".") &&
                !files[i].getName().equals(".."))
            {
                deleteRecursively(files[i]);
            }
            else if (!files[i].delete()) throw new IOException(
                "Could not delete file " + files[i].getAbsolutePath());
        }
        if (!dir.delete()) throw new IOException(
            "Could not delete dir " + dir.getAbsolutePath());
    }
    
    private static void extractZipEntry(
        ZipFile zipFile, ZipEntry entry, File destBase) throws IOException
    {
        if (entry.isDirectory())
        {
            File outDir = new File(destBase, entry.getName());
            if (!outDir.mkdirs() && !outDir.isDirectory())
                throw new IOException("Failed to extract " + entry.getName());
        }
        else
        {
            InputStream in = null;
            OutputStream out = null;
            
            try
            {
                File outFile = new File(destBase, entry.getName());
                outFile.getParentFile().mkdirs();
                
                in = zipFile.getInputStream(entry);
                out = new BufferedOutputStream(new FileOutputStream(outFile));
                
                byte[] buf = new byte[4096];
                int bread;
                while ((bread = in.read(buf)) != -1)
                    out.write(buf, 0, bread);
            }
            finally
            {
                if (in != null) try { in.close(); } catch (Exception e) { }
                if (out != null) try { out.close(); } catch (Exception e) { }
            }
        }
    }
}
