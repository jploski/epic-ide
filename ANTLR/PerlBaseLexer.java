// $ANTLR : "add.g" -> "PerlBaseLexer.java"$

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
				case '\'':
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
		int _cnt359=0;
		_loop359:
		do {
			if ((_tokenSet_1.member(LA(1)))) {
				{
				match(_tokenSet_1);
				}
			}
			else {
				if ( _cnt359>=1 ) { break _loop359; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt359++;
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
		
		boolean synPredMatched366 = false;
		if (((LA(1)=='A') && (LA(2)=='R') && (LA(3)=='R'))) {
			int _m366 = mark();
			synPredMatched366 = true;
			inputState.guessing++;
			try {
				{
				match("ARRAY");
				}
			}
			catch (RecognitionException pe) {
				synPredMatched366 = false;
			}
			rewind(_m366);
			inputState.guessing--;
		}
		if ( synPredMatched366 ) {
			match("ARRAY");
			if ( inputState.guessing==0 ) {
				_ttype = ARRAY_REF;
			}
		}
		else {
			boolean synPredMatched368 = false;
			if (((LA(1)=='S') && (LA(2)=='C') && (LA(3)=='A'))) {
				int _m368 = mark();
				synPredMatched368 = true;
				inputState.guessing++;
				try {
					{
					match("SCALAR");
					}
				}
				catch (RecognitionException pe) {
					synPredMatched368 = false;
				}
				rewind(_m368);
				inputState.guessing--;
			}
			if ( synPredMatched368 ) {
				match("SCALAR");
				if ( inputState.guessing==0 ) {
					_ttype = SCALAR_REF;
				}
			}
			else {
				boolean synPredMatched370 = false;
				if (((LA(1)=='H') && (LA(2)=='A') && (LA(3)=='S'))) {
					int _m370 = mark();
					synPredMatched370 = true;
					inputState.guessing++;
					try {
						{
						match("HASH");
						}
					}
					catch (RecognitionException pe) {
						synPredMatched370 = false;
					}
					rewind(_m370);
					inputState.guessing--;
				}
				if ( synPredMatched370 ) {
					match("HASH");
					if ( inputState.guessing==0 ) {
						_ttype = HASH_REF;
					}
				}
				else {
					boolean synPredMatched372 = false;
					if (((LA(1)=='C') && (LA(2)=='O') && (LA(3)=='D'))) {
						int _m372 = mark();
						synPredMatched372 = true;
						inputState.guessing++;
						try {
							{
							match("CODE");
							}
						}
						catch (RecognitionException pe) {
							synPredMatched372 = false;
						}
						rewind(_m372);
						inputState.guessing--;
					}
					if ( synPredMatched372 ) {
						match("CODE");
						if ( inputState.guessing==0 ) {
							_ttype = CODE_REF;
						}
					}
					else {
						boolean synPredMatched374 = false;
						if (((LA(1)=='R') && (LA(2)=='E') && (LA(3)=='F'))) {
							int _m374 = mark();
							synPredMatched374 = true;
							inputState.guessing++;
							try {
								{
								match("REF");
								}
							}
							catch (RecognitionException pe) {
								synPredMatched374 = false;
							}
							rewind(_m374);
							inputState.guessing--;
						}
						if ( synPredMatched374 ) {
							match("REF");
							if ( inputState.guessing==0 ) {
								_ttype = REF;
							}
						}
						else {
							boolean synPredMatched376 = false;
							if (((LA(1)=='F') && (LA(2)=='i') && (LA(3)=='l'))) {
								int _m376 = mark();
								synPredMatched376 = true;
								inputState.guessing++;
								try {
									{
									match("FileHandle");
									}
								}
								catch (RecognitionException pe) {
									synPredMatched376 = false;
								}
								rewind(_m376);
								inputState.guessing--;
							}
							if ( synPredMatched376 ) {
								match("FileHandle");
								if ( inputState.guessing==0 ) {
									_ttype = FILE_HANDLE;
								}
							}
							else {
								boolean synPredMatched378 = false;
								if (((LA(1)=='f') && (LA(2)=='i') && (LA(3)=='l'))) {
									int _m378 = mark();
									synPredMatched378 = true;
									inputState.guessing++;
									try {
										{
										match("fileno(");
										}
									}
									catch (RecognitionException pe) {
										synPredMatched378 = false;
									}
									rewind(_m378);
									inputState.guessing--;
								}
								if ( synPredMatched378 ) {
									{
									match("fileno(");
									}
									{
									int _cnt381=0;
									_loop381:
									do {
										if (((LA(1) >= '0' && LA(1) <= '9'))) {
											matchRange('0','9');
										}
										else {
											if ( _cnt381>=1 ) { break _loop381; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
										}
										
										_cnt381++;
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
		_loop389:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				mPURE_NAME_CHAR(false);
			}
			else {
				break _loop389;
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
		int _cnt392=0;
		_loop392:
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
				if ( _cnt392>=1 ) { break _loop392; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt392++;
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
		
		match('\'');
		{
		_loop399:
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
				break _loop399;
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
		int _cnt402=0;
		_loop402:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				matchRange('0','9');
			}
			else {
				if ( _cnt402>=1 ) { break _loop402; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt402++;
		} while (true);
		}
		{
		if ((LA(1)=='.')) {
			match('.');
			{
			int _cnt405=0;
			_loop405:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					matchRange('0','9');
				}
				else {
					if ( _cnt405>=1 ) { break _loop405; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt405++;
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
		int _cnt409=0;
		_loop409:
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
				if ( _cnt409>=1 ) { break _loop409; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt409++;
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			
				if(  makeToken(WS).getColumn() != 1)
					_ttype = Token.SKIP;
				    else
				    {
				    	int new_col =   text.length()-_begin;
				    	//System.out.println("§§§Indent: "+new_col+"§§§§§\n");
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
		long[] data = new long[8];
		data[0]=-2593835551551399432L;
		data[1]=-2L;
		for (int i = 2; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[8];
		data[0]=-4294968840L;
		for (int i = 1; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[8];
		data[0]=-2305846312043554312L;
		for (int i = 1; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[8];
		data[0]=-549755813896L;
		data[1]=-268435457L;
		for (int i = 2; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	
	}
