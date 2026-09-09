package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionResult;

public class PerlVariableWatchExpressionResult implements IWatchExpressionResult {

	IDebugElement debugElement=null;
	public PerlVariableWatchExpressionResult(String arg0, IDebugElement arg1){
		debugElement=arg1;
	}
	
	private StackFrame getStackFrame(){
		if(debugElement instanceof StackFrame){
			return (StackFrame)debugElement;
		}
		return null;
	}
	
	@Override
	public String[] getErrorMessages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DebugException getException() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExpressionText() {
		// TODO Auto-generated method stub
		return "boo";
	}

	@Override
	public IValue getValue() {
		// TODO Auto-generated method stub
		try {
			if(getStackFrame() != null){
				return new LexicalVariable(getStackFrame().getPerlThread().getDB(), getStackFrame(), new org.epic.debug.db.DumpedEntity("", new String[]{"SCALAR(0x80070450)"}, "'hi'", 2)).getValue();
			}
		} catch (DebugException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean hasErrors() {
		// TODO Auto-generated method stub
		return false;
	}

}
