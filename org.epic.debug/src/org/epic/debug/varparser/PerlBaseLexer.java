// $ANTLR : "add.g" -> "PerlBaseLexer.java"$
 package org.epic.debug.varparser; 
import java.io.InputStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import antlr.ANTLRException;
import java.io.Reader;
import java.util.Hashtable;
import antlr.CharScanner;
import antlr.InputBuffer;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.Token;
import antlr.CommonToken;
import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.MismatchedCharException;
import antlr.TokenStream;
import antlr.ANTLRHashString;
import antlr.LexerSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.SemanticException;

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
		int _cnt2889=0;
		_loop2889:
		do {
			if ((_tokenSet_1.member(LA(1)))) {
				{
				match(_tokenSet_1);
				}
			}
			else {
				if ( _cnt2889>=1 ) { break _loop2889; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt2889++;
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
		
		boolean synPredMatched2896 = false;
		if (((LA(1)=='A') && (LA(2)=='R') && (LA(3)=='R'))) {
			int _m2896 = mark();
			synPredMatched2896 = true;
			inputState.guessing++;
			try {
				{
				match("ARRAY");
				}
			}
			catch (RecognitionException pe) {
				synPredMatched2896 = false;
			}
			rewind(_m2896);
			inputState.guessing--;
		}
		if ( synPredMatched2896 ) {
			match("ARRAY");
			if ( inputState.guessing==0 ) {
				_ttype = ARRAY_REF;
			}
		}
		else {
			boolean synPredMatched2898 = false;
			if (((LA(1)=='S') && (LA(2)=='C') && (LA(3)=='A'))) {
				int _m2898 = mark();
				synPredMatched2898 = true;
				inputState.guessing++;
				try {
					{
					match("SCALAR");
					}
				}
				catch (RecognitionException pe) {
					synPredMatched2898 = false;
				}
				rewind(_m2898);
				inputState.guessing--;
			}
			if ( synPredMatched2898 ) {
				match("SCALAR");
				if ( inputState.guessing==0 ) {
					_ttype = SCALAR_REF;
				}
			}
			else {
				boolean synPredMatched2900 = false;
				if (((LA(1)=='H') && (LA(2)=='A') && (LA(3)=='S'))) {
					int _m2900 = mark();
					synPredMatched2900 = true;
					inputState.guessing++;
					try {
						{
						match("HASH");
						}
					}
					catch (RecognitionException pe) {
						synPredMatched2900 = false;
					}
					rewind(_m2900);
					inputState.guessing--;
				}
				if ( synPredMatched2900 ) {
					match("HASH");
					if ( inputState.guessing==0 ) {
						_ttype = HASH_REF;
					}
				}
				else {
					boolean synPredMatched2902 = false;
					if (((LA(1)=='C') && (LA(2)=='O') && (LA(3)=='D'))) {
						int _m2902 = mark();
						synPredMatched2902 = true;
						inputState.guessing++;
						try {
							{
							match("CODE");
							}
						}
						catch (RecognitionException pe) {
							synPredMatched2902 = false;
						}
						rewind(_m2902);
						inputState.guessing--;
					}
					if ( synPredMatched2902 ) {
						match("CODE");
						if ( inputState.guessing==0 ) {
							_ttype = CODE_REF;
						}
					}
					else {
						boolean synPredMatched2904 = false;
						if (((LA(1)=='R') && (LA(2)=='E') && (LA(3)=='F'))) {
							int _m2904 = mark();
							synPredMatched2904 = true;
							inputState.guessing++;
							try {
								{
								match("REF");
								}
							}
							catch (RecognitionException pe) {
								synPredMatched2904 = false;
							}
							rewind(_m2904);
							inputState.guessing--;
						}
						if ( synPredMatched2904 ) {
							match("REF");
							if ( inputState.guessing==0 ) {
								_ttype = REF;
							}
						}
						else {
							boolean synPredMatched2906 = false;
							if (((LA(1)=='F') && (LA(2)=='i') && (LA(3)=='l'))) {
								int _m2906 = mark();
								synPredMatched2906 = true;
								inputState.guessing++;
								try {
									{
									match("FileHandle");
									}
								}
								catch (RecognitionException pe) {
									synPredMatched2906 = false;
								}
								rewind(_m2906);
								inputState.guessing--;
							}
							if ( synPredMatched2906 ) {
								match("FileHandle");
								if ( inputState.guessing==0 ) {
									_ttype = FILE_HANDLE;
								}
							}
							else {
								boolean synPredMatched2908 = false;
								if (((LA(1)=='f') && (LA(2)=='i') && (LA(3)=='l'))) {
									int _m2908 = mark();
									synPredMatched2908 = true;
									inputState.guessing++;
									try {
										{
										match("fileno(");
										}
									}
									catch (RecognitionException pe) {
										synPredMatched2908 = false;
									}
									rewind(_m2908);
									inputState.guessing--;
								}
								if ( synPredMatched2908 ) {
									{
									match("fileno(");
									}
									{
									int _cnt2911=0;
									_loop2911:
									do {
										if (((LA(1) >= '0' && LA(1) <= '9'))) {
											matchRange('0','9');
										}
										else {
											if ( _cnt2911>=1 ) { break _loop2911; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
										}
										
										_cnt2911++;
									} while (true);
									}
									match(')');
									if ( inputState.guessing==0 ) {
										_ttype = FILE_NO;
									}
								}
								else if ((_tokenSet_0.member(LA(1))) && (true) && (true)) {
									mPURE_NAME(false);
									if ( inputState.guessing==0 ) {
										_ttype = PURE_NAME;
									}
								}
								else {
									throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
								}
								}}}}}}
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
		_loop2919:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				mPURE_NAME_CHAR(false);
			}
			else {
				break _loop2919;
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
		int _cnt2922=0;
		_loop2922:
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
				if ( _cnt2922>=1 ) { break _loop2922; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt2922++;
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
		_loop2930:
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
				break _loop2930;
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
		_loop2935:
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
				break _loop2935;
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
			match('c');
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
		int _cnt2938=0;
		_loop2938:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				matchRange('0','9');
			}
			else {
				if ( _cnt2938>=1 ) { break _loop2938; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt2938++;
		} while (true);
		}
		{
		if ((LA(1)=='.')) {
			match('.');
			{
			int _cnt2941=0;
			_loop2941:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					matchRange('0','9');
				}
				else {
					if ( _cnt2941>=1 ) { break _loop2941; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt2941++;
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
		int _cnt2945=0;
		_loop2945:
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
				if ( _cnt2945>=1 ) { break _loop2945; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt2945++;
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			
				if(  makeToken(WS).getColumn() != 1)
					_ttype = Token.SKIP;
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
