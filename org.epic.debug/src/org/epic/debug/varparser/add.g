header { package org.epic.debug.varparser; }
class PerlParserSimple extends Parser;




options
{
    exportVocab = Add;
    k = 3;
}



tokens
{
	  ARRAY_NAME;
	  SCALAR_NAME;
	  HASH_NAME;
	  MODULE_NAME;
	  ARRAY_REF;
	  SCALAR_REF;
	  HASH_REF;
	  CODE_REF;
	  GLOB;
	  REF;
	  NUMBER;
	  SEPARATOR;
	  INDENT_START;
	  INDENT_END;
	  FILE_HANDLE;
	  FILE_NO;
	  WS;
}

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

}

//topLevel: (NL)*( namedVar (NL)* )* {printConsole("1\n");}{printConsole("2\n");} INDENT_START {printConsole("3\n");}PURE_NAME {printConsole("4\n");}NL {printConsole("5\n");}INDENT_END {printConsole("6\n");}EOF;
topLevel: (NL)*( namedVar (NL)* )* EOF;
namedVar: namedScalar | namedArray | namedHash | fileHandle;

namedArray: name:ARRAY_NAME{addVar(name.getText(),"Array");printConsole("++++ARRAY:"+name.getText()+"\n");} name_noeq EQ val_noponl NL( (arrayEntries)?   PAREN_CL)? {finalizeVar(); printConsole("---- ARRAY:"+name.getText()+"\n");};

arrayReference: ARRAY_REF name:ADR (~NL)* NL {addVar("Array->"+name.getText()," ");printConsole("++++ARRAYREF:"+name.getText()+"\n");}(arrayEntries)? {finalizeVar();printConsole("----ARRAYREF:"+name.getText()+"\n");};
arrayEntries: (INDENT_START ~NUMBER)=> INDENT_START  val_nonl NL INDENT_END
			|  INDENT_START (arrayEntry)* INDENT_END;
arrayEntry: start:NUMBER {addVar("["+start.getText()+"]",null);printConsole("INDEX:"+start.getText());} val_nonl NL{finalizeVar();};



namedHash: name:HASH_NAME{addVar(name.getText(),"Hash");printConsole("++++HASH:"+name.getText()+"\n");} name_noeq EQ val_noponl NL( (hashEntries) PAREN_CL)?  {finalizeVar();printConsole("----HASH:"+name.getText()+"\n");};
hashReference: HASH_REF name:ADR (~NL)* NL {addVar("Hash->"+name.getText()," ");printConsole("++++HASHREF:"+name.getText()+"\n");}(hashEntries)?{finalizeVar();printConsole("---HASHREF:"+name.getText()+"\n");};
hashEntries:  	(INDENT_START STRING)=> INDENT_START (hashEntry)* INDENT_END
				|  INDENT_START  val_nonl NL INDENT_END;
						
hashEntry:  start:STRING{addVar(start.getText(),"");printConsole("H["+start.getColumn()+"]");}name_noka KEY_ASSIGN{printConsole("KEY:"+start.getText());}value {finalizeVar();};

namedScalar: name:SCALAR_NAME{addVar(name.getText(),"Scalar");printConsole("++++SCALAR:"+name.getText()+"\n");} EQ value {finalizeVar();printConsole("----SCALAR:"+name.getText()+"\n");};
scalarRef: SCALAR_REF name:ADR{addVar("Scalar->"+name.getText()," ");printConsole("++++SCALARREF:"+name.getText()+"\n");} NL INDENT_START REF_SYMB value {finalizeVar();printConsole("----SCALARREF:"+name.getText()+"\n");}INDENT_END;

codeRef: CODE_REF name:ADR{addVar("Code->"+name.getText()," ");setVal(name.getText(),"CodeRef");printConsole("++++CODEREF:"+name.getText()+"\n");} NL INDENT_START REF_SYMB val_nonl {finalizeVar();printConsole("----CodeREF:"+name.getText()+"\n");}NL INDENT_END;

ref: REF name:ADR{printConsole("++++REF_SYMB:"+name.getText()+"\n");} NL INDENT_START REF_SYMB value {printConsole("----REF_SYMB:"+name.getText()+"\n");}INDENT_END;

fileHandle: FILE_HANDLE PAREN_OP name:PURE_NAME{addVar(name.getText(),"FileHandle");printConsole("++++FH:"+name.getText()+"\n");} PAREN_CL KEY_ASSIGN val:FILE_NO{setVal(val.getText(),"FileHandle"); finalizeVar();printConsole("++++FNO_SYMB:"+val.getText()+"\n");};
fileHandleRef: name:FILE_REF{addVar("->"+name.getText(),"FileHandleRef");printConsole("++++FRef:"+name.getText()+"\n");}  NL (INDENT_START fileHandle NL INDENT_END)? {finalizeVar();};
globRef: GLOB name:ADR{addVar("GLOB->"+name.getText()," ");printConsole("++++Glob_SYMB:"+name.getText()+"\n");} name_nonl NL INDENT_START REF_SYMB name_nonl NL (INDENT_START val_nonl NL INDENT_END)? INDENT_END{finalizeVar();printConsole("----Glob_SYMB:"+name.getText()+"\n");};

//value: value_ref | value_simple | value_none |  val_nonl NL;
value: 		(HASH_REF | SCALAR_REF | CODE_REF | REF | FILE_REF | GLOB )=> refs 
		|	(STRING NL)=>s:STRING{setVal(s.getText(),"Scalar");printConsole(" VAL:"+s.getText()+"\n");} NL
		|	(NUMBER NL)=>n:NUMBER{setVal(n.getText(),"Scalar");printConsole(" VAL_NUM:"+n.getText()+"\n");}NL 
		| 	(FILE_HANDLE)=>fileHandle NL
		|	(NL)=> NL{setVal("undef","Scalar");}
		|	(~(NL|EQ) EQ)=> (m:~(NL|EQ){appendVal(m.getText());})+
			EQ  
			((HASH_REF | SCALAR_REF | CODE_REF | REF | FILE_REF | GLOB )=> refs|val_nonl NL)
		|	val_nonl NL;
		
	
//	|	((~(NL|EQ|HASH_REF | SCALAR_REF | CODE_REF | REF | FILE_REF | GLOB))+ EQ)=> EQ pn2:PURE_NAME EQ {appendName("="+pn2.getText());} refs
 //value_simple:(STRING NL)=>STRING{setVal(s.getText(),"Scalar");printConsole(" VAL:"+s.getText()+"\n");} NL;
// | n:NUMBER{setVal(n.getText(),"Scalar");printConsole(" VAL_NUM:"+n.getText()+"\n");}NL 
//value_none: (NL)=> NL{setVal("undef","Scalar");};
refs:  (hashReference | arrayReference | scalarRef | codeRef | ref | fileHandleRef | globRef);
//val_nonl: {mLex.mIgnoreWS=false;}(n:~NL{appendVal(n.getText());})*{mLex.mIgnoreWS=true;};
val_nonl: (n:~NL{appendVal(n);})*;
val_noponl:(n:~(NL|PAREN_OP){appendVal(n);}| PAREN_OP)*;


name_noeq: (n:~EQ{appendName(n.getText());})*;
name_noka: (n:~KEY_ASSIGN{appendName(n.getText());})*;
name_nonl: (n:~NL{appendName(n.getText());})*;
//refs: (PURE_NAME EQ)? (hashReference | arrayReference | scalarRef | codeRef | ref | fileHandleRef);
// ----------------------------------------------------------------------------
// the lexer

class PerlBaseLexer extends Lexer;



options
{
    exportVocab = Add;
    testLiterals = false;
    k = 4;
    charVocabulary = '\u0000'..'\uFFFE' ;
}

{
	int mCurrentIndent = 0;
	int mCurrentIndentLevels=0;
	int mIndentEndToSend =0;
	java.util.Stack mIndentStack = new java.util.Stack();
	
}

//protected PREFIXED_NAME:	(~('\n' | ' ' | '\t' |'='))+;
protected PREFIXED_NAME:	(~('\n' | ' ' | '\t' ))+;
HASH_NAME:	"%" PREFIXED_NAME;
ARRAY_NAME: "@" PREFIXED_NAME;
SCALAR_NAME: "$" PREFIXED_NAME;
FILE_REF: "*" PREFIXED_NAME;

MODULE_NAME: //	(PURE_NAME "::") => PURE_NAME "::" PURE_NAME
			("ARRAY") => "ARRAY" {$setType(ARRAY_REF);}
			|	("SCALAR") => "SCALAR"{$setType(SCALAR_REF);}
			|	("HASH") => "HASH"	{$setType(HASH_REF);}
			|	("CODE") => "CODE"	{$setType(CODE_REF);}
			|	("REF") => "REF"	{$setType(REF);}
			|	("GLOB") => "GLOB"	{$setType(GLOB);}
			|	("FileHandle") => "FileHandle"	{$setType(FILE_HANDLE);}
			|   ("fileno(") => ("fileno(") ('0'..'9')+ ')' {$setType(FILE_NO);}
			|	PURE_NAME {$setType(PURE_NAME);}
			;
//protected PURE_NAME_CHAR: 		~('\n'| '\r' | ' ' | '\t' |'=' | '\'' | '$' | '@' | '%' | '(' | ')' | '-');//FIRST_PURE_NAME_CHAR | '0'..'9' | '?' | '*';
protected PURE_NAME_CHAR: 		~('\n'| '\r' | ' ' | '\t' |'='| ')' | '(' );
//FIRST_PURE_NAME_CHAR | '0'..'9' | '?' | '*';
protected FIRST_PURE_NAME_CHAR: ~('\n'| '\r' | ' ' | '\t' |'=' | '\''| '\"' | '$' | '@' | '%' | '(' | ')' | '-' | '*' | '0'..'9');//FIRST_PURE_NAME_CHAR | '0'..'9' | '?' | '*';
protected PURE_NAME:   			(FIRST_PURE_NAME_CHAR) (PURE_NAME_CHAR)*;
ADR: 					"(0x" ('0'..'9' | 'a'..'f')+')';
REF_SYMB:					"->";
KEY_ASSIGN:				"=>";
STRING:					STRING1 | STRING2;
protected STRING1:					'\''( (~('\'' | '\\')) | CHAR_ESC )* '\'';
protected STRING2:					'\"'( (~('\"' | '\\')) | CHAR_ESC )* '\"';
NUMBER:					('0'..'9')+('.'('0'..'9')+)?;
EQ: '=';
WS:
    ( ' '
    | '\t'
    )+
    {
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
	    		$setType(Token.SKIP);
    		if( new_col > mCurrentIndent)
	    		{
	    			$setType(INDENT_START);
	    			mCurrentIndentLevels++;
	    			mIndentStack.push( new Integer(new_col) );
	    			mCurrentIndent =new_col;
	    		}
	    		else
	    		 $setType(Token.SKIP);

    		/*if( new_col < mCurrentIndent)
	    		{
	    			$setType(INDENT_END);
	    			mCurrentIndentLevels--;
	    			mIndentStack.pop();
	    		}*/
	    	//mCurrentIndent =new_col;
	    }

    };
NL:
 ( "\r\n" | '\r' | '\n' )   { newline (); };
PAREN_OP: '(';
PAREN_CL: ')';

protected CHAR_ESC:
    '\\'
    ( 'n'
    | 'r'
    | 't'
    | 'b'
    | "c"('\u0000'..'\ufffe')
    | 'f'
    | '\"'
    | '\''
    | '\\'
    )
;

//ADD:       '+';
//STRINGIFY: '$';
//NUMBERIFY: '#';
//DELIM:     ';';
//OPAREN:    '(';
//CPAREN:    ')';
//
//// whitespace
//WS:
//    ( ' '
//    | '\t'
//    | ( "\r\n" | '\r' | '\n' )
//      { newline (); }
//    )
//    { $setType(Token.SKIP); }
//;
//
//// a numeric literal
//NUMBER_LITERAL:
//    ('-')? (DIGIT)+
//;
//
//// decimal digit
//protected
//DIGIT:
//    '0'..'9'
//;
//
//// string literals
//STRING_LITERAL:
//    '\"'!
//    (CHAR_ESC | ~('\"'|'\\') )*
//    '\"'!
//;
//
//// escape sequence inside a string literal
//protected
//CHAR_ESC:
//    '\\'
//    ( 'n'  { $setText("\n"); }
//    | 'r'  { $setText("\r"); }
//    | 't'  { $setText("\t"); }
//    | 'b'  { $setText("\b"); }
//    | 'f'  { $setText("\f"); }
//    | '\"' { $setText("\""); }
//    | '\'' { $setText("\'"); }
//    | '\\' { $setText("\\"); }
//    )
//;
