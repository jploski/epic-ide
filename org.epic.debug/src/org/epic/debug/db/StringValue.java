package org.epic.debug.db;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class StringValue implements IValue {

	private String value;
	private IDebugTarget debugTarget;
	public StringValue(String value, IDebugTarget iDebugTarget){
		this.value=value;
		this.debugTarget = iDebugTarget;
	}
	@Override
	public IDebugTarget getDebugTarget() {
		return this.debugTarget;
	}

	@Override
	public ILaunch getLaunch() {
		return this.debugTarget.getLaunch();
	}

	@Override
	public String getModelIdentifier() {
		return this.debugTarget.getModelIdentifier();
	}

	@Override
	public Object getAdapter(Class arg0) {
		return this.debugTarget.getAdapter(arg0);
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return "SCALAR";
	}

	@Override
	public String getValueString() throws DebugException {
		return value;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return null;
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return false;
	}

	@Override
	public boolean isAllocated() throws DebugException {
		return true;
	}

}
