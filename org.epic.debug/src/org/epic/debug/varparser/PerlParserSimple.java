// $ANTLR : "add.g" -> "PerlParserSimple.java"$
 package org.epic.debug.varparser; 
import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

public class PerlParserSimple extends antlr.LLkParser       implements AddTokenTypes
 {

	java.util.Stack mVarStack = new java.util.Stack();
	java.util.ArrayList mVarList = null; //new java.util.ArrayList();
	org.eclipse.debug.core.model.IDebugElement mDebugger;
	int mScope;
	
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
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
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
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_2);
		}
	}
	
	public final void namedScalar() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		Token  pn2 = null;
		
		try {      // for error handling
			name = LT(1);
			match(SCALAR_NAME);
			addVar(name.getText(),"Scalar");printConsole("++++SCALAR:"+name.getText()+"\n");
			{
			if ((LA(1)==EQ) && (_tokenSet_3.member(LA(2))) && (_tokenSet_4.member(LA(3)))) {
				match(EQ);
				value();
				finalizeVar();printConsole("----SCALAR:"+name.getText()+"\n");
			}
			else if ((LA(1)==EQ) && (LA(2)==PURE_NAME) && (LA(3)==EQ)) {
				match(EQ);
				pn2 = LT(1);
				match(PURE_NAME);
				match(EQ);
				appendName("="+pn2.getText());
				refs();
				finalizeVar();printConsole("----SCALAR:"+name.getText()+"\n");
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_2);
		}
	}
	
	public final void namedArray() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			name = LT(1);
			match(ARRAY_NAME);
			addVar(name.getText(),"Array");printConsole("++++ARRAY:"+name.getText()+"\n");
			match(EQ);
			match(PAREN_OP);
			match(NL);
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
			finalizeVar(); printConsole("---- ARRAY:"+name.getText()+"\n");
			match(PAREN_CL);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_2);
		}
	}
	
	public final void namedHash() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			name = LT(1);
			match(HASH_NAME);
			addVar(name.getText(),"Hash");printConsole("++++HASH:"+name.getText()+"\n");
			match(EQ);
			match(PAREN_OP);
			match(NL);
			{
			switch ( LA(1)) {
			case INDENT_START:
			{
				hashEntries();
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
			finalizeVar();printConsole("----HASH:"+name.getText()+"\n");
			match(PAREN_CL);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_2);
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
			addVar(name.getText(),"FileHandle");printConsole("++++FH:"+name.getText()+"\n");
			match(PAREN_CL);
			match(KEY_ASSIGN);
			val = LT(1);
			match(FILE_NO);
			setVal(val.getText(),"FileHandle"); finalizeVar();printConsole("++++FNO_SYMB:"+val.getText()+"\n");
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_2);
		}
	}
	
	public final void arrayEntries() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			if ((LA(1)==INDENT_START) && (LA(2)==NUMBER||LA(2)==INDENT_END)) {
				match(INDENT_START);
				{
				_loop15:
				do {
					if ((LA(1)==NUMBER)) {
						arrayEntry();
					}
					else {
						break _loop15;
					}
					
				} while (true);
				}
				match(INDENT_END);
			}
			else if ((LA(1)==INDENT_START) && (LA(2)==REF_SYMB)) {
				match(INDENT_START);
				match(REF_SYMB);
				match(PURE_NAME);
				match(NL);
				match(INDENT_END);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
	}
	
	public final void arrayReference() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(ARRAY_REF);
			name = LT(1);
			match(ADR);
			match(NL);
			addVar("Array->"+name.getText()," ");printConsole("++++ARRAYREF:"+name.getText()+"\n");
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
			case NUMBER:
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
			finalizeVar();printConsole("----ARRAYREF:"+name.getText()+"\n");
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
	}
	
	public final void arrayEntry() throws RecognitionException, TokenStreamException {
		
		Token  start = null;
		
		try {      // for error handling
			start = LT(1);
			match(NUMBER);
			addVar("["+start.getText()+"]",null);printConsole("INDEX:"+start.getText());
			value();
			finalizeVar();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
	}
	
	public final void value() throws RecognitionException, TokenStreamException {
		
		Token  p = null;
		Token  p2 = null;
		Token  s = null;
		Token  n = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case PURE_NAME:
			{
				p = LT(1);
				match(PURE_NAME);
				setVal(p.getText(),"");System.out.print(" VAL:"+p.getText());
				{
				switch ( LA(1)) {
				case PURE_NAME:
				{
					{
					p2 = LT(1);
					match(PURE_NAME);
					}
					appendVal(" "+p2.getText());printConsole(" "+p2.getText());
					break;
				}
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
				printConsole("\n");
				match(NL);
				break;
			}
			case ARRAY_REF:
			case SCALAR_REF:
			case HASH_REF:
			case CODE_REF:
			case REF:
			case FILE_REF:
			{
				refs();
				break;
			}
			case STRING:
			{
				s = LT(1);
				match(STRING);
				setVal(s.getText(),"Scalar");printConsole(" VAL:"+s.getText()+"\n");
				match(NL);
				break;
			}
			case NUMBER:
			{
				n = LT(1);
				match(NUMBER);
				setVal(n.getText(),"Scalar");printConsole(" VAL_NUM:"+n.getText()+"\n");
				match(NL);
				break;
			}
			case NL:
			{
				match(NL);
				setVal("undef","Scalar");
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
	}
	
	public final void hashEntries() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			if ((LA(1)==INDENT_START) && (LA(2)==INDENT_END||LA(2)==STRING)) {
				match(INDENT_START);
				{
				_loop23:
				do {
					if ((LA(1)==STRING)) {
						hashEntry();
					}
					else {
						break _loop23;
					}
					
				} while (true);
				}
				match(INDENT_END);
			}
			else if ((LA(1)==INDENT_START) && (LA(2)==REF_SYMB)) {
				match(INDENT_START);
				match(REF_SYMB);
				match(PURE_NAME);
				match(NL);
				match(INDENT_END);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
	}
	
	public final void hashReference() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(HASH_REF);
			name = LT(1);
			match(ADR);
			match(NL);
			addVar("Hash->"+name.getText()," ");printConsole("++++HASHREF:"+name.getText()+"\n");
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
			case NUMBER:
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
			finalizeVar();printConsole("---HASHREF:"+name.getText()+"\n");
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
	}
	
	public final void hashEntry() throws RecognitionException, TokenStreamException {
		
		Token  start = null;
		
		try {      // for error handling
			start = LT(1);
			match(STRING);
			printConsole("H["+start.getColumn()+"]");
			match(KEY_ASSIGN);
			addVar("\'"+start.getText()+"\'","");printConsole("KEY:"+start.getText());
			value();
			finalizeVar();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_8);
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
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
	}
	
	public final void scalarRef() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(SCALAR_REF);
			name = LT(1);
			match(ADR);
			addVar("Scalar->"+name.getText()," ");printConsole("++++SCALARREF:"+name.getText()+"\n");
			match(NL);
			match(INDENT_START);
			match(REF_SYMB);
			value();
			finalizeVar();printConsole("----SCALARREF:"+name.getText()+"\n");
			match(INDENT_END);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
	}
	
	public final void codeRef() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		Token  a = null;
		Token  b = null;
		Token  c = null;
		Token  d = null;
		
		try {      // for error handling
			match(CODE_REF);
			name = LT(1);
			match(ADR);
			addVar("Code->"+name.getText()," ");setVal(name.getText(),"CodeRef");printConsole("++++CODEREF:"+name.getText()+"\n");
			match(NL);
			match(INDENT_START);
			match(REF_SYMB);
			{
			switch ( LA(1)) {
			case MODULE_NAME:
			{
				a = LT(1);
				match(MODULE_NAME);
				appendVal(a.getText());
				break;
			}
			case NUMBER:
			case NL:
			case ADR:
			case PURE_NAME:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			_loop34:
			do {
				switch ( LA(1)) {
				case PURE_NAME:
				{
					{
					b = LT(1);
					match(PURE_NAME);
					appendVal(b.getText());
					}
					break;
				}
				case NUMBER:
				{
					{
					c = LT(1);
					match(NUMBER);
					appendVal(c.getText());
					}
					break;
				}
				case ADR:
				{
					{
					d = LT(1);
					match(ADR);
					appendVal(d.getText());
					}
					break;
				}
				default:
				{
					break _loop34;
				}
				}
			} while (true);
			}
			match(NL);
			finalizeVar();printConsole("----CodeREF:"+name.getText()+"\n");
			match(INDENT_END);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
	}
	
	public final void ref() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			match(REF);
			name = LT(1);
			match(ADR);
			printConsole("++++REF_SYMB:"+name.getText()+"\n");
			match(NL);
			match(INDENT_START);
			match(REF_SYMB);
			value();
			printConsole("----REF_SYMB:"+name.getText()+"\n");
			match(INDENT_END);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
	}
	
	public final void fileHandleRef() throws RecognitionException, TokenStreamException {
		
		Token  name = null;
		
		try {      // for error handling
			name = LT(1);
			match(FILE_REF);
			addVar("->"+name.getText(),"FileHandleRef");printConsole("++++FRef:"+name.getText()+"\n");
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
			case NUMBER:
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
			finalizeVar();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
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
		"REF",
		"NUMBER",
		"SEPARATOR",
		"INDENT_START",
		"INDENT_END",
		"FILE_HANDLE",
		"FILE_NO",
		"NL",
		"EQ",
		"PAREN_OP",
		"PAREN_CL",
		"ADR",
		"REF_SYMB",
		"PURE_NAME",
		"STRING",
		"KEY_ASSIGN",
		"FILE_REF",
		"PREFIXED_NAME",
		"PURE_NAME_CHAR",
		"FIRST_PURE_NAME_CHAR",
		"STRING1",
		"STRING2",
		"WS",
		"CHAR_ESC"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 131184L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 655474L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 369639168L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 42598514L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 72032370L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 67838066L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 73728L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 67174400L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	
	}
