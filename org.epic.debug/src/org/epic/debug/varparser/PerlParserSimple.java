// $ANTLR : "add.g" -> "PerlParserSimple.java"$
 package org.epic.debug.varparser; 
import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

public class PerlParserSimple extends antlr.LLkParser       implements AddTokenTypes
 {

	java.util.Stack mVarStack = new java.util.Stack();
	java.util.ArrayList mVarList = null; //new java.util.ArrayList();
	org.eclipse.debug.core.model.IDebugElement mDebugger;
	int mScope;
	PerlLexer mLex;
	antlr.TokenStreamHiddenTokenFilter mFilter;

	public void setLex(PerlLexer fLex)
	{
		mLex = fLex;
	}
	
	public void setFilter(antlr.TokenStreamHiddenTokenFilter fFilter)
	{
		mFilter = fFilter;
	}
	public void printConsole(String fString)
	{
		//System.out.println(fString);
	}
	
	public void setVarList( java.util.ArrayList fVarList )
	{
		mVarList = fVarList;	
	}
	
	public void setDebugger(org.eclipse.debug.core.model.IDebugElement fDebugger)
	{
		mDebugger = fDebugger;
	}

	public void setScope(int fScope)
	{
		mScope = fScope;
	}
	
	
	public PerlDebugVar[] getVarArray()
	{
		return( (PerlDebugVar[]) mVarList.toArray(new PerlDebugVar[mVarList.size()]));
	}

	public java.util.ArrayList getVars()
	{
		return( mVarList);
	}
	public void addVar(String fName, String fType)
	{
		PerlDebugVar var = new PerlDebugVar(mDebugger,mScope);
		PerlDebugValue val = new PerlDebugValue(mDebugger);

		try{
		val.setType(fType);
		var.setName(fName);
		var.setValue(val);


		if( ! mVarStack.empty() )
		{
			 ((PerlDebugVar)mVarStack.peek()).getPdValue().addVar(var);

		}

		}catch (Exception e){};		mVarStack.push(var);
	}

public void setVal(String fVal, String fType)
{
		try{
		 ((PerlDebugVar)mVarStack.peek()).getPdValue().setValue(fVal);
 		 ((PerlDebugVar)mVarStack.peek()).getPdValue().setType(fType);
 		}catch (Exception e){};

}



public void appendVal(String fVal)
{
		try{
		 ((PerlDebugVar)mVarStack.peek()).getPdValue().appendValue(fVal);
 		 }catch (Exception e){};

}

public void appendVal(Token tok) {
    antlr.CommonHiddenStreamToken t = (antlr.CommonHiddenStreamToken)tok;
    //appendVal(t.getText());
    for ( ; t!=null ; t=mFilter.getHiddenAfter(t) ) {
        appendVal(t.getText());
    }
}
public void appendName(String fVal)
{
		try{
		 ((PerlDebugVar)mVarStack.peek()).appendName(fVal);
 		 }catch (Exception e){};

}


	public void finalizeVar()
	{
		PerlDebugVar var;
		var = (PerlDebugVar)mVarStack.pop();
		try{
		if( var.getPdValue().getValueString() == null )
		{
			var.getPdValue().appendValue("...");
		}
		}catch(Exception e) {}
		if( mVarStack.empty() )
			mVarList.add(var);
			try{
	 printConsole("::::Finelizing Var "+ var.getName());
			} catch(Exception e) {};
	}


protected PerlParserSimple(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public PerlParserSimple(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected PerlParserSimple(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public PerlParserSimple(TokenStream lexer) {
  this(lexer,3);
}

public PerlParserSimple(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

	public final void topLevel() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			_loop3:
			do {
				if ((LA(1)==NL)) {
					match(NL);
				}
				else {
					break _loop3;
				}
				
			} while (true);
			}
			{
			_loop7:
			do {
				if ((_tokenSet_0.member(LA(1)))) {
					namedVar();
					{
					_loop6:
					do {
						if ((LA(1)==NL)) {
							match(NL);
						}
						else {
							break _loop6;
						}
						
					} while (true);
					}
				}
				else {
					break _loop7;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void namedVar() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case SCALAR_NAME:
			{
				namedScalar();
				break;
			}
			case ARRAY_NAME:
			{
				namedArray();
				break;
			}
			case HASH_NAME:
			{
				namedHash();
				break;
			}
			case FILE_HANDLE:
			{
				fileHandle();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void namedScalar() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			name = LT(1);
			match(SCALAR_NAME);
			if ( inputState.guessing==0 ) {
				addVar(name.getText(),"Scalar");printConsole("++++SCALAR:"+name.getText()+"\n");
			}
			match(EQ);
			value();
			if ( inputState.guessing==0 ) {
				finalizeVar();printConsole("----SCALAR:"+name.getText()+"\n");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void namedArray() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			name = LT(1);
			match(ARRAY_NAME);
			if ( inputState.guessing==0 ) {
				addVar(name.getText(),"Array");printConsole("++++ARRAY:"+name.getText()+"\n");
			}
			name_noeq();
			match(EQ);
			val_noponl();
			match(NL);
			{
			switch ( LA(1)) {
			case INDENT_START:
			case PAREN_CL:
			{
				{
				switch ( LA(1)) {
				case INDENT_START:
				{
					arrayEntries();
					break;
				}
				case PAREN_CL:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(PAREN_CL);
				break;
			}
			case EOF:
			case ARRAY_NAME:
			case SCALAR_NAME:
			case HASH_NAME:
			case FILE_HANDLE:
			case NL:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				finalizeVar(); printConsole("---- ARRAY:"+name.getText()+"\n");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void namedHash() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			name = LT(1);
			match(HASH_NAME);
			if ( inputState.guessing==0 ) {
				addVar(name.getText(),"Hash");printConsole("++++HASH:"+name.getText()+"\n");
			}
			name_noeq();
			match(EQ);
			val_noponl();
			match(NL);
			{
			switch ( LA(1)) {
			case INDENT_START:
			{
				{
				hashEntries();
				}
				match(PAREN_CL);
				break;
			}
			case EOF:
			case ARRAY_NAME:
			case SCALAR_NAME:
			case HASH_NAME:
			case FILE_HANDLE:
			case NL:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				finalizeVar();printConsole("----HASH:"+name.getText()+"\n");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void fileHandle() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		Token  val = null;
		
		try {      // for error handling
			match(FILE_HANDLE);
			match(PAREN_OP);
			name = LT(1);
			match(PURE_NAME);
			if ( inputState.guessing==0 ) {
				addVar(name.getText(),"FileHandle");printConsole("++++FH:"+name.getText()+"\n");
			}
			match(PAREN_CL);
			match(KEY_ASSIGN);
			val = LT(1);
			match(FILE_NO);
			if ( inputState.guessing==0 ) {
				setVal(val.getText(),"FileHandle"); finalizeVar();printConsole("++++FNO_SYMB:"+val.getText()+"\n");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void name_noeq() throws RecognitionException, TokenStreamException {
		
		Token  n = null;
		
		try {      // for error handling
			{
			_loop75:
			do {
				if ((_tokenSet_3.member(LA(1)))) {
					n = LT(1);
					matchNot(EQ);
					if ( inputState.guessing==0 ) {
						appendName(n.getText());
					}
				}
				else {
					break _loop75;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_4);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void val_noponl() throws RecognitionException, TokenStreamException {
		
		Token  n = null;
		
		try {      // for error handling
			{
			_loop72:
			do {
				switch ( LA(1)) {
				case ARRAY_NAME:
				case SCALAR_NAME:
				case HASH_NAME:
				case MODULE_NAME:
				case ARRAY_REF:
				case SCALAR_REF:
				case HASH_REF:
				case CODE_REF:
				case GLOB:
				case REF:
				case NUMBER:
				case SEPARATOR:
				case INDENT_START:
				case INDENT_END:
				case FILE_HANDLE:
				case FILE_NO:
				case WS:
				case EQ:
				case PAREN_CL:
				case ADR:
				case STRING:
				case KEY_ASSIGN:
				case REF_SYMB:
				case PURE_NAME:
				case FILE_REF:
				case PREFIXED_NAME:
				case PURE_NAME_CHAR:
				case FIRST_PURE_NAME_CHAR:
				case STRING1:
				case STRING2:
				case CHAR_ESC:
				{
					{
					n = LT(1);
					match(_tokenSet_5);
					}
					if ( inputState.guessing==0 ) {
						appendVal(n);
					}
					break;
				}
				case PAREN_OP:
				{
					match(PAREN_OP);
					break;
				}
				default:
				{
					break _loop72;
				}
				}
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_6);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void arrayEntries() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			boolean synPredMatched18 = false;
			if (((LA(1)==INDENT_START) && ((LA(2) >= ARRAY_NAME && LA(2) <= CHAR_ESC)) && ((LA(3) >= ARRAY_NAME && LA(3) <= CHAR_ESC)))) {
				int _m18 = mark();
				synPredMatched18 = true;
				inputState.guessing++;
				try {
					{
					match(INDENT_START);
					matchNot(NUMBER);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched18 = false;
				}
				rewind(_m18);
				inputState.guessing--;
			}
			if ( synPredMatched18 ) {
				match(INDENT_START);
				val_nonl();
				match(NL);
				match(INDENT_END);
			}
			else if ((LA(1)==INDENT_START) && (LA(2)==NUMBER||LA(2)==INDENT_END) && (_tokenSet_7.member(LA(3)))) {
				match(INDENT_START);
				{
				_loop20:
				do {
					if ((LA(1)==NUMBER)) {
						arrayEntry();
					}
					else {
						break _loop20;
					}
					
				} while (true);
				}
				match(INDENT_END);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_8);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void arrayReference() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(ARRAY_REF);
			name = LT(1);
			match(ADR);
			{
			_loop14:
			do {
				if ((_tokenSet_9.member(LA(1)))) {
					matchNot(NL);
				}
				else {
					break _loop14;
				}
				
			} while (true);
			}
			match(NL);
			if ( inputState.guessing==0 ) {
				addVar("Array->"+name.getText()," ");printConsole("++++ARRAYREF:"+name.getText()+"\n");
			}
			{
			switch ( LA(1)) {
			case INDENT_START:
			{
				arrayEntries();
				break;
			}
			case EOF:
			case ARRAY_NAME:
			case SCALAR_NAME:
			case HASH_NAME:
			case INDENT_END:
			case FILE_HANDLE:
			case NL:
			case STRING:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				finalizeVar();printConsole("----ARRAYREF:"+name.getText()+"\n");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void val_nonl() throws RecognitionException, TokenStreamException {
		
		Token  n = null;
		
		try {      // for error handling
			{
			_loop68:
			do {
				if ((_tokenSet_9.member(LA(1)))) {
					n = LT(1);
					matchNot(NL);
					if ( inputState.guessing==0 ) {
						appendVal(n);
					}
				}
				else {
					break _loop68;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_6);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void arrayEntry() throws RecognitionException, TokenStreamException {
		
		Token  start = null;
		
		try {      // for error handling
			start = LT(1);
			match(NUMBER);
			if ( inputState.guessing==0 ) {
				addVar("["+start.getText()+"]",null);printConsole("INDEX:"+start.getText());
			}
			val_nonl();
			match(NL);
			if ( inputState.guessing==0 ) {
				finalizeVar();
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_11);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void hashEntries() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			boolean synPredMatched31 = false;
			if (((LA(1)==INDENT_START) && (LA(2)==INDENT_END||LA(2)==STRING) && (_tokenSet_7.member(LA(3))))) {
				int _m31 = mark();
				synPredMatched31 = true;
				inputState.guessing++;
				try {
					{
					match(INDENT_START);
					match(STRING);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched31 = false;
				}
				rewind(_m31);
				inputState.guessing--;
			}
			if ( synPredMatched31 ) {
				match(INDENT_START);
				{
				_loop33:
				do {
					if ((LA(1)==STRING)) {
						hashEntry();
					}
					else {
						break _loop33;
					}
					
				} while (true);
				}
				match(INDENT_END);
			}
			else if ((LA(1)==INDENT_START) && ((LA(2) >= ARRAY_NAME && LA(2) <= CHAR_ESC)) && ((LA(3) >= ARRAY_NAME && LA(3) <= CHAR_ESC))) {
				match(INDENT_START);
				val_nonl();
				match(NL);
				match(INDENT_END);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_8);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void hashReference() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(HASH_REF);
			name = LT(1);
			match(ADR);
			{
			_loop27:
			do {
				if ((_tokenSet_9.member(LA(1)))) {
					matchNot(NL);
				}
				else {
					break _loop27;
				}
				
			} while (true);
			}
			match(NL);
			if ( inputState.guessing==0 ) {
				addVar("Hash->"+name.getText()," ");printConsole("++++HASHREF:"+name.getText()+"\n");
			}
			{
			switch ( LA(1)) {
			case INDENT_START:
			{
				hashEntries();
				break;
			}
			case EOF:
			case ARRAY_NAME:
			case SCALAR_NAME:
			case HASH_NAME:
			case INDENT_END:
			case FILE_HANDLE:
			case NL:
			case STRING:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				finalizeVar();printConsole("---HASHREF:"+name.getText()+"\n");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void hashEntry() throws RecognitionException, TokenStreamException {
		
		Token  start = null;
		
		try {      // for error handling
			start = LT(1);
			match(STRING);
			if ( inputState.guessing==0 ) {
				addVar(start.getText(),"");printConsole("H["+start.getColumn()+"]");
			}
			name_noka();
			match(KEY_ASSIGN);
			if ( inputState.guessing==0 ) {
				printConsole("KEY:"+start.getText());
			}
			value();
			if ( inputState.guessing==0 ) {
				finalizeVar();
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_12);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void name_noka() throws RecognitionException, TokenStreamException {
		
		Token  n = null;
		
		try {      // for error handling
			{
			_loop78:
			do {
				if ((_tokenSet_13.member(LA(1)))) {
					n = LT(1);
					matchNot(KEY_ASSIGN);
					if ( inputState.guessing==0 ) {
						appendName(n.getText());
					}
				}
				else {
					break _loop78;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void value() throws RecognitionException, TokenStreamException {
		
		Token  s = null;
		Token  n = null;
		Token  m = null;
		
		try {      // for error handling
			boolean synPredMatched46 = false;
			if (((_tokenSet_15.member(LA(1))) && (LA(2)==NL||LA(2)==ADR) && (_tokenSet_7.member(LA(3))))) {
				int _m46 = mark();
				synPredMatched46 = true;
				inputState.guessing++;
				try {
					{
					switch ( LA(1)) {
					case HASH_REF:
					{
						match(HASH_REF);
						break;
					}
					case SCALAR_REF:
					{
						match(SCALAR_REF);
						break;
					}
					case CODE_REF:
					{
						match(CODE_REF);
						break;
					}
					case REF:
					{
						match(REF);
						break;
					}
					case FILE_REF:
					{
						match(FILE_REF);
						break;
					}
					case GLOB:
					{
						match(GLOB);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
				}
				catch (RecognitionException pe) {
					synPredMatched46 = false;
				}
				rewind(_m46);
				inputState.guessing--;
			}
			if ( synPredMatched46 ) {
				refs();
			}
			else {
				boolean synPredMatched48 = false;
				if (((LA(1)==STRING) && (LA(2)==NL) && (_tokenSet_10.member(LA(3))))) {
					int _m48 = mark();
					synPredMatched48 = true;
					inputState.guessing++;
					try {
						{
						match(STRING);
						match(NL);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched48 = false;
					}
					rewind(_m48);
					inputState.guessing--;
				}
				if ( synPredMatched48 ) {
					s = LT(1);
					match(STRING);
					if ( inputState.guessing==0 ) {
						setVal(s.getText(),"Scalar");printConsole(" VAL:"+s.getText()+"\n");
					}
					match(NL);
				}
				else {
					boolean synPredMatched50 = false;
					if (((LA(1)==NUMBER) && (LA(2)==NL) && (_tokenSet_10.member(LA(3))))) {
						int _m50 = mark();
						synPredMatched50 = true;
						inputState.guessing++;
						try {
							{
							match(NUMBER);
							match(NL);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched50 = false;
						}
						rewind(_m50);
						inputState.guessing--;
					}
					if ( synPredMatched50 ) {
						n = LT(1);
						match(NUMBER);
						if ( inputState.guessing==0 ) {
							setVal(n.getText(),"Scalar");printConsole(" VAL_NUM:"+n.getText()+"\n");
						}
						match(NL);
					}
					else {
						boolean synPredMatched52 = false;
						if (((LA(1)==FILE_HANDLE) && (LA(2)==PAREN_OP) && (LA(3)==PURE_NAME))) {
							int _m52 = mark();
							synPredMatched52 = true;
							inputState.guessing++;
							try {
								{
								match(FILE_HANDLE);
								}
							}
							catch (RecognitionException pe) {
								synPredMatched52 = false;
							}
							rewind(_m52);
							inputState.guessing--;
						}
						if ( synPredMatched52 ) {
							fileHandle();
							match(NL);
						}
						else {
							boolean synPredMatched54 = false;
							if (((LA(1)==NL) && (_tokenSet_10.member(LA(2))) && (_tokenSet_7.member(LA(3))))) {
								int _m54 = mark();
								synPredMatched54 = true;
								inputState.guessing++;
								try {
									{
									match(NL);
									}
								}
								catch (RecognitionException pe) {
									synPredMatched54 = false;
								}
								rewind(_m54);
								inputState.guessing--;
							}
							if ( synPredMatched54 ) {
								match(NL);
								if ( inputState.guessing==0 ) {
									setVal("undef","Scalar");
								}
							}
							else {
								boolean synPredMatched57 = false;
								if (((_tokenSet_16.member(LA(1))) && (_tokenSet_9.member(LA(2))) && ((LA(3) >= ARRAY_NAME && LA(3) <= CHAR_ESC)))) {
									int _m57 = mark();
									synPredMatched57 = true;
									inputState.guessing++;
									try {
										{
										{
										match(_tokenSet_16);
										}
										match(EQ);
										}
									}
									catch (RecognitionException pe) {
										synPredMatched57 = false;
									}
									rewind(_m57);
									inputState.guessing--;
								}
								if ( synPredMatched57 ) {
									{
									int _cnt60=0;
									_loop60:
									do {
										if ((_tokenSet_16.member(LA(1)))) {
											{
											m = LT(1);
											match(_tokenSet_16);
											}
											if ( inputState.guessing==0 ) {
												appendVal(m.getText());
											}
										}
										else {
											if ( _cnt60>=1 ) { break _loop60; } else {throw new NoViableAltException(LT(1), getFilename());}
										}
										
										_cnt60++;
									} while (true);
									}
									match(EQ);
									{
									boolean synPredMatched63 = false;
									if (((_tokenSet_15.member(LA(1))) && (LA(2)==NL||LA(2)==ADR) && (_tokenSet_7.member(LA(3))))) {
										int _m63 = mark();
										synPredMatched63 = true;
										inputState.guessing++;
										try {
											{
											switch ( LA(1)) {
											case HASH_REF:
											{
												match(HASH_REF);
												break;
											}
											case SCALAR_REF:
											{
												match(SCALAR_REF);
												break;
											}
											case CODE_REF:
											{
												match(CODE_REF);
												break;
											}
											case REF:
											{
												match(REF);
												break;
											}
											case FILE_REF:
											{
												match(FILE_REF);
												break;
											}
											case GLOB:
											{
												match(GLOB);
												break;
											}
											default:
											{
												throw new NoViableAltException(LT(1), getFilename());
											}
											}
											}
										}
										catch (RecognitionException pe) {
											synPredMatched63 = false;
										}
										rewind(_m63);
										inputState.guessing--;
									}
									if ( synPredMatched63 ) {
										refs();
									}
									else if (((LA(1) >= ARRAY_NAME && LA(1) <= CHAR_ESC)) && (_tokenSet_7.member(LA(2))) && (_tokenSet_7.member(LA(3)))) {
										val_nonl();
										match(NL);
									}
									else {
										throw new NoViableAltException(LT(1), getFilename());
									}
									
									}
								}
								else if (((LA(1) >= ARRAY_NAME && LA(1) <= CHAR_ESC)) && (_tokenSet_7.member(LA(2))) && (_tokenSet_7.member(LA(3)))) {
									val_nonl();
									match(NL);
								}
								else {
									throw new NoViableAltException(LT(1), getFilename());
								}
								}}}}}
							}
							catch (RecognitionException ex) {
								if (inputState.guessing==0) {
									reportError(ex);
									consume();
									consumeUntil(_tokenSet_10);
								} else {
								  throw ex;
								}
							}
						}
						
	public final void scalarRef() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(SCALAR_REF);
			name = LT(1);
			match(ADR);
			if ( inputState.guessing==0 ) {
				addVar("Scalar->"+name.getText()," ");printConsole("++++SCALARREF:"+name.getText()+"\n");
			}
			match(NL);
			match(INDENT_START);
			match(REF_SYMB);
			value();
			if ( inputState.guessing==0 ) {
				finalizeVar();printConsole("----SCALARREF:"+name.getText()+"\n");
			}
			match(INDENT_END);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void codeRef() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(CODE_REF);
			name = LT(1);
			match(ADR);
			if ( inputState.guessing==0 ) {
				addVar("Code->"+name.getText()," ");setVal(name.getText(),"CodeRef");printConsole("++++CODEREF:"+name.getText()+"\n");
			}
			match(NL);
			match(INDENT_START);
			match(REF_SYMB);
			val_nonl();
			if ( inputState.guessing==0 ) {
				finalizeVar();printConsole("----CodeREF:"+name.getText()+"\n");
			}
			match(NL);
			match(INDENT_END);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void ref() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(REF);
			name = LT(1);
			match(ADR);
			if ( inputState.guessing==0 ) {
				printConsole("++++REF_SYMB:"+name.getText()+"\n");
			}
			match(NL);
			match(INDENT_START);
			match(REF_SYMB);
			value();
			if ( inputState.guessing==0 ) {
				printConsole("----REF_SYMB:"+name.getText()+"\n");
			}
			match(INDENT_END);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void fileHandleRef() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			name = LT(1);
			match(FILE_REF);
			if ( inputState.guessing==0 ) {
				addVar("->"+name.getText(),"FileHandleRef");printConsole("++++FRef:"+name.getText()+"\n");
			}
			match(NL);
			{
			switch ( LA(1)) {
			case INDENT_START:
			{
				match(INDENT_START);
				fileHandle();
				match(NL);
				match(INDENT_END);
				break;
			}
			case EOF:
			case ARRAY_NAME:
			case SCALAR_NAME:
			case HASH_NAME:
			case INDENT_END:
			case FILE_HANDLE:
			case NL:
			case STRING:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				finalizeVar();
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void globRef() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(GLOB);
			name = LT(1);
			match(ADR);
			if ( inputState.guessing==0 ) {
				addVar("GLOB->"+name.getText()," ");printConsole("++++Glob_SYMB:"+name.getText()+"\n");
			}
			name_nonl();
			match(NL);
			match(INDENT_START);
			match(REF_SYMB);
			name_nonl();
			match(NL);
			{
			switch ( LA(1)) {
			case INDENT_START:
			{
				match(INDENT_START);
				val_nonl();
				match(NL);
				match(INDENT_END);
				break;
			}
			case INDENT_END:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(INDENT_END);
			if ( inputState.guessing==0 ) {
				finalizeVar();printConsole("----Glob_SYMB:"+name.getText()+"\n");
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void name_nonl() throws RecognitionException, TokenStreamException {
		
		Token  n = null;
		
		try {      // for error handling
			{
			_loop81:
			do {
				if ((_tokenSet_9.member(LA(1)))) {
					n = LT(1);
					matchNot(NL);
					if ( inputState.guessing==0 ) {
						appendName(n.getText());
					}
				}
				else {
					break _loop81;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_6);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void refs() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case HASH_REF:
			{
				hashReference();
				break;
			}
			case ARRAY_REF:
			{
				arrayReference();
				break;
			}
			case SCALAR_REF:
			{
				scalarRef();
				break;
			}
			case CODE_REF:
			{
				codeRef();
				break;
			}
			case REF:
			{
				ref();
				break;
			}
			case FILE_REF:
			{
				fileHandleRef();
				break;
			}
			case GLOB:
			{
				globRef();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"ARRAY_NAME",
		"SCALAR_NAME",
		"HASH_NAME",
		"MODULE_NAME",
		"ARRAY_REF",
		"SCALAR_REF",
		"HASH_REF",
		"CODE_REF",
		"GLOB",
		"REF",
		"NUMBER",
		"SEPARATOR",
		"INDENT_START",
		"INDENT_END",
		"FILE_HANDLE",
		"FILE_NO",
		"WS",
		"NL",
		"EQ",
		"PAREN_CL",
		"ADR",
		"STRING",
		"KEY_ASSIGN",
		"REF_SYMB",
		"PAREN_OP",
		"PURE_NAME",
		"FILE_REF",
		"PREFIXED_NAME",
		"PURE_NAME_CHAR",
		"FIRST_PURE_NAME_CHAR",
		"STRING1",
		"STRING2",
		"CHAR_ESC"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 262256L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 2359410L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 137434759152L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 4194304L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 137168420848L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 2097152L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 137438953458L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 44433522L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 137436856304L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 36044914L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { 147456L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 33685504L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { 137371844592L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { 67108864L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 1073757952L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 137432662000L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	
	}
