package org.epic.debug.util;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.epic.debug.db.DebuggerInterface;

/**
 * An implementation of IPathMapper which "translates" paths into
 * themselves, suitable if both "perl -d" and EPIC access the same
 * file system.
 */
public class NullPathMapper implements IPathMapper
{
    public IPath getDebuggerPath(IPath epicPath, DebuggerInterface db)
    {
        return epicPath;
    }
    
    public IPath getEpicPath(IPath dbPath)
    {
        return dbPath;
    }
    
    public boolean requiresEffectiveIncPath()
    {
        return false;
    }
    
    public void setEffectiveIncPath(List<IPath> inc)
    {
    }
}
