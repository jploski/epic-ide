package org.epic.debug;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
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
public abstract class Target extends DebugElement implements IDebugTarget
{

	private boolean mShutDownStarted;
	protected IPath mProjectDir;
	protected String mStartupFile;
	protected String mStartupFileAbsolut;
	protected IPath mWorkingDir;
	ILaunch mLaunch;
	IProcess mProcess;
	Process mJavaProcess;
	final static String EMPTY_STRING = "";
	String mProcessName;
	protected IProject mProject;


	/**
	 * Constructor for DebugTarget.
	 */
	public Target()
	{
		super(null);
	}

	/**
		 * Constructor for DebugTarget.
		 */
	public Target(ILaunch launch)
	{
		super(null);
		mLaunch = launch;
		initPath();
		fireCreationEvent();
		//	((PerlDebugPlugin)PerlDebugPlugin.getDefault()).registerDebugTarget(this);
	}
	
	abstract boolean isLocal();
	
	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
	 */
	public IProcess getProcess()
	{
		return mProcess;
	}

	public void setProcess(IProcess fProcess)
	{
		mProcess = fProcess;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugTarget#getName()
	 */
	public String getName() throws DebugException
	{

		return (mProcessName);
	}

	/**
	* @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	*/
	public IDebugTarget getDebugTarget()
	{
		return this;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch()
	{
		return mLaunch;
	}

	public String getJavaHome()
	{
		
	
		return (System.getProperty("java.home"));
	}

	void initPath()
	{
		String startfile = null;
		String prjName = null;
		String progParams = null;

		
		try
		{
			startfile =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
					EMPTY_STRING);
			prjName =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					EMPTY_STRING);

		} catch (Exception ce)
		{
			PerlDebugPlugin.log(ce);
		}
		IProject prj =
			PerlDebugPlugin.getWorkspace().getRoot().getProject(prjName);
		mProject = prj;
		mProjectDir = prj.getLocation();
		IPath path = mProjectDir.append(startfile);
		mWorkingDir = path.removeLastSegments(1);
		mStartupFile = path.lastSegment();
		mStartupFileAbsolut = prj.findMember(startfile).getLocation().toString();
	}
	
	Process startPerlProcess()
	{
		
		initPath();
		String startfile = null;
		String prjName = null;
		String progParams = null;
		mProcessName = "Perl-Interpreter";
		try
		{
			startfile =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
					EMPTY_STRING);
			prjName =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					EMPTY_STRING);
			progParams =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROGRAM_PARAMETERS,
					EMPTY_STRING);

		} catch (Exception ce)
		{
			PerlDebugPlugin.log(ce);
		}

		IProject prj =
			PerlDebugPlugin.getWorkspace().getRoot().getProject(prjName);

		IPath workingDir = getLocalWorkingDir();

		List fCmdList = null;
		try
		{
			fCmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(prj);
		} catch (Exception e)
		{
			PerlDebugPlugin.getDefault().logError(
				"Could not start Debug Process (Erroror assambling command line !",
				e);
		}
		fCmdList.add("-I"+PerlDebugPlugin.getPlugInDir());
		if (mLaunch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE))
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

		
		
		//startfile = getProjectDir().toString()+"/"+startfile;
		fCmdList.add(mStartupFileAbsolut);
		if (progParams.length() > 0)
		{
			ExecutionArguments exArgs = new ExecutionArguments(progParams);
			fCmdList.addAll(exArgs.getProgramArgumentsL());
		}
		String[] cmdParams =
			(String[]) fCmdList.toArray(new String[fCmdList.size()]);

		try
		{
			mJavaProcess =
				Runtime.getRuntime().exec(
					cmdParams,
					PerlDebugPlugin.getDebugEnv(this),
					new File(workingDir.toString()));
		} catch (IOException e1)
		{
			PerlDebugPlugin.getDefault().logError(
				"Could not start Debug Process!",
				e1);
		}

		
		//	if( ! isTerminated())

		/**************org***/
		mProcess = DebugPlugin.newProcess(mLaunch, mJavaProcess, mProcessName);

		return (mJavaProcess);

	}

	public IPath getLocalWorkingDir()
	{
		return mWorkingDir;
	}

	public IPath getProjectDir()
	{
			return mProjectDir;
	}

	public String getStartupFile()
	{
		return mStartupFile;
	}
//	/**
//		 * Fire a debug event marking the creation of this element.
//		 */
//	private void fireCreationEvent()
//	{
//		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
//	}

	 void fireChangeEvent()
	{
		fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
	}
	 void fireCreateEvent()
	{
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	
	public void shutdown()
	{
		shutdown(true);
	}

		public void killDebugProcess()
		{
			if (mProcess != null)
				try {
					mProcess.terminate();
				} catch (DebugException e) {
					// TODO Auto-generated catch block
					PerlDebugPlugin.getDefault().logError("Could not terminate debugger process", e);
				}
			
		}
	public void shutdown(boolean unregister)
	{
		
		if( mShutDownStarted)
					return;
				mShutDownStarted = true;

		//((PerlDebugPlugin)PerlDebugPlugin.getDefault()).unregisterDebugTarget(this);
		try
		{
			if (mProcess != null)
				mProcess.terminate();
		} catch (DebugException e)
		{
			PerlDebugPlugin.getDefault().logError(
				"Error Termonating " + mProcessName,
				e);
		}
		
		fireTerminateEvent();
	}

	public void start()
	{
	}
	
	
				
	/**
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == this.getClass()) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	
	
	public IProject getProject() {
		return mProject;
	}
	

}