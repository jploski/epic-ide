package org.epic.debug.util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/*
 * Created on 19.03.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PathMapper
{
	ArrayList mMapping;
			
	public PathMapper()
	{
		mMapping = new ArrayList();
	}
			
	public void add(PathMapping fMap)
	{
		mMapping.add(fMap);	
		Collections.sort(mMapping,(Comparator)mMapping.get(0));
	}
			
	public IPath mapPath( IPath fPath )
	{
		String erg = mapPath(fPath.toString());
		if( erg != null)
			return( new Path(erg));
					
		return( null );
	}
	public String mapPath(String fOrg)
	{
		PathMapping map;
				
		for( int i =0; i< mMapping.size(); ++i)
		{
			map = ((PathMapping) mMapping.get(i));
			if( fOrg.indexOf(map.getOrg())== 0) 
			{
				String erg = map.getMapped() + fOrg.substring(map.getOrg().length());
				return(erg);
			}
				
		}
		return null;				
			
	}
}
class PathMapping implements Comparator
		{
			String mOrg;
			String mMapped;
			
			public PathMapping(String fOrg, String fMapped)
			{
				mOrg=fOrg;
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
		


