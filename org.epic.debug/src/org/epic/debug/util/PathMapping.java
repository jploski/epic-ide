package org.epic.debug.util;

import java.util.Comparator;

import org.eclipse.core.runtime.Path;

public class PathMapping implements Comparator
{
	String mOrg;
	String mMapped;
	
	public PathMapping(String fOrg, String fMapped)
	{
		mOrg= new Path(fOrg).toString();
		mMapped=fMapped;
	}
			
	public String getOrg()
	{
		return mOrg;
	}

	public String getMapped()
	{
		return mMapped;
	}
			
	public int compare(Object arg0, Object arg1) {
			
		Path p0 = new Path(((PathMapping)arg0).mOrg);
		Path p1 = new Path(((PathMapping)arg1).mOrg);
		
		if( p0.segmentCount() ==  p1.segmentCount())
		 return 0;
		 
		if( p0.segmentCount() >  p1.segmentCount())
						 return -1;
		return 1;
		 
	}
	
}
