package org.epic.debug.db;

import org.eclipse.core.runtime.IPath;

/**
 * Stores position of the instruction pointer.
 */
class IPPosition
{
    private final int line;
    private final IPath path;
    
    public IPPosition(IPath path, int line)
    {
        this.path = path;
        this.line = line;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof IPPosition)) return false;
        IPPosition pos = (IPPosition) obj;
        
        return
            path.equals(pos.getPath()) &&
            line == pos.getLine();
    }

    public int getLine()
    {
        return line;
    }

    /**
     * @return path of the stack frame, valid in the file system
     *         of "perl -d"; this path is not necessarily local to EPIC
     */
    public IPath getPath()
    {
        return path;
    }

    public int hashCode()
    {
        return path.hashCode() * 37 + line;
    }
    
    public String toString()
    {
        return path + ":" + line;
    }
}