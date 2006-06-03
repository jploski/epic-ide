package org.epic.debug;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.epic.core.PerlCore;
import org.epic.core.util.PerlExecutableUtilities;
import org.epic.debug.util.ExecutionArguments;
import org.epic.perleditor.PerlEditorPlugin;

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
        IPath path = prj.getFile(new Path(startfile)).getLocation();
		mStartupFile = path.lastSegment();
		mStartupFileAbsolut = path.toString();
	}
	
	protected Process startPerlProcess()
	{
		
		initPath();
		String prjName = null;
        String perlParams = null;
		String progParams = null;
		mProcessName = "Perl-Interpreter";
		try
		{
			prjName =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					EMPTY_STRING);
            perlParams =
                mLaunch.getLaunchConfiguration().getAttribute(
                    PerlLaunchConfigurationConstants.ATTR_PERL_PARAMETERS,
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

		IPath workingDir;
        
        try
        {
            workingDir = getLocalWorkingDir();
        }
        catch (CoreException e)        
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not start Perl interpreter: invalid working directory",
                e);
            return null;
        }

		List fCmdList = null;
		try
		{
			fCmdList =
				PerlExecutableUtilities.getPerlCommandLine(
                    PerlCore.create(prj));
		} catch (Exception e)
		{
			PerlDebugPlugin.getDefault().logError(
				"Could not start Perl interpreter: error assambling command line",
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
        if (perlParams != null && perlParams.length() > 0)
        {
            ExecutionArguments exArgs = new ExecutionArguments(perlParams);
            fCmdList.addAll(exArgs.getProgramArgumentsL());
        }

		fCmdList.add(mStartupFileAbsolut);
        
		if (progParams != null && progParams.length() > 0)
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
					workingDir.toFile());
		}
        catch (Exception e1)
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

	public IPath getLocalWorkingDir() throws CoreException
	{
        File workingDir = verifyWorkingDirectory(mLaunch.getLaunchConfiguration());
        if (workingDir == null) workingDir = getDefaultWorkingDir();        
		return Path.fromOSString(workingDir.getAbsolutePath());
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
    
    /**
     * Returns the default working directory used when none is specified
     * in the launch configuration - the parent directory of the script. 
     */
    private File getDefaultWorkingDir()
    {
        return Path.fromOSString(mStartupFileAbsolut).removeLastSegments(1).toFile();
    }
	
    /**
     * Returns the working directory path specified by the given launch
     * configuration, or <code>null</code> if none.
     * 
     * @param configuration  launch configuration
     * @return the working directory path specified by the given launch
     *         configuration, or <code>null</code> if none
     * @exception CoreException
     *            if unable to retrieve the attribute
     */
    private IPath getWorkingDirectoryPath(ILaunchConfiguration configuration) throws CoreException
    {
        String path = configuration.getAttribute(
            PerlLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
            (String) null);

        if (path == null) return null;
        else
        {
            path = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(path);
            return new Path(path);
        }
    }
    
    /**
     * Verifies the working directory specified by the given launch
     * configuration exists, and returns the working directory, or
     * <code>null</code> if none is specified.
     * 
     * @param configuration  launch configuration
     * @return the working directory specified by the given launch
     *         configuration, or <code>null</code> if none
     * @exception CoreException if unable to retrieve the attribute
     */
    private File verifyWorkingDirectory(ILaunchConfiguration configuration)
        throws CoreException
    {
        IPath path = getWorkingDirectoryPath(configuration);
        if (path == null) return null;
        
        if (path.isAbsolute())
        {
            File dir = new File(path.toOSString());
            if (dir.isDirectory()) return dir;
        }
        
        // If we get here, we assume that the entered path is workspace-relative.
        // This is true for paths that do not start with slash, but also for
        // paths that start with slash to which some variables may evaluate.
        // In any case, we try to locate the working directory in the workspace.

        IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if (res instanceof IContainer && res.exists())
            return res.getLocation().toFile();

        throw new CoreException(
            new Status(
                Status.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                Status.OK,
                MessageFormat.format(
                    "Working directory does not exist: {0}",
                    new String[] { path.toString() }),
                null
                ));
    }
}