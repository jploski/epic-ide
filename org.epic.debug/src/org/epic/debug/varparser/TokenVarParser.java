package org.epic.debug.varparser;


import org.epic.debug.PerlDB;
import org.epic.debug.PerlDebugPlugin;



/**
 * The main for this example. It accepts the name of a file as a commandline
 * argument, and will interpret the contents of the file.
 * 
 * <p>
 * This file is in the public domain.
 * </p>
 * 
 * @author Dan Bornstein, danfuzz@milk.com
 */
public class TokenVarParser {

	private PerlDB mDebugger;
	private int mScope;

	java.util.Stack mVarStack = new java.util.Stack();
	java.util.ArrayList mVarList = null; //new java.util.ArrayList();
	int mPos;
	String indent;
	char mChars[];

	public TokenVarParser(PerlDB fDebugger) {
		mDebugger = fDebugger;
	}

	public java.util.ArrayList parseVars(String fText, int fScope) {
		return (parseVars(fText, fScope, new java.util.ArrayList()));
	}

	public java.util.ArrayList parseVars(String fText, int fScope,
			java.util.ArrayList fVarList) {
		boolean hasErrors = false;

		mChars = fText.toCharArray();

		mScope = fScope;
		setVarList(fVarList);

		mPos = 0;
		indent = " ";
		readVars();

		if (hasErrors) {
			System.out.println("!!!!! Parse Error!!!!");
			logParsError(fText);
		} else {
			System.out.println("!!!!! Parse OK!!!!");
		}
		return (getVars());

	}

	//***************************
	 void readVars() {
		int x = 1;
		do {
			x = readVar();
		} while (x == 1);
	}

	 int readVar() {
		String name, value;

		if (mChars[mPos] == 'E')
			return (0);
		while (mChars[mPos] != 'N') {
			System.err.println("Name not found[" + mPos + "]\n");
			++mPos;
		};
		mPos++;
		name = readString();
		value = readStrings();
		//System.out.println(indent + name + "=" + value + "\n");
		addVar(name);
		setValue(value);
		if (mChars[mPos] == 'I') {
			mPos++;
			String indentOrg = indent;
			indent = indent + "   ";
			readVars();
			indent = indentOrg;
		}
		finalizeVar();
		if (mChars[mPos] == 'O') {
			mPos++;
			return (0);
		}
		/* fine */
		return (1);
	}
	 String readString() {
		return (readString(true));
	}

	 String readString(boolean fPrintError) {
		if (mChars[mPos] != 'S') {
			if (fPrintError) {
				System.err.println("String not found[" + mPos + "]\n");
			}
			return (null);
		};
		mPos++;
		String temp = new String(mChars, mPos, 8);
		int length = Integer.parseInt(temp, 16);
		mPos += 8;
		temp = new String(mChars, mPos, length);
		mPos += length;
		return temp;
	}

	 String readStrings() {
		String erg, temp;

		erg = readString();
		if (erg == null)
			return (null);
		do {
			temp = readString(false);
			if (temp != null)
				erg += temp;
		} while (temp != null);
		return (erg);
	}

	////**************************

	public void setVarList(java.util.ArrayList fVarList) {
		mVarList = fVarList;
	}

	public PerlDebugVar[] getVarArray() {
		return ((PerlDebugVar[]) mVarList.toArray(new PerlDebugVar[mVarList
				.size()]));
	}

	public java.util.ArrayList getVars() {
		return (mVarList);
	}

	public void addVar(String fName) {
		addVar(fName, " ");
	}

	public void setValue(String fVal)
	{ setValue(fVal," ");}
	
	
	public void setValue(String fVal, String fType)
	{
			try{
			 ((PerlDebugVar)mVarStack.peek()).getPdValue().setValue(fVal);
	 		 ((PerlDebugVar)mVarStack.peek()).getPdValue().setType(fType);
	 		}catch (Exception e){};

	}
	public void addVar(String fName, String fType) {
		PerlDebugVar var = new PerlDebugVar(mDebugger, mScope);
		PerlDebugValue val = new PerlDebugValue(mDebugger);

		try {
			val.setType(fType);
			var.setName(fName);
			var.setValue(val);

			if (!mVarStack.empty()) {
				((PerlDebugVar) mVarStack.peek()).getPdValue().addVar(var);

			}

		} catch (Exception e) {
		};
		mVarStack.push(var);
	}

	public void finalizeVar() {
		PerlDebugVar var;
		var = (PerlDebugVar) mVarStack.pop();
		try {
			if (var.getPdValue().getValueString() == null) {
				var.getPdValue().appendValue("...");
			}
		} catch (Exception e) {
		}
		if (mVarStack.empty())
			mVarList.add(var);
		try {
		} catch (Exception e) {
		};
	}

	private void logParsError(String fText) {

		StringBuffer out = new StringBuffer();

		out.append("*******************************\n");
		out.append("*******************************\n");
		out.append("+++++++Error Parsing Vars++++++\n");
		out.append("*******************************\n");
		out.append("*******************************\n");

		out.append(fText);

		out.append("-------------------------------\n");
		out.append("-------------------------------\n");
		out.append("+++++++Error Parsing Vars++++++\n");
		out.append("-------------------------------\n");
		out.append("-------------------------------\n");

		PerlDebugPlugin.getDefault().logError(
				"Error Parsing Debugger Variables",
				new Exception(out.toString()));

	}

}