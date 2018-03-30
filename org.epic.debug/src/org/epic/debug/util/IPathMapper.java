package org.epic.debug.util;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.epic.debug.db.DebuggerInterface;

/**
 * Implementors are responsible for translating paths from the file
 * system known to "perl -d" to the file system known to EPIC
 * (i.e. the file system which contains the workspace) and vice versa.
 */
public interface IPathMapper
{
    /**
     * @param epicPath
     *        an absolute path valid in the EPIC file system
     * @param db
     *        interface through which the "perl -d" process can be queried
     * @return the corresponding absolute path valid in the debugger
     *         file system or null if the path could not be translated
     */
    public IPath getDebuggerPath(IPath epicPath, DebuggerInterface db);

    /**
     * @param dbPath
     *        an absolute path valid in the debugger file system
     * @return the corresponding absolute path valid in the EPIC file
     *         system or null if the path could not be translated
     */
    public IPath getEpicPath(IPath dbPath);    
    
    /**
     * @return true if the implementation wishes to obtain information
     *         about the effective \@INC path from the debugger;
     *         false otherwise
     */
    public boolean requiresEffectiveIncPath();
    
    /**
     * Provides the debugger's effective \@INC path to the IPathMapper.
     * This method is only invoked if {@link #requiresEffectiveIncPath()}
     * returns true. Its invocation is then guaranteed to occur before
     * any path translations are requested.
     * 
     * @param inc the effective \@INC path, as a list of IPath objects
     */
    public void setEffectiveIncPath(List<IPath> inc);
}
