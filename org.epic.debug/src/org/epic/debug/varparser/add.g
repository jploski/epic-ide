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
	  REF;
	  NUMBER;
	  SEPARATOR;
	  INDENT_START;
	  INDENT_END;
	  FILE_HANDLE;
	  FILE_NO;
}

{
	java.util.Stack mVarStack = new java.util.Stack();
	java.util.ArrayList mVarList = new java.util.ArrayList();
	org.eclipse.debug.core.model.IDebugElement mDebugger;

	public void setDebugger(org.eclipse.debug.core.model.IDebugElement fDebugger)
	{
		mDebugger = fDebugger;
	}

	public PerlDebugVar[] getVarArray()
	{
		return( (PerlDebugVar[]) mVarList.toArray(new PerlDebugVar[mVarList.size()]));
	}


	public void addVar(String fName, String fType)
	{
		PerlDebugVar var = new PerlDebugVar(mDebugger);
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
	 System.out.println("::::Finelizing Var "+ var.getName());
			} catch(Exception e) {};
	}

}

//topLevel: (NL)*( namedVar (NL)* )* {System.out.println("1\n");}{System.out.println("2\n");} INDENT_START {System.out.println("3\n");}PURE_NAME {System.out.println("4\n");}NL {System.out.println("5\n");}INDENT_END {System.out.println("6\n");}EOF;
topLevel: (NL)*( namedVar (NL)* )* EOF;
namedVar: namedScalar | namedArray | namedHash | fileHandle;

namedArray: name:ARRAY_NAME{addVar(name.getText(),"Array");System.out.println("++++ARRAY:"+name.getText()+"\n");} EQ PAREN_OP NL (arrayEntries)? {finalizeVar(); System.out.println("---- ARRAY:"+name.getText()+"\n");} PAREN_CL;
arrayReference: ARRAY_REF name:ADR NL {addVar("Array->"+name.getText()," ");System.out.println("++++ARRAYREF:"+name.getText()+"\n");}(arrayEntries)? {finalizeVar();System.out.println("----ARRAYREF:"+name.getText()+"\n");};
arrayEntries:  INDENT_START (arrayEntry)* INDENT_END
			| INDENT_START REF_SYMB PURE_NAME NL INDENT_END;
arrayEntry: start:NUMBER {addVar("["+start.getText()+"]",null);System.out.println("INDEX:"+start.getText());} value {finalizeVar();};



namedHash: name:HASH_NAME{addVar(name.getText(),"Hash");System.out.println("++++HASH:"+name.getText()+"\n");} EQ PAREN_OP NL (hashEntries)? {finalizeVar();System.out.println("----HASH:"+name.getText()+"\n");}PAREN_CL;
hashReference: HASH_REF name:ADR NL {addVar("Hash->"+name.getText()," ");System.out.println("++++HASHREF:"+name.getText()+"\n");}(hashEntries)?{finalizeVar();System.out.println("---HASHREF:"+name.getText()+"\n");};
hashEntries:  	INDENT_START (hashEntry)* INDENT_END 
							| INDENT_START REF_SYMB PURE_NAME NL INDENT_END ;
hashEntry:  start:STRING{System.out.println("H["+start.getColumn()+"]");}  KEY_ASSIGN {addVar("\'"+start.getText()+"\'","");System.out.println("KEY:"+start.getText());}value {finalizeVar();};

namedScalar: name:SCALAR_NAME{addVar(name.getText(),"Scalar");System.out.println("++++SCALAR:"+name.getText()+"\n");} ( EQ value {finalizeVar();System.out.println("----SCALAR:"+name.getText()+"\n");} | EQ pn2:PURE_NAME EQ {appendName("="+pn2.getText());} refs {finalizeVar();System.out.println("----SCALAR:"+name.getText()+"\n");});
scalarRef: SCALAR_REF name:ADR{addVar("Scalar->"+name.getText()," ");System.out.println("++++SCALARREF:"+name.getText()+"\n");} NL INDENT_START REF_SYMB value {finalizeVar();System.out.println("----SCALARREF:"+name.getText()+"\n");}INDENT_END;

codeRef: CODE_REF name:ADR{addVar("Code->"+name.getText()," ");setVal(name.getText(),"CodeRef");System.out.println("++++CODEREF:"+name.getText()+"\n");} NL INDENT_START REF_SYMB (a:MODULE_NAME{appendVal(a.getText());})? (( b:PURE_NAME {appendVal(b.getText());}) | (c:NUMBER{appendVal(c.getText());}) | (d:ADR{appendVal(d.getText());}))* NL{finalizeVar();System.out.println("----CodeREF:"+name.getText()+"\n");}INDENT_END;

ref: REF name:ADR{System.out.println("++++REF_SYMB:"+name.getText()+"\n");} NL INDENT_START REF_SYMB value {System.out.println("----REF_SYMB:"+name.getText()+"\n");}INDENT_END;

fileHandle: FILE_HANDLE PAREN_OP name:PURE_NAME{addVar(name.getText(),"FileHandle");System.out.println("++++FH:"+name.getText()+"\n");} PAREN_CL KEY_ASSIGN val:FILE_NO{setVal(val.getText(),"FileHandle"); finalizeVar();System.out.println("++++FNO_SYMB:"+val.getText()+"\n");};
fileHandleRef: name:FILE_REF{addVar("->"+name.getText(),"FileHandleRef");System.out.println("++++FRef:"+name.getText()+"\n");}  NL (INDENT_START fileHandle NL INDENT_END)? {finalizeVar();};

value: p:PURE_NAME{setVal(p.getText(),"");System.out.print(" VAL:"+p.getText());} ((p2:PURE_NAME){appendVal(" "+p2.getText());System.out.println(" "+p2.getText());})? {System.out.println("\n");}NL  | refs | s:STRING{setVal(s.getText(),"Scalar");System.out.println(" VAL:"+s.getText()+"\n");} NL | n:NUMBER{setVal(n.getText(),"Scalar");System.out.println(" VAL_NUM:"+n.getText()+"\n");}NL ;
refs:  (hashReference | arrayReference | scalarRef | codeRef | ref | fileHandleRef);
//refs: (PURE_NAME EQ)? (hashReference | arrayReference | scalarRef | codeRef | ref | fileHandleRef);
// ----------------------------------------------------------------------------
// the lexer

class PerlBaseLexer extends Lexer;



options
{
    exportVocab = Add;
    testLiterals = false;
    k = 3;
    charVocabulary = '\3'..'\377' ;
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
			|	("FileHandle") => "FileHandle"	{$setType(FILE_HANDLE);}
			|   ("fileno(") => ("fileno(") ('0'..'9')+ ')' {$setType(FILE_NO);}
			|	PURE_NAME {$setType(PURE_NAME);}
			;
//protected PURE_NAME_CHAR: 		~('\n'| '\r' | ' ' | '\t' |'=' | '\'' | '$' | '@' | '%' | '(' | ')' | '-');//FIRST_PURE_NAME_CHAR | '0'..'9' | '?' | '*';
protected PURE_NAME_CHAR: 		~('\n'| '\r' | ' ' | '\t' |'='| ')' | '(' );
//FIRST_PURE_NAME_CHAR | '0'..'9' | '?' | '*';
protected FIRST_PURE_NAME_CHAR: ~('\n'| '\r' | ' ' | '\t' |'=' | '\'' | '$' | '@' | '%' | '(' | ')' | '-' | '*' | '0'..'9');//FIRST_PURE_NAME_CHAR | '0'..'9' | '?' | '*';
protected PURE_NAME:   			(FIRST_PURE_NAME_CHAR) (PURE_NAME_CHAR)*;
ADR: 					"(0x" ('0'..'9' | 'a'..'f')+')';
REF_SYMB:					"->";
KEY_ASSIGN:				"=>";
STRING:					'\''( (~('\'' | '\\')) | CHAR_ESC )* '\'';
NUMBER:					('0'..'9')+('.'('0'..'9')+)?;
EQ: '=';
WS:
    ( ' '
    | '\t'
    )+
    {
    	if(  makeToken(WS).getColumn() != 1)
    		$setType(Token.SKIP);
	    else
	    {
	    	int new_col =   text.length()-_begin;
	    	//System.out.println("§§§Indent: "+new_col+"§§§§§\n");
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
    | 'c'
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
