/*
 * Created on 16.01.2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugElement;

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlDebugValue implements IValue {

	java.util.ArrayList mVars = new java.util.ArrayList();
	String mValue;
	String mType;
	IDebugElement mDebugger;
	/**
	 * 
	 */
	public PerlDebugValue(IDebugElement fDebugger) {
		super();
		mValue = null;
		mType = null;
		mDebugger = fDebugger;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValue#getReferenceTypeName()
	 */
	public String getReferenceTypeName() throws DebugException {
		return mType;
	}

	public void setType(String fType)
	{
		mType = fType;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValue#getValueString()
	 */
	public String getValueString() throws DebugException {
		return mValue;
	}
	
	public void setValue(String fVal)
	{
		mValue = fVal; 
	}
	
	public void appendValue(String fVal)
		{
			if (mValue == null )
				mValue = fVal;
			else
				mValue = mValue + fVal; 
		}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValue#isAllocated()
	 */
	public boolean isAllocated() throws DebugException {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValue#getVariables()
	 */
	public IVariable[] getVariables() throws DebugException {
		return ((IVariable[])mVars.toArray(new IVariable[mVars.size()]));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValue#hasVariables()
	 */
	public boolean hasVariables() throws DebugException {
		return (mVars.size()>0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return mDebugger.getModelIdentifier();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return mDebugger.getDebugTarget();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return mDebugger.getLaunch();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
			return null;
	}
	
	public void addVar(PerlDebugVar fVar )
	{
		mVars.add(fVar);
	}
	
}
