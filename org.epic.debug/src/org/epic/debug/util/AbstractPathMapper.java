package org.epic.debug.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.epic.debug.db.DebuggerInterface;

/**
 * Base class for IPathMapper implementations which translate
 * paths by mapping from one known path prefix to another.
 */
public abstract class AbstractPathMapper implements IPathMapper
{
    private final List epicPathPrefixes;
    private final List dbPathPrefixes;
    
    protected AbstractPathMapper()
    {
        epicPathPrefixes = new ArrayList();
        dbPathPrefixes = new ArrayList();
    }
    
    protected void addMapping(IPath epicPathPrefix, IPath dbPathPrefix)
    {
        epicPathPrefixes.add(epicPathPrefix);
        dbPathPrefixes.add(dbPathPrefix);
    }

    public IPath getDebuggerPath(IPath epicPath, DebuggerInterface db)
    {
        return swapPrefix(epicPath, epicPathPrefixes, dbPathPrefixes);
    }

    public IPath getEpicPath(IPath dbPath)
    {
        return swapPrefix(dbPath, dbPathPrefixes, epicPathPrefixes);
    }    
    
    public boolean requiresEffectiveIncPath()
    {
        return false;
    }
    
    public void setEffectiveIncPath(List inc)
    {
    }
    
    private IPath swapPrefix(IPath path, List srcPrefixes, List targetPrefixes)
    {   
        for (int i = 0; i < srcPrefixes.size(); i++)
        {
            IPath srcPrefix = (IPath) srcPrefixes.get(i);
            
            if (srcPrefix.isPrefixOf(path))
            {
                path = path.removeFirstSegments(srcPrefix.segmentCount());
                return ((IPath) targetPrefixes.get(i)).append(path);
            }
        }
        return null;
    }
}
