package org.epic.debug.varparser;
import java.io.StringReader;

import org.epic.debug.PerlDB;
import org.epic.debug.PerlDebugPlugin;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.TokenStreamHiddenTokenFilter;

/**
 * The main for this example. It accepts the name of a file as a commandline
 * argument, and will interpret the contents of the file.
 *
 * <p>This file is in the public domain.</p>
 *
 * @author Dan Bornstein, danfuzz@milk.com
 */
public class PerlVarParser 
{
	private PerlParser par;
	private PerlDB mDebugger;
	private int mScope;
	public TokenStreamHiddenTokenFilter mFilter;
	   
    public PerlVarParser( PerlDB fDebugger)
    {
      mDebugger = fDebugger;
    }
    
	public java.util.ArrayList parseVars( String fText, int fScope)
	{
		return( parseVars(fText, fScope,new java.util.ArrayList()));
	}
	
    public java.util.ArrayList parseVars( String fText, int fScope, java.util.ArrayList fVarList )
    {
    	boolean hasErrors=false;
    	fText = fText+ System.getProperty("line.separator");
		try{
			
			 //		  Construct the lexer
			PerlLexer lex = new PerlLexer ( new StringReader( fText ) );

			// use nonstandard token object
		    	lex.setTokenObjectClass("antlr.CommonHiddenStreamToken");

			// create the filter
		    	mFilter = new TokenStreamHiddenTokenFilter(lex);
		    	mFilter.hide(PerlLexer.WS); // hide not discard
		    	

			// run parser attached to filtered token stream
		    //	TestParser parser = new TestParser(filter);
		    //	parser.slist(); // Parse the input statements
			
					
	
		// construct the parser
		par = new PerlParser (mFilter);
		par.setLex(lex);
		par.setFilter(mFilter);
		par.setDebugger(mDebugger);
		par.setScope(fScope);
		par.setVarList(fVarList);
    	
		try
		{
			par.topLevel();
			
		} catch (TokenStreamException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
			hasErrors = true;
		}
    	} catch(RecognitionException e){
    	
    	   	par.reportError(e);
    	   	hasErrors = true;
    	}
    	
    	if (  par.hasError() > 0 || hasErrors)
    	{
    		System.out.println("!!!!! Parse Error!!!!");
    		logParsError(fText);
    	}
    	else
    	{
			System.out.println("!!!!! Parse OK!!!!");
    	}
    	return(par.getVars());
    	
    }

    
    private void logParsError(String fText)
    {
			
		
		
		StringBuffer out	=	new StringBuffer();
	    	
    	out.append("*******************************\n");
		out.append("*******************************\n");
		out.append("+++++++Error Parsing Vars++++++\n");
		out.append("*******************************\n");
		out.append("*******************************\n");

			out.append(fText);
			
		out.append("-------------------------------\n");
		out.append("-------------------------------\n");
		out.append("+++++++Error Parsing Vars++++++\n");
		out.append("-------------------------------\n");
		out.append("-------------------------------\n");
		
		
		PerlDebugPlugin.getDefault().logError("Error Parsing Debugger Variables", new Exception(out.toString()));
								 
    }
    
}
