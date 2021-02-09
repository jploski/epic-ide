package org.epic.debug.db;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.eclipse.jface.action.IAction;
import org.epic.debug.DebugTarget;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PerlXWatchExpressionDelegate implements org.eclipse.debug.core.model.IWatchExpressionDelegate
{
	@Override
	public void evaluateExpression(String arg0, IDebugElement arg1,
			IWatchExpressionListener arg2) {
		StackFrame stackFrame=null;
		if(arg1 instanceof StackFrame) stackFrame=(StackFrame) arg1;
		if(stackFrame!=null){
			DebuggerInterface db=stackFrame.getPerlThread().getDB();
			try {
				String result=db.eval("x ;{ ("+arg0+")?\\"+arg0+":undef };");
				if(result.trim().equals("0  undef")){
					arg2.watchEvaluationFinished(new PerlWatchExpressionStringResult("undef", stackFrame.getDebugTarget()));
					return;
				}
				PerlXReader r = new PerlXReader(arg0, result, stackFrame);
				arg2.watchEvaluationFinished(new PerlWatchExpressionVarResult(r.getValue(), stackFrame.getDebugTarget()));
				return;
			} catch (IOException e) {
			} catch (DebugException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			arg2.watchEvaluationFinished(new FailedPerlWatchExpressionResult(new String[]{"Failed to run expression"}, null));
		}
	}
	
	class PerlWatchExpressionStringResult implements IWatchExpressionResult {

		StringValue result;
		IDebugTarget debugTarget;

		public PerlWatchExpressionStringResult(String result, IDebugTarget iDebugTarget){
			this.result=new StringValue(result, iDebugTarget);
			this.debugTarget = iDebugTarget;
		}

		@Override
		public String[] getErrorMessages() {
			return null;
		}

		@Override
		public DebugException getException() {
			return null;
		}

		@Override
		public String getExpressionText() {
			return "boo";
		}

		@Override
		public IValue getValue() {
			return result;
		}

		@Override
		public boolean hasErrors() {
			return false;
		}
	}

	class PerlWatchExpressionVarResult implements IWatchExpressionResult {

		IValue result;
		IDebugTarget debugTarget;

		public PerlWatchExpressionVarResult(IValue result, IDebugTarget iDebugTarget){
			this.result=result;
			this.debugTarget = iDebugTarget;
		}

		@Override
		public String[] getErrorMessages() {
			return null;
		}

		@Override
		public DebugException getException() {
			return null;
		}

		@Override
		public String getExpressionText() {
			return "boo";
		}

		@Override
		public IValue getValue() {
			return result;
		}

		@Override
		public boolean hasErrors() {
			return false;
		}
	}

	class FailedPerlWatchExpressionResult implements IWatchExpressionResult {

		String[] errs;
		DebugException exception;
		public FailedPerlWatchExpressionResult(String[] errs, DebugException exception){
			this.errs = errs;
			this.exception = exception;
		}
		
		
		@Override
		public String[] getErrorMessages() {
			return errs;
		}

		@Override
		public DebugException getException() {
			return exception;
		}

		@Override
		public String getExpressionText() {
			return "boo";
		}

		@Override
		public IValue getValue() {
			return null;
		}

		@Override
		public boolean hasErrors() {
			return true;
		}

	}
}