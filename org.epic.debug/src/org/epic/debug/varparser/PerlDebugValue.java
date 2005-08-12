/*
 * Created on 16.01.2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug.varparser;
import java.util.Iterator;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlDebugValue extends DebugElement implements IValue {

	public static int mIsTainted = 1;
	public static int mValueHasChanged =2;
	public static int mValueUnchanged =0;
	
	java.util.ArrayList mVars = new java.util.ArrayList();
	String mValue;
	String mType;
	IDebugElement mDebugger;
	
	static String changeFlagsToString(int fVal)
	{
		String erg =" ";
		
			if( (fVal & mValueHasChanged) > 0 )
				erg = erg+"*****Changed";
			if( (fVal & mIsTainted) > 0 )
				erg = erg+"*****Tainted";
			if( fVal == mValueUnchanged )
				erg= "+++unchanged";
				
			return erg;
				
	}
	
	void setChangeFlags(int fVal, boolean fRecourse)
		{
	
			Iterator it = mVars.iterator();
			
			while( it.hasNext() )
			{
				((PerlDebugVar)it.next()).setChangeFlags(fVal,fRecourse);
			}
			
		}
		
	public int calculateChangeFlags(PerlDebugValue fValOrg)
	{
		int ret =0;
	  try{
	  	
		//System.out.println("-*-Comparing Value"+fValOrg.mValue+"/"+mValue);
			if( ((mValue == null) && (fValOrg.getValueString()!= null))
				|| ! mValue.equals(fValOrg.getValueString()) )
			 	{
					ret = mValueHasChanged;
				} 
		 	
			if( ((mType == null) && (fValOrg.mType != null))
				|| ! mType.equals(fValOrg.mType) )
				{
					ret = mValueHasChanged;
				}
			
			
			int count_org = fValOrg.mVars.size();
			int count_new = mVars.size();
			PerlDebugVar var_org,var_new;
			
			boolean found;
			
			if( count_org != count_new)
				ret |= mIsTainted;
								
			for( int new_pos = 0; new_pos < count_new; ++new_pos)
			{
				found = false;
				var_new = (PerlDebugVar) mVars.get(new_pos);
				
				for( int org_pos = 0; (org_pos < count_org) && !found; ++org_pos)
				{
					var_org = (PerlDebugVar) fValOrg.mVars.get(org_pos);
					if( var_org.getName().equals(var_new.getName() ) )
					{
						found = true;
						if (var_new.calculateChangeFlags(var_org) )
						 ret |= mIsTainted;
					}
				}
				
				if ( !found )
				{
					var_new.setChangeFlags(mValueHasChanged,true);
					ret |=  mIsTainted;
				}
			}	
			
		
			return(ret);					 
		
			} catch( Exception e ){System.out.println(e);}
		
			return(mValueUnchanged);
		}
	/**
	 * 
	 */
	public PerlDebugValue(IDebugElement fDebugger) {
		super(fDebugger.getDebugTarget());
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
		if (adapter == this.getClass()) {
			return this;
		}
		return super.getAdapter(adapter);
	}
	public void addVar(PerlDebugVar fVar )
	{
		mVars.add(fVar);
	}
	
}
