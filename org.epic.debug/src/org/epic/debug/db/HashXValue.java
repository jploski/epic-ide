package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;

class HashXValue extends PerlXValue {
	public HashXValue(String name, StackFrame f)
			throws DebugException {
		super(name, f);
	}

	public boolean add(PerlXValue e) throws DebugException {
		if (e != null) {
			e.expression=this.expression+"->{"+e.name+"}";
			vars.add(new HashXKey(e));
			return true;
		}
		return false;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return "HASH";
	}

	@Override
	public String getPrefix() {
		return "%";
	}
	
	@Override
	public String getBraces() {
		return "{}";
	}
}
