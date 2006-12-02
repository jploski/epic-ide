package org.epic.debug.util;

import java.util.Comparator;

import org.eclipse.core.runtime.Path;

public class PathMapping implements Comparator
{
    private final String original;
    private final String mapped;

    public PathMapping(String fOrg, String fMapped)
    {
        original = new Path(fOrg).toString();
        mapped = fMapped;
    }

    public String getOriginal()
    {
        return original;
    }

    public String getMapped()
    {
        return mapped;
    }

    public int compare(Object arg0, Object arg1)
    {
        Path p0 = new Path(((PathMapping) arg0).original);
        Path p1 = new Path(((PathMapping) arg1).original);

        if (p0.segmentCount() == p1.segmentCount()) return 0;
        if (p0.segmentCount() > p1.segmentCount()) return -1;
        return 1;
    }
}
