package org.epic.debug.varparser;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.epic.debug.PerlDB;
import org.epic.debug.PerlDebugPlugin;

import antlr.InputBuffer;
import antlr.LexerSharedInputState;
import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStreamException;

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
	   
    public static class PerlLexer extends PerlBaseLexer
    {
	private static int mNormal = 0;
	private static int mEndIndent = 1;
	
	private int mState;
	private Token mDelayedTok;
	private int mCurrentPos;
	
	 
	public PerlLexer(InputStream in) {
			super(in);
		}
	public PerlLexer(Reader in) {
	super(in);
	init();
	}
	public PerlLexer(InputBuffer ib) {
	super(ib);
	init();
	}
	
	public PerlLexer(LexerSharedInputState state) {
	super(state);
	init();
	}

	private void  init()
	{
		mState = mNormal;
		mDelayedTok = null;
		mCurrentPos = 0;
	}
		
	public Token nextToken() throws TokenStreamException {
		Token tok;

		
		if( mDelayedTok != null)
		{
			if( (mCurrentIndentLevels > 0) 
				&& ((Integer)mIndentStack.peek()).intValue()> mCurrentPos)
				{
				tok = new Token(INDENT_END);
				 mCurrentIndentLevels--;
				 mIndentStack.pop();
				// System.out.println("*****IndentEnd\n");
				}	
			else
				{
					tok = mDelayedTok;
					mCurrentIndent = mCurrentPos;
					mDelayedTok = null;	
				}
		}
		else
		{
			tok = super.nextToken();
			mCurrentPos = tok.getColumn();
			if(tok.getType()== NL)
			{ 
				mCurrentPos++;
				mCurrentPos --;
			}
			if( 
				(tok.getType()!= NL
					|| ( (tok.getType()== NL) && (mCurrentPos == 1) )
				)
				&& (tok.getType()!= INDENT_START)
				&& (mCurrentIndentLevels > 0) 
				&& ((Integer)mIndentStack.peek()).intValue()> mCurrentPos)
			{
				mDelayedTok=tok;
				tok = new Token(INDENT_END);
				mCurrentIndentLevels--;
				mIndentStack.pop();
				//System.out.println("*****IndentEnd\n");
			}
			
			}
			
			return(tok);
		} 
		

		
		
	
    }
    
    
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
		try{
		// Construct the lexer
		PerlBaseLexer lex = new PerlLexer ( new StringReader( fText ) );
        
		// construct the parser
		par = new PerlParser (lex);
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
    	
    	if (  par.hasError() > 1 || hasErrors)
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
