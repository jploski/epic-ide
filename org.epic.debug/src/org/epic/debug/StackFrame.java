package org.epic.debug;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.epic.debug.ui.action.*;
import org.epic.debug.varparser.PerlDebugVar;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class StackFrame extends DebugElement implements IStackFrame {

	/**
	 * Constructor for StackFrame.
	 */
	private IVariable [] mVars;
	private PerlDebugThread mThread;
	private int mIP_Line;
	private IPath mIP_Path;
	private ArrayList mVarsOrg;
	static HashMap mPerlInternalVars;

static{
	ResourceBundle rb = 
	ResourceBundle.getBundle("org.epic.debug.perlIntVars");
	Enumeration e = rb.getKeys();
	mPerlInternalVars = new HashMap();
	while( e.hasMoreElements())
		mPerlInternalVars.put(e.nextElement(),"1");

}

   public StackFrame(PerlDebugThread fThread) {
		super(fThread.getDebugTarget());
		
		mThread = fThread;
		mVars = new PerlDebugVar[0];
		}
	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#getThread()
	 */
	public IThread getThread() {
		return mThread;
	}
	
	public PerlDebugThread getPerlThread() {
			return mThread;
		}
	
	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#getVariables()
	 */
	public IVariable[] getVariables() throws DebugException {
		if (mVars == null)
		{ getPerlThread();
		}
		return mVars;
	}

	public void updateVars() {
		ArrayList vars= (ArrayList) mVarsOrg.clone();
		
		String lVarname = null;
		PerlDebugVar var = null;
		
		for (int i = 0; i < vars.size(); i++) {
			{
				try {
					var = (PerlDebugVar) vars.get(i);
					lVarname = var.getName();
				} catch (DebugException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (var.isGlobalScope()
						&& mPerlInternalVars.containsKey(lVarname)
						&&  !ShowPerlInternalVariableActionDelegate.getPreferenceValue()
				) 
				{
					vars.remove(i);
					--i;
				}
				
				if (var.isGlobalScope()
						&& !mPerlInternalVars.containsKey(lVarname)
						&&  !ShowGlobalVariableActionDelegate.getPreferenceValue()
				) 
				{
					vars.remove(i);
					--i;
				}
				
				if (var.isLocalScope()
						&&  !ShowLocalVariableActionDelegate.getPreferenceValue()
				) 
				{
					vars.remove(i);
					--i;
				}
			}

		}
		
		try {
			mVars= ((PerlDebugVar[]) vars.toArray(new PerlDebugVar[vars.size()]));
		} catch (Exception e) {
		};
	}

	
	public void setVariables(PerlDebugVar[] fVars)
	{
		mVars = fVars;
	}
	
	public void setVariables(ArrayList fVars) throws DebugException {
		mVarsOrg = fVars;
		updateVars();
	}
		/**
	 * @see org.eclipse.debug.core.model.IStackFrame#hasVariables()
	 */
	public boolean hasVariables() throws DebugException {
		return ( mVars != null && mVars.length > 0);
	}

	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#getLineNumber()
	 */
	public int getLineNumber() throws DebugException {
		return get_IP_Line();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#getCharStart()
	 */
	public int getCharStart() throws DebugException {
		return -1;
	}

	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#getCharEnd()
	 */
	public int getCharEnd() throws DebugException {
		return -1;
	}

	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#getName()
	 */
	public String getName() throws DebugException {
		return (mIP_Path.lastSegment()+"[line: "+Integer.toString(mIP_Line)+"]");
	}

	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#getRegisterGroups()
	 */
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	/**
	 * @see org.eclipse.debug.core.model.IStackFrame#hasRegisterGroups()
	 */
	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return PerlDebugPlugin.getUniqueIdentifier();
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return mThread.getDebugTarget();
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return mThread.getLaunch();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepInto()
	 */
	public boolean canStepInto() {
		return mThread.canStepInto();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepOver()
	 */
	public boolean canStepOver() {
		return  mThread.canStepOver();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepReturn()
	 */
	public boolean canStepReturn() {
		return mThread.canStepReturn();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#isStepping()
	 */
	public boolean isStepping() {
		return mThread.isStepping();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#stepInto()
	 */
	public void stepInto() throws DebugException {
		mThread.stepInto();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#stepOver()
	 */
	public void stepOver() throws DebugException {
		mThread.stepOver();
	}

	/**
	 * @see org.eclipse.debug.core.model.IStep#stepReturn()
	 */
	public void stepReturn() throws DebugException {
		mThread.stepReturn();
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
	 */
	public boolean canResume() {
		return mThread.canResume();
		
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		return mThread.canSuspend();
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		return mThread.isSuspended();
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
		mThread.resume();
	}

	/**
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
		mThread.suspend();
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate() {
		return mThread.canTerminate();
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated() {
		return mThread.isTerminated();
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException {
		mThread.terminate();
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if( adapter == this.getClass() )
			return this;
		else
		 return super.getAdapter(adapter);
	}

	/**
	 * @return
	 */
	public int get_IP_Line() {
		return mIP_Line;
	}

	/**
	 * @return
	 */
	public IPath get_IP_Path() {
		return mIP_Path;
	}

	/**
	 * @param i
	 */
	public void set_IP_Line(int i) {
		mIP_Line = i;
	}

	/**
	 * @param path
	 */
	public void set_IP_Path(IPath path) {
		mIP_Path = path;
	}

	
	
}