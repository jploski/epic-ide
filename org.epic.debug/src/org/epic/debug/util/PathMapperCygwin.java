package org.epic.debug.util;
import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RESyntax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
public class PathMapperCygwin extends PathMapper {

	public PathMapperCygwin()
	{
		super();

		char[] buf = new char[1024];
		int count;
		StringBuffer debugOutput = new StringBuffer();
		RE mReParseCygwinMount=null;
		
		try {
			mReParseCygwinMount = new RE("^(.+)\\s+on\\s+(/.*) type",RE.REG_MULTILINE , RESyntax.RE_SYNTAX_PERL5);
		} catch (REException e) {
			e.printStackTrace();
		}
		
		Process mProcess = null;
		try {
			mProcess = Runtime.getRuntime().exec("mount");
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		BufferedReader output = new BufferedReader(
						new InputStreamReader( mProcess.getInputStream())
						);
//		mProcess.exitValue()
	
	
		try {
			do{
			count = output.read(buf);
			if( count >0)
				debugOutput.append(buf);
			}
			while( count > 0);
		} catch (IOException e2) {e2.printStackTrace();}
		String erg = debugOutput.toString();
		System.out.println(erg);
		
		erg = erg.replaceAll("\n","\r\n");
				REMatch[] matches = mReParseCygwinMount.getAllMatches(erg);
		
				
		//PathMapper mapping = new PathMapper();
		for( int pos = 0; pos < matches.length; ++pos)
				{
					//System.out.println("Win:: "+matches[pos].toString(1));
					//System.out.println("Cyg:: "+matches[pos].toString(2));
					add(new PathMapping(matches[pos].toString(2), matches[pos].toString(1)));
				}
		
		}
	}

