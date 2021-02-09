package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;

class WatchValue extends ArrayXValue {
	WatchVar var=null;
	PerlXValue hiddenValue=null;
	public WatchValue(StackFrame f) throws DebugException {
		super("watch", f);
	}

	
	
	public boolean add(PerlXValue e) throws DebugException {
		if (e != null) {
			hiddenValue=e;
			e.expression=this.expression;
			var=new WatchVar(e);
			vars.add(var);
			return true;
		}
		return false;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return "WATCH";
	}
	
	@Override
	public String getDetailValue() throws DebugException {
		return hiddenValue.getDetailValue();
	}
	
	@Override
	public String getValueString() throws DebugException {
		return hiddenValue.getValueString();
	}
}
