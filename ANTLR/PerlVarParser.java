import java.io.Reader;
import java.io.InputStream;
import java.io.StringReader;
import antlr.InputBuffer;
import antlr.Token;
/*import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.MismatchedCharException;
import antlr.TokenStream;
import antlr.ANTLRHashString;*/
import antlr.LexerSharedInputState;
/*import antlr.collections.impl.BitSet;
import antlr.SemanticException;*/
import antlr.TokenStreamException;
import org.epic.debug.PerlDB;
import java.io.*;

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
				 System.out.println("*****IndentEnd\n");
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
				System.out.println("*****IndentEnd\n");
			}
			
			}
			
			return(tok);
		} 
		

		
		
	
    }
    
    
    public PerlVarParser( PerlDB fDebugger)
    {
      mDebugger = fDebugger;
    }
    
    public PerlDebugVar[] parseVars( String fText)
    {
		try{
		// construct the lexer
		PerlBaseLexer lex = new PerlLexer ( new StringReader( fText ) );
        
		// construct the parser
		par = new PerlParser (lex);
		par.setDebugger(mDebugger);
    	
		par.topLevel();
    	} catch(Exception e){};	
    	
    	if ( par.hasError() > 1)
    	{
    		System.out.println("!!!!! Paars Error!!!!");
    		logParsError(fText);
    	}
    	else
    	{
			System.out.println("!!!!! Paars OK!!!!");
    	}
    	return(par.getVarArray());
    	
    }

    
    private void logParsError(String fText)
    {
		PrintStream out = null;
				
    	try{
    	
		out = new PrintStream( new FileOutputStream("c:\\ParsError.log",true),true);
    	}catch(Exception e) {System.err.println("Could not open ParsError-Logfile !!!");}						
    	
    	out.println("*******************************");
		out.println("*******************************");
		out.println("+++++++Error Parsing Vars++++++");
		out.println("*******************************");
		out.println("*******************************");

			out.println(fText);
			
		out.println("-------------------------------");
		out.println("-------------------------------");
		out.println("+++++++Error Parsing Vars++++++");
		out.println("-------------------------------");
		out.println("-------------------------------");
		
		out.close();
								 
    }
    
}
