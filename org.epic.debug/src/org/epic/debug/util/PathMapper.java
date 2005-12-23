package org.epic.debug.util;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.runtime.*;
import org.epic.debug.PerlDB;
import org.epic.debug.PerlDebugPlugin;

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
public class PathMapper {
	ArrayList mMapping;
	ArrayList mIncLocal;
	ArrayList mIncRemote;

	public PathMapper() {
		mMapping = new ArrayList();
		mIncLocal = new ArrayList();
		mIncRemote = new ArrayList();
	}

	public void add(PathMapping fMap) {
		mMapping.add(fMap);
		Collections.sort(mMapping, (Comparator) mMapping.get(0));
	}

	public IPath mapPath(IPath fPath) {
		String erg = mapPath(fPath.toString());
		if (erg != null)
			return (new Path(erg));

		return (null);
	}
	private String mapPath(String fOrg) {
		PathMapping map;
		String erg;
		
		for (int i = 0; i < mMapping.size(); ++i) {
			
			map = ((PathMapping) mMapping.get(i));
			if (fOrg.indexOf(map.getOrg()) == 0) {
				erg = map.getMapped() + fOrg.substring(map.getOrg().length());
				return (erg);
			}
			erg = mapInc(fOrg);
			if (erg != null)
				return erg;
		}
		erg = mapInc(fOrg);
		if( erg != null )
			return erg;
		return fOrg;

	}

	public void addLocalInc(String fPath) {
		mIncLocal.add(fPath);
	}

	public void addRemoteInc(String fPath) {
		mIncRemote.add(fPath);
	}

	public String mapInc(String fOrg) {
		String incRem;
		String cut = null;
		String mapped;
		File file;
		
		for (int i = 0; (i < mIncRemote.size()) && (cut == null); ++i) {
			incRem = (String) mIncRemote.get(i);

			if (fOrg.startsWith(incRem)) {
				cut = fOrg.substring(incRem.length());
			}
		}

		if (cut != null) {
			for (int i = 0; i < mIncLocal.size(); ++i) {
				mapped = ((String) mIncLocal.get(i)) + cut;
				file = new File(mapped);
				if(file.exists())
					return mapped;
			}
		}
		
		return(null);
	}

	public void initInc(PerlDB fDB) throws CoreException
	{
		PerlDebugPlugin.createDefaultIncPath(mIncLocal);
		fDB.getRemoteInc(mIncRemote);
	}
	
	public void print()
	{
		String t = "\n*****PathMapper*****\n";
		for( int i=0; i < mMapping.size(); ++i)
		{
		 t+= "\n"+((PathMapping)mMapping.get(i)).mOrg+"-->"+((PathMapping)mMapping.get(i)).mMapped+"\n";
		}
		t += "\n++++incMappingRemote++++\n";
		for( int i=0; i < mIncRemote.size(); ++i)
		{
		 t+= "\n"+((String)mIncRemote.get(i))+"\n";
		}
		t += "\n++++incMappingLocal++++\n";
		for( int i=0; i < mIncLocal.size(); ++i)
		{
		 t+= "\n"+((String)mIncLocal.get(i))+"\n";
		}
		System.err.println(t);
	}
}

