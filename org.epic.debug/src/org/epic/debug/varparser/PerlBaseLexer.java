// $ANTLR : "add.g" -> "PerlBaseLexer.java"$
 package org.epic.debug.varparser; 
import java.io.InputStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import java.io.Reader;
import java.util.Hashtable;
import antlr.InputBuffer;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.Token;
import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.TokenStream;
import antlr.LexerSharedInputState;
import antlr.collections.impl.BitSet;

public class PerlBaseLexer extends antlr.CharScanner implements AddTokenTypes, TokenStream
 {

	int mCurrentIndent = 0;
	int mCurrentIndentLevels=0;
	int mIndentEndToSend =0;
	java.util.Stack mIndentStack = new java.util.Stack();
	
public PerlBaseLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public PerlBaseLexer(Reader in) {
	this(new CharBuffer(in));
}
public PerlBaseLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public PerlBaseLexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = true;
	setCaseSensitive(true);
	literals = new Hashtable();
}

public Token nextToken() throws TokenStreamException {
	Token theRetToken=null;
tryAgain:
	for (;;) {
		Token _token = null;
		int _ttype = Token.INVALID_TYPE;
		resetText();
		try {   // for char stream error handling
			try {   // for lexical error handling
				switch ( LA(1)) {
				case '%':
				{
					mHASH_NAME(true);
					theRetToken=_returnToken;
					break;
				}
				case '@':
				{
					mARRAY_NAME(true);
					theRetToken=_returnToken;
					break;
				}
				case '$':
				{
					mSCALAR_NAME(true);
					theRetToken=_returnToken;
					break;
				}
				case '*':
				{
					mFILE_REF(true);
					theRetToken=_returnToken;
					break;
				}
				case '-':
				{
					mREF_SYMB(true);
					theRetToken=_returnToken;
					break;
				}
				case '"':  case '\'':
				{
					mSTRING(true);
					theRetToken=_returnToken;
					break;
				}
				case '0':  case '1':  case '2':  case '3':
				case '4':  case '5':  case '6':  case '7':
				case '8':  case '9':
				{
					mNUMBER(true);
					theRetToken=_returnToken;
					break;
				}
				case '\t':  case ' ':
				{
					mWS(true);
					theRetToken=_returnToken;
					break;
				}
				case '\n':  case '\r':
				{
					mNL(true);
					theRetToken=_returnToken;
					break;
				}
				case ')':
				{
					mPAREN_CL(true);
					theRetToken=_returnToken;
					break;
				}
				default:
					if ((LA(1)=='(') && (LA(2)=='0')) {
						mADR(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='=') && (LA(2)=='>')) {
						mKEY_ASSIGN(true);
						theRetToken=_returnToken;
					}
					else if ((_tokenSet_0.member(LA(1)))) {
						mMODULE_NAME(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='=') && (true)) {
						mEQ(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='(') && (true)) {
						mPAREN_OP(true);
						theRetToken=_returnToken;
					}
				else {
					if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
				else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				}
				if ( _returnToken==null ) continue tryAgain; // found SKIP token
				_ttype = _returnToken.getType();
				_returnToken.setType(_ttype);
				return _returnToken;
			}
			catch (RecognitionException e) {
				throw new TokenStreamRecognitionException(e);
			}
		}
		catch (CharStreamException cse) {
			if ( cse instanceof CharStreamIOException ) {
				throw new TokenStreamIOException(((CharStreamIOException)cse).io);
			}
			else {
				throw new TokenStreamException(cse.getMessage());
			}
		}
	}
}

	protected final void mPREFIXED_NAME(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PREFIXED_NAME;
		int _saveIndex;
		
		{
		int _cnt85=0;
		_loop85:
		do {
			if ((_tokenSet_1.member(LA(1)))) {
				{
				match(_tokenSet_1);
				}
			}
			else {
				if ( _cnt85>=1 ) { break _loop85; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt85++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mHASH_NAME(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HASH_NAME;
		int _saveIndex;
		
		match("%");
		mPREFIXED_NAME(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mARRAY_NAME(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ARRAY_NAME;
		int _saveIndex;
		
		match("@");
		mPREFIXED_NAME(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSCALAR_NAME(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SCALAR_NAME;
		int _saveIndex;
		
		match("$");
		mPREFIXED_NAME(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFILE_REF(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FILE_REF;
		int _saveIndex;
		
		match("*");
		mPREFIXED_NAME(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMODULE_NAME(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MODULE_NAME;
		int _saveIndex;
		
		boolean synPredMatched92 = false;
		if (((LA(1)=='A') && (LA(2)=='R') && (LA(3)=='R') && (LA(4)=='A'))) {
			int _m92 = mark();
			synPredMatched92 = true;
			inputState.guessing++;
			try {
				{
				match("ARRAY");
				}
			}
			catch (RecognitionException pe) {
				synPredMatched92 = false;
			}
			rewind(_m92);
			inputState.guessing--;
		}
		if ( synPredMatched92 ) {
			match("ARRAY");
			if ( inputState.guessing==0 ) {
				_ttype = ARRAY_REF;
			}
		}
		else {
			boolean synPredMatched94 = false;
			if (((LA(1)=='S') && (LA(2)=='C') && (LA(3)=='A') && (LA(4)=='L'))) {
				int _m94 = mark();
				synPredMatched94 = true;
				inputState.guessing++;
				try {
					{
					match("SCALAR");
					}
				}
				catch (RecognitionException pe) {
					synPredMatched94 = false;
				}
				rewind(_m94);
				inputState.guessing--;
			}
			if ( synPredMatched94 ) {
				match("SCALAR");
				if ( inputState.guessing==0 ) {
					_ttype = SCALAR_REF;
				}
			}
			else {
				boolean synPredMatched96 = false;
				if (((LA(1)=='H') && (LA(2)=='A') && (LA(3)=='S') && (LA(4)=='H'))) {
					int _m96 = mark();
					synPredMatched96 = true;
					inputState.guessing++;
					try {
						{
						match("HASH");
						}
					}
					catch (RecognitionException pe) {
						synPredMatched96 = false;
					}
					rewind(_m96);
					inputState.guessing--;
				}
				if ( synPredMatched96 ) {
					match("HASH");
					if ( inputState.guessing==0 ) {
						_ttype = HASH_REF;
					}
				}
				else {
					boolean synPredMatched98 = false;
					if (((LA(1)=='C') && (LA(2)=='O') && (LA(3)=='D') && (LA(4)=='E'))) {
						int _m98 = mark();
						synPredMatched98 = true;
						inputState.guessing++;
						try {
							{
							match("CODE");
							}
						}
						catch (RecognitionException pe) {
							synPredMatched98 = false;
						}
						rewind(_m98);
						inputState.guessing--;
					}
					if ( synPredMatched98 ) {
						match("CODE");
						if ( inputState.guessing==0 ) {
							_ttype = CODE_REF;
						}
					}
					else {
						boolean synPredMatched102 = false;
						if (((LA(1)=='G') && (LA(2)=='L') && (LA(3)=='O') && (LA(4)=='B'))) {
							int _m102 = mark();
							synPredMatched102 = true;
							inputState.guessing++;
							try {
								{
								match("GLOB");
								}
							}
							catch (RecognitionException pe) {
								synPredMatched102 = false;
							}
							rewind(_m102);
							inputState.guessing--;
						}
						if ( synPredMatched102 ) {
							match("GLOB");
							if ( inputState.guessing==0 ) {
								_ttype = GLOB;
							}
						}
						else {
							boolean synPredMatched104 = false;
							if (((LA(1)=='F') && (LA(2)=='i') && (LA(3)=='l') && (LA(4)=='e'))) {
								int _m104 = mark();
								synPredMatched104 = true;
								inputState.guessing++;
								try {
									{
									match("FileHandle");
									}
								}
								catch (RecognitionException pe) {
									synPredMatched104 = false;
								}
								rewind(_m104);
								inputState.guessing--;
							}
							if ( synPredMatched104 ) {
								match("FileHandle");
								if ( inputState.guessing==0 ) {
									_ttype = FILE_HANDLE;
								}
							}
							else {
								boolean synPredMatched106 = false;
								if (((LA(1)=='f') && (LA(2)=='i') && (LA(3)=='l') && (LA(4)=='e'))) {
									int _m106 = mark();
									synPredMatched106 = true;
									inputState.guessing++;
									try {
										{
										match("fileno(");
										}
									}
									catch (RecognitionException pe) {
										synPredMatched106 = false;
									}
									rewind(_m106);
									inputState.guessing--;
								}
								if ( synPredMatched106 ) {
									{
									match("fileno(");
									}
									{
									int _cnt109=0;
									_loop109:
									do {
										if (((LA(1) >= '0' && LA(1) <= '9'))) {
											matchRange('0','9');
										}
										else {
											if ( _cnt109>=1 ) { break _loop109; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
										}
										
										_cnt109++;
									} while (true);
									}
									match(')');
									if ( inputState.guessing==0 ) {
										_ttype = FILE_NO;
									}
								}
								else {
									boolean synPredMatched100 = false;
									if (((LA(1)=='R') && (LA(2)=='E') && (LA(3)=='F') && (true))) {
										int _m100 = mark();
										synPredMatched100 = true;
										inputState.guessing++;
										try {
											{
											match("REF");
											}
										}
										catch (RecognitionException pe) {
											synPredMatched100 = false;
										}
										rewind(_m100);
										inputState.guessing--;
									}
									if ( synPredMatched100 ) {
										match("REF");
										if ( inputState.guessing==0 ) {
											_ttype = REF;
										}
									}
									else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true)) {
										mPURE_NAME(false);
										if ( inputState.guessing==0 ) {
											_ttype = PURE_NAME;
										}
									}
									else {
										throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
									}
									}}}}}}}
									if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
										_token = makeToken(_ttype);
										_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
									}
									_returnToken = _token;
								}
								
	protected final void mPURE_NAME(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PURE_NAME;
		int _saveIndex;
		
		{
		mFIRST_PURE_NAME_CHAR(false);
		}
		{
		_loop117:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				mPURE_NAME_CHAR(false);
			}
			else {
				break _loop117;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mPURE_NAME_CHAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PURE_NAME_CHAR;
		int _saveIndex;
		
		{
		match(_tokenSet_2);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mFIRST_PURE_NAME_CHAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FIRST_PURE_NAME_CHAR;
		int _saveIndex;
		
		{
		match(_tokenSet_0);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mADR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ADR;
		int _saveIndex;
		
		match("(0x");
		{
		int _cnt120=0;
		_loop120:
		do {
			switch ( LA(1)) {
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				matchRange('0','9');
				break;
			}
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':
			{
				matchRange('a','f');
				break;
			}
			default:
			{
				if ( _cnt120>=1 ) { break _loop120; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt120++;
		} while (true);
		}
		match(')');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mREF_SYMB(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = REF_SYMB;
		int _saveIndex;
		
		match("->");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mKEY_ASSIGN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = KEY_ASSIGN;
		int _saveIndex;
		
		match("=>");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSTRING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STRING;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '\'':
		{
			mSTRING1(false);
			break;
		}
		case '"':
		{
			mSTRING2(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSTRING1(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STRING1;
		int _saveIndex;
		
		match('\'');
		{
		_loop128:
		do {
			if ((_tokenSet_3.member(LA(1)))) {
				{
				{
				match(_tokenSet_3);
				}
				}
			}
			else if ((LA(1)=='\\')) {
				mCHAR_ESC(false);
			}
			else {
				break _loop128;
			}
			
		} while (true);
		}
		match('\'');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSTRING2(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STRING2;
		int _saveIndex;
		
		match('\"');
		{
		_loop133:
		do {
			if ((_tokenSet_4.member(LA(1)))) {
				{
				{
				match(_tokenSet_4);
				}
				}
			}
			else if ((LA(1)=='\\')) {
				mCHAR_ESC(false);
			}
			else {
				break _loop133;
			}
			
		} while (true);
		}
		match('\"');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCHAR_ESC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CHAR_ESC;
		int _saveIndex;
		
		match('\\');
		{
		switch ( LA(1)) {
		case 'n':
		{
			match('n');
			break;
		}
		case 'r':
		{
			match('r');
			break;
		}
		case 't':
		{
			match('t');
			break;
		}
		case 'b':
		{
			match('b');
			break;
		}
		case 'c':
		{
			match("c");
			{
			matchRange('\u0000','\ufffe');
			}
			break;
		}
		case 'f':
		{
			match('f');
			break;
		}
		case '"':
		{
			match('\"');
			break;
		}
		case '\'':
		{
			match('\'');
			break;
		}
		case '\\':
		{
			match('\\');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNUMBER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMBER;
		int _saveIndex;
		
		{
		int _cnt136=0;
		_loop136:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				matchRange('0','9');
			}
			else {
				if ( _cnt136>=1 ) { break _loop136; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt136++;
		} while (true);
		}
		{
		if ((LA(1)=='.')) {
			match('.');
			{
			int _cnt139=0;
			_loop139:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					matchRange('0','9');
				}
				else {
					if ( _cnt139>=1 ) { break _loop139; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt139++;
			} while (true);
			}
		}
		else {
		}
		
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEQ(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EQ;
		int _saveIndex;
		
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mWS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WS;
		int _saveIndex;
		
		{
		int _cnt143=0;
		_loop143:
		do {
			switch ( LA(1)) {
			case ' ':
			{
				match(' ');
				break;
			}
			case '\t':
			{
				match('\t');
				break;
			}
			default:
			{
				if ( _cnt143>=1 ) { break _loop143; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt143++;
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			
				if(  makeToken(WS).getColumn() != 1)
				{
					//if(mIgnoreWS)$setType(Token.SKIP);
					//else 
					//$setType(PURE_NAME);
				}
				    else
				    {
				    	int new_col =   text.length()-_begin;
				    	//printConsole("§§§Indent: "+new_col+"§§§§§\n");
				    	if( new_col == mCurrentIndent)
				    		_ttype = Token.SKIP;
					if( new_col > mCurrentIndent)
				    		{
				    			_ttype = INDENT_START;
				    			mCurrentIndentLevels++;
				    			mIndentStack.push( new Integer(new_col) );
				    			mCurrentIndent =new_col;
				    		}
				    		else
				    		 _ttype = Token.SKIP;
			
					/*if( new_col < mCurrentIndent)
				    		{
				    			$setType(INDENT_END);
				    			mCurrentIndentLevels--;
				    			mIndentStack.pop();
				    		}*/
				    	//mCurrentIndent =new_col;
				    }
			
			
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NL;
		int _saveIndex;
		
		{
		if ((LA(1)=='\r') && (LA(2)=='\n')) {
			match("\r\n");
		}
		else if ((LA(1)=='\r') && (true)) {
			match('\r');
		}
		else if ((LA(1)=='\n')) {
			match('\n');
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		if ( inputState.guessing==0 ) {
			newline ();
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPAREN_OP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PAREN_OP;
		int _saveIndex;
		
		match('(');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPAREN_CL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PAREN_CL;
		int _saveIndex;
		
		match(')');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	
	private static final long[] mk_tokenSet_0() {
		long[] data = new long[2048];
		data[0]=-2593835568731268609L;
		data[1]=-2L;
		for (int i = 2; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[2048];
		data[0]=-4294968833L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[2048];
		data[0]=-2305846312043554305L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[2048];
		data[0]=-549755813889L;
		data[1]=-268435457L;
		for (int i = 2; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = new long[2048];
		data[0]=-17179869185L;
		data[1]=-268435457L;
		for (int i = 2; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	
	}
