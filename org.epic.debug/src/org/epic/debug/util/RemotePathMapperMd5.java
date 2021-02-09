package org.epic.debug.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.epic.core.PerlCore;
import org.epic.debug.db.DebuggerInterface;

/**
 * Maps paths of a remote machine to local (EPIC) paths by attempting
 * look up a file in the local \@INC path.  This version does not care about the path,
 * but only the filename.  This allows the remote setup to be different than the project setup
 * However, the necessary price is that this mapper cares very much about file contents and 
 * will not map a remote file to a local file unless they have the same sha1.  (it ignores line ending style)
 */
public class RemotePathMapperMd5 extends RemotePathMapper
{
	private DebuggerInterface db;
	private IProject project;
    public RemotePathMapperMd5(IProject project)
        throws CoreException
    {   
    	super(project, "");
    	this.project = project;
    }
    
    public IPath getEpicPath(IPath dbPath)
    {
        return findMatch(
            dbPath, debuggerInc, epicInc);
    }
            
    private IPath findMatch(
        IPath path,
        List sourceIncDirs,
        List targetIncDirs)
    {
    	if(db==null) {
    		return null;
    	}
    	try{
    		String md5 = db.getScriptMd5(path);
    		if(md5MatchCache.containsKey(md5)){
    			return md5MatchCache.get(md5);
    		}
    		IPath prjMatch=findMatch(project, path, md5);
    		if(prjMatch != null) return prjMatch;
    		
    		IPath wMatch=findMatch(project.getWorkspace().getRoot(), path, md5);
    		if(wMatch != null) return wMatch;
    		
    		
			//Nothing in the project or workspace matched.  get crazy: try to download the file
			String sourceCode=db.eval("epic_breakpoints::get_script_source('"+path+"');");
			File ad=new File(project.getLocation().toFile(), "AUTO DOWNLOADED REMOTE PERL SCRIPTS\\"+md5);
			if(!ad.exists()) ad.mkdirs();
			File f= new File(ad, path.toFile().getName());
			
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(sourceCode);
			bw.close();
			
			org.eclipse.core.runtime.Path path2=new org.eclipse.core.runtime.Path(project.getLocation().toString()+"\\AUTO DOWNLOADED REMOTE PERL SCRIPTS\\"+md5+"\\"+path.toFile().getName());
			project.refreshLocal(project.DEPTH_INFINITE, null);
			
			md5MatchCache.put(md5, path2);
			return path2;
    	}catch (Exception e){
    		e.printStackTrace();
    	}
        return null;
    }

	private IPath findMatch(IContainer container, IPath path, String md5) throws CoreException,
			Exception {
		for(IResource res: container.members()){
			if(res instanceof IContainer){
				IPath subMatch=findMatch((IContainer)res, path, md5);
				if(subMatch!=null) return subMatch;
			}
			else if(!res.getFullPath().toFile().getAbsolutePath().contains("epic_links") &&
					res.getName().equals(path.toFile().getName()) &&
					md5.equals(md5Hex(res.getLocation()))){
				md5MatchCache.put(md5, res.getLocation());
				return res.getLocation();
			}
		}
		return null;
	}
    
    private Hashtable<String, IPath> md5MatchCache=new Hashtable<String, IPath>();
    public String md5Hex(IPath path) throws Exception{
    	File f=path.toFile();
        MessageDigest md5=MessageDigest.getInstance("MD5");
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) {
           line = line.replaceAll("(\\r|\\n)", "");
           md5.update(line.getBytes());
        }
        br.close();
        String hexdigest=toHex(md5.digest());
        return hexdigest;
	}
	
    public static String toHex(byte[] bytes) {
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "x", bi);
	}

	public void setDebuggerInterface(DebuggerInterface db) {
		this.db=db;
	}
}
