package org.epic.debug;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.DebugPlugin;
import org.epic.debug.util.ExecutionArguments;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public abstract class Target implements IDebugTarget {

	private IPath mWorkinkDir;
	ILaunch mLaunch;
	 IProcess mProcess;
	Process mJavaProcess;
	final static String EMPTY_STRING="";
	String mProcessName ;
	

	/**
	 * Constructor for DebugTarget.
	 */
	public Target() {
		super();
	}

/**
	 * Constructor for DebugTarget.
	 */
	public Target(ILaunch launch) {
		super();
		mLaunch = launch;
			}
	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
	 */
	public IProcess getProcess() {
		return mProcess;
	}
	
	public void setProcess( IProcess fProcess)
	{
		mProcess = fProcess;
	}
	
	
	

	
	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getName()
	 */
	public String getName() throws DebugException {
		
		return (mProcessName);
	}

		/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return this;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return mLaunch;
	}

	
	Process startPerlProcess()
		{
			String startfile = null;
			String prjName = null;
			String progParams = null;
			mProcessName ="Perl-Interpreter";
			try {
				 startfile = mLaunch.getLaunchConfiguration().getAttribute(PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE
										, EMPTY_STRING);
				 prjName =  mLaunch.getLaunchConfiguration().getAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME
										, EMPTY_STRING);
				progParams = mLaunch.getLaunchConfiguration().getAttribute(PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS										, EMPTY_STRING);
										
			} catch (Exception ce) {PerlDebugPlugin.log(ce);}
			IProject prj = PerlDebugPlugin.getWorkspace().getRoot().getProject(prjName);
		
			IPath path = prj.getLocation().append(startfile);
			IPath workingDir =  path.removeLastSegments(1);
		
			List fCmdList = null;
			try{
					fCmdList=PerlExecutableUtilities.getPerlExecutableCommandLine(prj);
				} catch ( Exception e){ System.out.println(e);}
		
	
			if( mLaunch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE) )
			{
						fCmdList.add("-d");
						mProcessName = "Perl-Debugger";
			}
			if (PerlEditorPlugin.getDefault().getWarningsPreference()) 
			{
				fCmdList.add("-w");
			 }
		
			if (PerlEditorPlugin.getDefault().getTaintPreference()) 
			{
			fCmdList.add("-T");
			}
		
			fCmdList.add(startfile);
			if( progParams.length() > 0)
			{
				 ExecutionArguments exArgs = new ExecutionArguments(progParams);
				 fCmdList.addAll(exArgs.getProgramArgumentsL());
			}
			String[] cmdParams =
			(String[]) fCmdList.toArray(new String[fCmdList.size()]);

			mWorkinkDir = workingDir;
			try {
				mJavaProcess= Runtime.getRuntime().exec(
							cmdParams,
							PerlDebugPlugin.getDebugEnv(),
							new File(workingDir.toString()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
				//	if( ! isTerminated())
				
				
				
				/**************org***/
				mProcess = DebugPlugin.newProcess(mLaunch,mJavaProcess,mProcessName);
				
				return(mJavaProcess);

		}
		
	IPath getWorkingDir()
	{ return mWorkinkDir;}
	
	
}