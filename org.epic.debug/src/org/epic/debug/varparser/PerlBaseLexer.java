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
		int _cnt157=0;
		_loop157:
		do {
			if ((_tokenSet_1.member(LA(1)))) {
				{
				match(_tokenSet_1);
				}
			}
			else {
				if ( _cnt157>=1 ) { break _loop157; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt157++;
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
		
		boolean synPredMatched164 = false;
		if (((LA(1)=='A') && (LA(2)=='R') && (LA(3)=='R'))) {
			int _m164 = mark();
			synPredMatched164 = true;
			inputState.guessing++;
			try {
				{
				match("ARRAY");
				}
			}
			catch (RecognitionException pe) {
				synPredMatched164 = false;
			}
			rewind(_m164);
			inputState.guessing--;
		}
		if ( synPredMatched164 ) {
			match("ARRAY");
			if ( inputState.guessing==0 ) {
				_ttype = ARRAY_REF;
			}
		}
		else {
			boolean synPredMatched166 = false;
			if (((LA(1)=='S') && (LA(2)=='C') && (LA(3)=='A'))) {
				int _m166 = mark();
				synPredMatched166 = true;
				inputState.guessing++;
				try {
					{
					match("SCALAR");
					}
				}
				catch (RecognitionException pe) {
					synPredMatched166 = false;
				}
				rewind(_m166);
				inputState.guessing--;
			}
			if ( synPredMatched166 ) {
				match("SCALAR");
				if ( inputState.guessing==0 ) {
					_ttype = SCALAR_REF;
				}
			}
			else {
				boolean synPredMatched168 = false;
				if (((LA(1)=='H') && (LA(2)=='A') && (LA(3)=='S'))) {
					int _m168 = mark();
					synPredMatched168 = true;
					inputState.guessing++;
					try {
						{
						match("HASH");
						}
					}
					catch (RecognitionException pe) {
						synPredMatched168 = false;
					}
					rewind(_m168);
					inputState.guessing--;
				}
				if ( synPredMatched168 ) {
					match("HASH");
					if ( inputState.guessing==0 ) {
						_ttype = HASH_REF;
					}
				}
				else {
					boolean synPredMatched170 = false;
					if (((LA(1)=='C') && (LA(2)=='O') && (LA(3)=='D'))) {
						int _m170 = mark();
						synPredMatched170 = true;
						inputState.guessing++;
						try {
							{
							match("CODE");
							}
						}
						catch (RecognitionException pe) {
							synPredMatched170 = false;
						}
						rewind(_m170);
						inputState.guessing--;
					}
					if ( synPredMatched170 ) {
						match("CODE");
						if ( inputState.guessing==0 ) {
							_ttype = CODE_REF;
						}
					}
					else {
						boolean synPredMatched172 = false;
						if (((LA(1)=='R') && (LA(2)=='E') && (LA(3)=='F'))) {
							int _m172 = mark();
							synPredMatched172 = true;
							inputState.guessing++;
							try {
								{
								match("REF");
								}
							}
							catch (RecognitionException pe) {
								synPredMatched172 = false;
							}
							rewind(_m172);
							inputState.guessing--;
						}
						if ( synPredMatched172 ) {
							match("REF");
							if ( inputState.guessing==0 ) {
								_ttype = REF;
							}
						}
						else {
							boolean synPredMatched174 = false;
							if (((LA(1)=='F') && (LA(2)=='i') && (LA(3)=='l'))) {
								int _m174 = mark();
								synPredMatched174 = true;
								inputState.guessing++;
								try {
									{
									match("FileHandle");
									}
								}
								catch (RecognitionException pe) {
									synPredMatched174 = false;
								}
								rewind(_m174);
								inputState.guessing--;
							}
							if ( synPredMatched174 ) {
								match("FileHandle");
								if ( inputState.guessing==0 ) {
									_ttype = FILE_HANDLE;
								}
							}
							else {
								boolean synPredMatched176 = false;
								if (((LA(1)=='f') && (LA(2)=='i') && (LA(3)=='l'))) {
									int _m176 = mark();
									synPredMatched176 = true;
									inputState.guessing++;
									try {
										{
										match("fileno(");
										}
									}
									catch (RecognitionException pe) {
										synPredMatched176 = false;
									}
									rewind(_m176);
									inputState.guessing--;
								}
								if ( synPredMatched176 ) {
									{
									match("fileno(");
									}
									{
									int _cnt179=0;
									_loop179:
									do {
										if (((LA(1) >= '0' && LA(1) <= '9'))) {
											matchRange('0','9');
										}
										else {
											if ( _cnt179>=1 ) { break _loop179; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
										}
										
										_cnt179++;
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
		_loop187:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				mPURE_NAME_CHAR(false);
			}
			else {
				break _loop187;
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
		int _cnt190=0;
		_loop190:
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
				if ( _cnt190>=1 ) { break _loop190; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt190++;
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
		_loop198:
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
				break _loop198;
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
		_loop203:
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
				break _loop203;
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
		int _cnt206=0;
		_loop206:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				matchRange('0','9');
			}
			else {
				if ( _cnt206>=1 ) { break _loop206; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt206++;
		} while (true);
		}
		{
		if ((LA(1)=='.')) {
			match('.');
			{
			int _cnt209=0;
			_loop209:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					matchRange('0','9');
				}
				else {
					if ( _cnt209>=1 ) { break _loop209; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt209++;
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
		int _cnt213=0;
		_loop213:
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
				if ( _cnt213>=1 ) { break _loop213; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt213++;
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
