/*
 * Created on 17.07.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug.varparser;

import java.io.InputStream;
import java.io.Reader;

import antlr.CommonHiddenStreamToken;
import antlr.InputBuffer;
import antlr.LexerSharedInputState;
import antlr.Token;
import antlr.TokenStreamException;


public class PerlLexer extends PerlBaseLexer
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
			tok = new CommonHiddenStreamToken();
			tok.setType(INDENT_END);
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
			tok = new CommonHiddenStreamToken();
			tok.setType(INDENT_END);
			mCurrentIndentLevels--;
			mIndentStack.pop();
			//System.out.println("*****IndentEnd\n");
		}
		
		}
	
		return(tok);
	} 
	

	
	

}