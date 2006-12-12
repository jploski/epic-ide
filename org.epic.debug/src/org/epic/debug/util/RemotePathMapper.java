package org.epic.debug.util;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.epic.core.PerlCore;

/**
 * Maps paths of a remote machine to local (EPIC) paths by attempting
 * look up a file in the local \@INC path. 
 */
public class RemotePathMapper extends AbstractPathMapper
{
    private final String remoteProjectDir;
    private final List epicInc;
    private List debuggerInc;
    
    public RemotePathMapper(IProject project, String remoteProjectDir)
        throws CoreException
    {
        this.remoteProjectDir = remoteProjectDir;
        
        epicInc = convertFilesToPaths(
            PerlCore.create(project).getEffectiveIncPath());
        
        addMapping(project.getLocation(), new Path(remoteProjectDir));
    }
    
    public IPath getEpicPath(IPath dbPath)
    {
        // Paths under remoteProjectDir are handled by the superclass:
        IPath ret = super.getEpicPath(dbPath);
        if (ret != null) return ret;
        
        IPath relativePath = makeRelative(dbPath);
        if (relativePath == null) return null;

        for (Iterator i = epicInc.iterator(); i.hasNext();)
        {
            IPath incDir = (IPath) i.next();            
            IPath epicPath = incDir.append(relativePath);            
            if (epicPath.toFile().exists()) return epicPath;
        }
        return null;
    }
    
    public boolean requiresEffectiveIncPath()
    {
        return true;
    }
    
    public void setEffectiveIncPath(List inc)
    {
        this.debuggerInc = inc;
    }
    
    private List convertFilesToPaths(List files)
    {
        List paths = new ArrayList(files.size());
        
        for (Iterator i = files.iterator(); i.hasNext();)
        {
            File file = (File) i.next();
            paths.add(Path.fromOSString(file.getAbsolutePath()));
        }
        return paths;
    }
    
    private IPath makeRelative(IPath dbPath)
    {
        for (Iterator i = debuggerInc.iterator(); i.hasNext();)
        {
            IPath incDir = (IPath) i.next();
            if (incDir.isPrefixOf(dbPath))
                return dbPath.removeFirstSegments(incDir.segmentCount());
        }
        return null;
    }

    public void addLinkedFolderMapping(IFolder folder)
    {
        addMapping(
            folder.getLocation(),
            new Path(remoteProjectDir + "/" +
                folder.getProjectRelativePath().toString()));
    }
}
