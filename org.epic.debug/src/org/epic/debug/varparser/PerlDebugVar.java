package org.epic.debug.varparser;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/*
 * Created on 16.01.2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlDebugVar extends DebugElement implements IVariable {

	private IDebugElement mDebugger;
	private String mName;
	private PerlDebugValue mValue;
	private int mHasChanged;
	private boolean mHideImage;
	private boolean mVisited;
	private boolean mIsSet;
	int mScope;
	public static final int IS_LOCAL_SCOPE = 1; 
	public static final int IS_GLOBAL_SCOPE = 2;  
	
	public boolean getHideImage()
	{
		return mHideImage;
	}
	
	public boolean matches(PerlDebugVar fVar)
	{
		if( ! fVar.mName.equals(mName))
			return(false);
		if(  fVar.mScope != mScope )
			return(false);
			
		return(true);
	}
			/**
		 * Constructor for Variable.
		 */
		

		public boolean isLocalScope()
		{
			return( mScope == IS_LOCAL_SCOPE);
		}
	
		public boolean isGlobalScope()
		{
			return( mScope == IS_GLOBAL_SCOPE);
		}
	
	public boolean calculateChangeFlags(PerlDebugVar fVarOrg)
	{
		if( fVarOrg.mVisited == true) 
			return(false);
		
		fVarOrg.mVisited = true;
		try{
		
		//System.out.println("-*-Comparing Var"+fVarOrg.getName()+"/"+getName());
		if( ! getName().equals(fVarOrg.getName()) )
		 {
		 	System.err.println("*****Error****:Comparing Variables with different Names");
		 	return false;
		 }
		 
	    int ret = mValue.calculateChangeFlags(fVarOrg.getPdValue());
	    
	   
	    setChangeFlags(ret,false);
	    
	    return( ret != PerlDebugValue.mValueUnchanged );
	    	 
	    } catch( Exception e ){System.out.println(e);}
		
		return false;	
	}
	
	public void setChangeFlags(int fVal, boolean fRecourse)
	{
		if( mIsSet == true) return;
		
		else mIsSet = true;
		mHasChanged = fVal;
		
		if( fRecourse )
			mValue.setChangeFlags(fVal, fRecourse);
	}
	
	/**
	 * 
	 */
	public PerlDebugVar(IDebugElement fDebugger, int fScope) {
		super(fDebugger.getDebugTarget());
		mDebugger = fDebugger;
		mName = null;
		mValue = null;
		mHideImage = false;
		mScope = fScope;
		mHasChanged = PerlDebugValue.mValueUnchanged;
		mVisited = false;
		mIsSet = false;
	}
	
	public PerlDebugVar(IDebugElement fDebugger,int fScope, boolean fHide) {
		super(fDebugger.getDebugTarget());
			mDebugger = fDebugger;
			mName = null;
			mValue = null;
			mHideImage = fHide;
			mScope = fScope;
			mVisited = false;
			mIsSet = false;
		}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IVariable#getValue()
	 */
	public IValue getValue() throws DebugException {
		return mValue;
	}
	
	public PerlDebugValue getPdValue() throws DebugException {
			return mValue;
		}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IVariable#getName()
	 */
	public String getName() throws DebugException {
		return mName;
	}
	
	public void setName(String fName)
	{
		mName = fName;
	}
	
	public void appendName(String fName)
		{
			mName = mName + fName;
		}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IVariable#getReferenceTypeName()
	 */
	public String getReferenceTypeName() throws DebugException {
		if( mValue != null )
			return( mValue.getReferenceTypeName());
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IVariable#hasValueChanged()
	 */
	public boolean hasValueChanged() throws DebugException {
		return ( (mHasChanged & PerlDebugValue.mValueHasChanged) > 0);
	}

	public boolean isTainted() throws DebugException {
			return ( (mHasChanged & PerlDebugValue.mIsTainted) > 0);
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
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(java.lang.String)
	 */
	public void setValue(String expression) throws DebugException {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(org.eclipse.debug.core.model.PerlDebugValue)
	 */
	public void setValue(PerlDebugValue value) throws DebugException {
		mValue= value;

	}
	public void setValue(IValue value) throws DebugException {
			mValue= (PerlDebugValue) value;

		}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#supportsValueModification()
	 */
	public boolean supportsValueModification() {
			return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(java.lang.String)
	 */
	public boolean verifyValue(String expression) throws DebugException {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(org.eclipse.debug.core.model.PerlDebugValue)
	 */
	public boolean verifyValue(PerlDebugValue value) throws DebugException {
		return false;
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
	public boolean verifyValue(IValue fVal)
	{
		return false;
	}

}
