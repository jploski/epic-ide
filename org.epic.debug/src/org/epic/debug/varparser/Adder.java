package org.epic.debug.varparser;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;

import antlr.InputBuffer;
import antlr.LexerSharedInputState;
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
public class Adder
{
    /**
     * The main method. See the header comment for more details.
     *
     * @param args the commandline arguments
     */
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
    static public void main (String[] args)
        throws Exception
    {
        
        //***************************************
      
        
//		RE mReStackTrace=null;
//		try{
//			mReStackTrace = new RE("^(.)\\s+=\\s+(.*)called from .* \\`([^\\']+)\\'\\s*line (\\d+)\\s*$",RE.REG_MULTILINE, RESyntax.RE_SYNTAX_PERL5); 
//		//mReStackTrace = new RE("^(.)\\s+=\\s+(.*)called from .* \\`([^\\']+)\\'\\s*line (\\d+)\\s*$",RE.REG_MULTILINE, RESyntax.RE_SYNTAX_PERL5);
//				
//				} catch (REException e){ new InstantiationException("Couldn't RegEX");};
//
//		//REMatch[] matches = mReStackTrace.getAllMatches("$ = main::infested called from file `Ambulation.pm' line 10\n");
//		REMatch[] matches = mReStackTrace.getAllMatches("$ = main::infested called from file `Ambulation.pm' line 10\r\n@ = Ambulation::legs(1, 2, 3, 4) called from file `camel_flea' line 7\r\n$ = main::pests('bactrian', 4) called from file `camel_flea' line 4");
//		for( int pos = 0; pos < matches.length; ++pos)
//		{
//			System.out.println("Called Function: "+matches[pos].toString(2));
//			System.out.println("Return Type: "+matches[pos].toString(1));	
//			System.out.println("Caled From:  "+matches[pos].toString(3)+"["+matches[pos].toString(4)+"]");
//		}
		
        //*******************************************
        // the name of the file to read
        String fileName = "F:\\eclipse_3\\eclipse\\workspace\\org.epic.debug\\test.txt";
//   	 String fileName = "F:\\eclipse_3\\eclipse\\workspace\\org.epic.debug\\test.dat";


        // construct the special shared input state that is needed
        // in order to annotate ExtentTokens properly
        
        // construct the lexer
        PerlBaseLexer lex = new PerlLexer (new DataInputStream( new FileInputStream(fileName) ) );

        // construct the parser
       PerlParser par = new PerlParser (lex);
	
		Token tok;
      while(true)
      {
      	tok =lex.nextToken();
      	System.out.println(tok.toString()+" L:"+ tok.getLine() +" C:" + tok.getColumn()+"\n");
      	 if (tok.getType() == 1) break;
      }

        //parse the file
        par.mVarList = new ArrayList();
    //   par.topLevel();
		System.out.println( "Error:"+ par.hasError());
        }
}
