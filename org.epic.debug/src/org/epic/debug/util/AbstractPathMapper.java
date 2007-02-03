package org.epic.debug.util;

import java.util.*;

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
    
    private IPath swapPrefix(IPath path, final List srcPrefixes, List targetPrefixes)
    {
        // We want to iterate over srcPrefixes in descending order by length,
        // so that we replace the longest possible srcPrefix. Compute the order now:
        Integer[] srcPrefixI = new Integer[srcPrefixes.size()];
        for (int i = 0; i < srcPrefixI.length; i++) srcPrefixI[i] = new Integer(i);
        Arrays.sort(srcPrefixI, new Comparator() {
            public int compare(Object o1, Object o2)
            {
                int i1 = ((Integer) o1).intValue();
                int i2 = ((Integer) o2).intValue();
                
                return
                    srcPrefixes.get(i2).toString().length() -
                    srcPrefixes.get(i1).toString().length();
            } });
        
        for (int i = 0; i < srcPrefixI.length; i++)
        {
            IPath srcPrefix = (IPath) srcPrefixes.get(srcPrefixI[i].intValue());
            
            if (srcPrefix.isPrefixOf(path))
            {
                path = path.removeFirstSegments(srcPrefix.segmentCount());
                return ((IPath) targetPrefixes.get(srcPrefixI[i].intValue())).append(path);
            }
        }
        return null;
    }
}
