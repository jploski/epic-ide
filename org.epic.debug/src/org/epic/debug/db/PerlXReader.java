package org.epic.debug.db;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.DebugTarget;

public class PerlXReader {
	String xoutput;
	public PerlXReader(String expression, String xoutput, StackFrame stack) throws DebugException{
		this.xoutput=xoutput;
		root=new WatchValue(stack);
		root.expression = expression;
		StringBuilder subxoutput=new StringBuilder(this.xoutput);
		parseXRef(root, subxoutput, stack);
	}
	
	public WatchValue root;
	
	public static void main(String [] argv){
		try {
			PerlXReader x=new PerlXReader("boo", testPattern2, null);
		} catch (DebugException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public PerlXValue getValue(){
		if(root.vars.size()>1 || !(root.hiddenValue instanceof RefXValue)) return root;
		return root.hiddenValue;
	}
	
	private void parseXRef(PerlXValue cur, StringBuilder subxoutput, StackFrame stackFrame) throws DebugException{
		int level=getLevel(subxoutput);
		while(!endReached(subxoutput, level)){
			PerlXValue nextValue=grabNextElement(stackFrame, subxoutput, cur.expression);
			if(nextValue!=null){
				cur.add(nextValue);
				if(!(nextValue instanceof ScalarXValue)) parseXRef(nextValue, subxoutput, stackFrame);
			}else{
				return;
			}
		}
		
	}

	private boolean endReached(StringBuilder s, int level) {
		return getLevel(s)<level;
	}

	private int getLevel(StringBuilder subxoutput){
		Pattern p=Pattern.compile("^\\s+");
		Matcher m=p.matcher(subxoutput);
		if(m.find()){
			return m.end();
		}
		return 0;
	}
	
	private PerlXValue grabNextElement(StackFrame f, StringBuilder subxoutput, String expressionBase) throws DebugException {
		//1  ARRAY(0x80449e08)
		//'a' => ARRAY(0x80449e08)
		//-> ARRAY(0x80449e08)
		//see if we match some reference type
		Pattern arrayPattern=Pattern.compile("^\\s*([^' ]+|'(?:[^'\\\\]|\\\\.)*')\\s+(?:=>\\s+)?(ARRAY|HASH|SCALAR|REF)\\((.*?)\\)(?:\\r?\\n|$)");
		Matcher m=arrayPattern.matcher(subxoutput);
		if(m.find()){
			String name=m.group(1);
			String refType=m.group(2);
			String address=m.group(3);
			subxoutput.replace(0, m.end(), "");
			if("ARRAY".equals(refType)){
				ArrayXValue val=new ArrayXValue(name, f);
				return val;
			}
			if("HASH".equals(refType)){
				HashXValue val=new HashXValue(name, f);
				return val;
			}
			RefXValue val=new RefXValue(name, f);
			return val;
		}
		
		//if we haven't matched a reference type we must just be a scalar value
		arrayPattern=Pattern.compile("^\\s*([^' ]+|'(?:[^'\\\\]|\\\\.)*')\\s+(?:=>\\s+)?([^' ]+|'((?:[^'\\\\]|\\\\.)*)')(?:\\r?\\n|$)");
		m=arrayPattern.matcher(subxoutput);
		if(m.find()){
			String name=m.group(1);
			String value=m.group(2);
			subxoutput.replace(0, m.end(), "");
			ScalarXValue val=new ScalarXValue(name, f, value);
			return val;
		}
		return null;
	}

	static String testPattern1=
		"0  HASH(0x80449d60)"			+ "\n" +
	    "   'one' => 1"					+ "\n" +
	    "   'two' => HASH(0x80449bb0)" 	+ "\n" +
	    "      'four' => 4" 			+ "\n" +
	    "      'three' => 3" 			+ "\n";
	
	static String testPattern2=
	"0  'one\\'\none'"				+ "\n" +
	"1  ARRAY(0x80449e08)"		+ "\n" +
	"   0  'two'"				+ "\n" +
	"   1  'three'"				+ "\n" +
	"2  ARRAY(0x80449da8)"		+ "\n" +
	"   0  'five'"				+ "\n" +
	"   1  ARRAY(0x8040a570)"	+ "\n" +
	"      0  'six'"			+ "\n" +
	"      1  'seven'"			+ "\n";
}
