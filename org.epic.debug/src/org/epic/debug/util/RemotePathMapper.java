package org.epic.debug.util;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.epic.core.PerlCore;
import org.epic.debug.db.DebuggerInterface;

/**
 * Maps paths of a remote machine to local (EPIC) paths by attempting
 * look up a file in the local \@INC path. 
 */
public class RemotePathMapper extends AbstractPathMapper
{
    private static final IPathChecker localPathChecker = new IPathChecker() {
        public boolean fileExists(IPath path)
        {
            return path.toFile().exists();
        } };
    
    private final String remoteProjectDir;
    private final List<IPath> epicInc;
    private List<IPath> debuggerInc;
    
    public RemotePathMapper(IProject project, String remoteProjectDir)
        throws CoreException
    {
        this.remoteProjectDir = remoteProjectDir;
        
        epicInc = convertFilesToPaths(
            PerlCore.create(project).getEffectiveIncPath());
        
        IPath projectPath = project.getLocation();
        try { projectPath = Path.fromOSString(projectPath.toFile().getCanonicalPath()); }
        catch (IOException e) { }
        
        addMapping(projectPath, new Path(remoteProjectDir));
    }
    
    public IPath getDebuggerPath(IPath epicPath, DebuggerInterface db)
    {
        // Paths under project dir are handled by the superclass:
        IPath ret = super.getDebuggerPath(epicPath, db);
        if (ret != null) return ret;
        
        IPath convertedPath = convertPath(
            epicPath, epicInc, debuggerInc, new RemotePathChecker(db));
        
        // cache for later
        if (convertedPath != null) addMapping(epicPath, convertedPath);
        return convertedPath;
    }
    
    public IPath getEpicPath(IPath dbPath)
    {
        // Paths under remoteProjectDir are handled by the superclass:
        IPath ret = super.getEpicPath(dbPath);
        if (ret != null) return ret;
        
        return convertPath(
            dbPath, debuggerInc, epicInc, localPathChecker);
    }
    
    public boolean requiresEffectiveIncPath()
    {
        return true;
    }
    
    public void setEffectiveIncPath(List<IPath> inc)
    {
        this.debuggerInc = inc;
    }
    
    private List<IPath> convertFilesToPaths(List<File> files)
    {
        List<IPath> paths = new ArrayList<IPath>(files.size());
        
        for (Iterator<File> i = files.iterator(); i.hasNext();)
        {
            File file = i.next();
            try { paths.add(Path.fromOSString(file.getCanonicalPath())); }
            catch (IOException e) { paths.add(Path.fromOSString(file.getAbsolutePath())); }
        }
        return paths;
    }
    
    private IPath convertPath(
        IPath path,
        List<IPath> sourceIncDirs,
        List<IPath> targetIncDirs,
        IPathChecker checker)
    {
        IPath relativePath = makeRelative(path, sourceIncDirs);
        if (relativePath == null) return null;

        for (Iterator<IPath> i = targetIncDirs.iterator(); i.hasNext();)
        {
            IPath incDir = i.next();            
            IPath convertedPath = incDir.append(relativePath);
            if (checker.fileExists(convertedPath)) return convertedPath;
        }
        return null;
    }
    
    private IPath makeRelative(IPath dbPath, List<IPath> incDirs)
    {
        for (Iterator<IPath> i = incDirs.iterator(); i.hasNext();)
        {
            IPath incDir = i.next();
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
    
    private static interface IPathChecker
    {
        public boolean fileExists(IPath path);
    }
    
    private static class RemotePathChecker implements IPathChecker
    {
        private final DebuggerInterface db;
        
        public RemotePathChecker(DebuggerInterface db)
        {
            this.db = db;
        }
        
        public boolean fileExists(IPath path)
        {
            try { return db.fileExists(path); }
            catch (IOException e) { return false; /* too bad :( */ }
        }
    }
}
