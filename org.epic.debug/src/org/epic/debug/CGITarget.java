package org.epic.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.browser.BrowserDescriptor;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.epic.core.PerlCore;
import org.epic.core.util.PerlExecutableUtilities;
import org.epic.core.views.browser.BrowserView;
import org.epic.debug.cgi.CustomBrowser;
import org.epic.debug.util.CGIProxy;
import org.epic.debug.util.RemotePort;

/**
 * @author ruehl
 * 
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public class CGITarget extends DebugTarget implements IDebugEventSetListener
{
	private int mWeberverPort;
	
	private boolean mDebug;

	private ArrayList cgiEnv;

	private boolean mShutDownStarted;

	private boolean mReConnect;

	private Process mBrazilProcess;
	private CGITarget mTarget;
	private IBrowser mBrowser;
	private CGIProxy mCGIProxy;
	private String mHtmlRootFileRel ;
	/**
	 * Constructor for DebugTarget.
	 */
	public CGITarget()
	{
		super();
	}

	/**
	 * Constructor for DebugTarget.
	 */
	public CGITarget(ILaunch launch)
	{
		super(launch);
		mDebug = launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE);
		if (mDebug)
			mProcessName = "CGI Perl Debugger";
		else
			mProcessName = "CGI Perl";
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	public void start()
	{
		mReConnect = true;

		if (!startTarget())
			terminate();

		if (mDebug)
		{

			if (!startSession())
				terminate();
		}
	}

	boolean startTarget()
	{

		String htmlRootDir = null;
		String htmlRootFile = null;
		String cgiRootDir = null;
		String cgiFileExtension = null;
		String projectName = null;
		IProject project;
		
		if (mDebug)
		{
			mDebugPort = new RemotePort();
			mDebugPort.startConnect();
			if (mDebugPort == null)
				return false;
		}
		try
		{
			htmlRootDir =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR,
					(String) null);

			htmlRootFile =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE,
					(String) null);

			cgiRootDir =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
					(String) null);

			cgiEnv =
				(ArrayList) mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_CGI_ENV,
					(List) null);
					
			cgiFileExtension =
							mLaunch.getLaunchConfiguration().getAttribute(
								PerlLaunchConfigurationConstants.ATTR_CGI_FILE_EXTENSION,
								(String) null);					
			projectName =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					(String) null);		
			
		} catch (CoreException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		project = PerlDebugPlugin.getWorkspace().getRoot().getProject(projectName);
		Path htmlDirPath = new Path(htmlRootDir);
		htmlRootDir = htmlDirPath.toString();

		Path htmlFilePath = new Path(htmlRootFile);
		htmlRootFile = htmlFilePath.toString();

		Path cgiDirPath = new Path(cgiRootDir);
		cgiRootDir = cgiDirPath.toString();

		 mHtmlRootFileRel =
			htmlFilePath
				.setDevice(null)
				.removeFirstSegments(htmlDirPath.segments().length)
				.toString();

		StringBuffer brazilProps = new StringBuffer();
		mCGIProxy = new CGIProxy(mLaunch, "CGI-Process", brazilProps);
		int webServerPort = RemotePort.findFreePort();

		/* start web-server */
		/* create config file */

        String perlPath = PerlExecutableUtilities.getPerlInterpreterPath();
        if (perlPath == null) perlPath = ""; // TODO report an error?
        
		brazilProps.append(
			"\ncgi.Debug="
				+ mDebug
				+ "\n"
				+ "root="
				+ htmlRootDir
				+ "\n"
				+ "port="
				+ webServerPort
				+ "\n"
				+ "cgi.root="
				+ cgiRootDir
				+ "\n"
//				+ "file.default="
//				+ htmlRootFileRel
//				+ "\n"
				+ "cgi.executable="
				+ perlPath
				+ "\n"
				+ "cgi.suffix="+cgiFileExtension
				+"\n"
				+ "cgi.DebugInclude="+" -I"+PerlDebugPlugin.getPlugInDir());
		
		List list = PerlExecutableUtilities.getPerlIncArgs(PerlCore.create(project));
		Iterator i = list.iterator();
		int x= 0;
		while(i.hasNext())
		{
			brazilProps.append("\ncgi.RunInclude["+x+"]="+i.next());
			++x;
		}	
		if (mDebug)
		{

			brazilProps.append(
				"\n" + "cgi.ENV_" + PerlDebugPlugin.getPerlDebugEnv(this));
		}
		
		if (cgiEnv != null)
			for (Iterator iter = cgiEnv.iterator(); iter.hasNext();)
			{
				String element = (String) iter.next();
				brazilProps.append("\n" + "cgi.ENV_" + element);
			}

		File templ =
			new File(PerlDebugPlugin.getPlugInDir() + File.separator + "brazil_cgi_templ.cfg");
		File dest =
			new File(
				PerlDebugPlugin.getDefault().getStateLocation()
					+ File.separator
					+ "brazil.cfg");

		try
		{
			copy(templ, dest, brazilProps.toString());
		} catch (IOException e)
		{
			e.printStackTrace();
			PerlDebugPlugin.getDefault().logError(
				"Could not create configuration file for WEB server",
				e);
			return false;
		}

		// Brazil command line parameters
		String javaExec =
			getJavaHome() + File.separator + "bin" + File.separator + "java";
		File workingDir =
			PerlDebugPlugin.getDefault().getStateLocation().toFile();

		String[] cmdParams =
			{
				javaExec,
				"-classpath",
				PerlDebugPlugin.getPlugInDir()
					+ "brazil_mini.jar"
					+ File.pathSeparator
					+ PerlDebugPlugin.getPlugInDir()
					+ "debug.jar"
					+ File.pathSeparator
					+ PerlDebugPlugin.getPlugInDir()
					+ "bin",
				"sunlabs.brazil.server.Main",
				"-c",
				"brazil.cfg" };

		try
		{
			//			String params = " ";
			//			for (int x = 0; x < cmdParams.length; ++x)
			//				params = params + " " + cmdParams[x];
			//
			//			PerlDebugPlugin.getDefault().logError(
			//				"CMDline:" + params + "\n" + workingDir);
			//Startup Brazil
			mBrazilProcess =
				Runtime.getRuntime().exec(cmdParams, null, workingDir);
		} catch (IOException e1)
		{
			// TODO Auto-generated catch block
			PerlDebugPlugin.getDefault().logError(
				"Could not start WEB server",
				e1);
			return false;
		}

		mProcess =
			DebugPlugin.newProcess(mLaunch, mBrazilProcess, "WEB-Server");
		fireCreationEvent(mProcess);
		mCGIProxy.waitForConnect();

		System.out.println(mCGIProxy.isConnected());
		if (!mCGIProxy.isConnected())
		{
			PerlDebugPlugin.getDefault().logError(
				"(CGI-Target)Could not connect to CGI-Proxy");
			return (false);
		}

		mLaunch.addProcess(mCGIProxy);
		fireCreationEvent(mCGIProxy);
		
		mWeberverPort = webServerPort;
		DebugPlugin.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						startBrowser(mWeberverPort, mHtmlRootFileRel);
					}
				});
		
		/* start console-proxy */
		return true;

	}
	/**
	 * Fire a debug event marking the creation of this element.
	 */
	private void fireCreationEvent(Object fSource)
	{
		fireEvent(new DebugEvent(fSource, DebugEvent.CREATE));
	}

	

	BrowserDescriptor[] browserDescr ;
	void startBrowser(int fPort, String  htmlRootFileRel)
	{
		String browserID = null;
		String browserPath = null;
		try
		{
			browserID =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_BROWSER_ID,
					(String) null);

			browserPath =
				mLaunch.getLaunchConfiguration().getAttribute(
					PerlLaunchConfigurationConstants.ATTR_CUSTOM_BROWSER_PATH,
					(String) null);
		} catch (CoreException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if( browserID.equals(BrowserView.ID_BROWSER) )
		{
			//show view
			Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();
			if (shell != null) {
				shell.getDisplay().syncExec(new Runnable() {
					public void run() {
						BrowserView view = null;
						IWorkbenchPage activePage = PerlDebugPlugin
								.getWorkbenchWindow().getActivePage();
						try {
							view = (BrowserView) activePage
									.showView(BrowserView.ID_BROWSER);
						} catch (PartInitException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						view.setUrl("http://localhost:" + mWeberverPort + "/");
						}

				});

			}
			return;
		}
		Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();
		if (shell != null) {
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
			
		browserDescr =
			BrowserManager.getInstance().getBrowserDescriptors();
				}});}
			
		BrowserDescriptor descr;

		if (CustomBrowser.isCustomBrowserID(browserID))
		{

			mBrowser = new CustomBrowser(browserPath);
		} else
		{

			for (int i = 0; i < browserDescr.length; i++)
			{
				descr = browserDescr[i];
				if (descr.getID().equals(browserID))
				{
					mBrowser = descr.getFactory().createBrowser();
				}
			}

		}

		try
		{
			mBrowser.displayURL("http://localhost:" + fPort + "/"+ htmlRootFileRel);
		} catch (Exception e)
		{
			PerlDebugPlugin.getDefault().logError(
				"Could not start browser for CGI debugging",
				e);
		}

	}

	boolean startSession()
	{
		/* start debugger */
		if (connectDebugger(false) != RemotePort.mWaitOK)
			return false;
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated()
	{
		if (mPerlDB == null)
			return (!mReConnect);

		return mPerlDB.isTerminated(this) && !mReConnect;
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate()
	{
		mReConnect = false;
		// Terminate Brazil process
		if (mBrazilProcess != null)
		{
			mBrazilProcess.destroy();
		}

		if (mBrowser != null)
			mBrowser.close();

		shutdown();
	}

	Process startPerlProcess()
	{
		return null;
	}

	void debugSessionTerminated()
	{

		if (mDebugPort != null)
			mDebugPort.shutdown();

		//	 mTarget = new CGITarget(mLaunch);
		//	 mTarget.mProcessName ="New";

		mTarget = this;

		Thread term = new Thread()
		{
			public void run()
			{
				mTarget.startSession();
				if( mTarget.mPerlDB.isTerminated())
				{ 
					mTarget.mPerlDB = null;
					return;
				}
				mLaunch.addDebugTarget(mTarget);
				((DebugTarget) mTarget).getDebugger().generateDebugInitEvent();
				getDebugger().generateDebugInitEvent();
			}
		};

		term.start();

		mLaunch.removeDebugTarget(this);
		//	mTarget.fireCreateEvent();

		//	fireTerminateEvent();
		//	fireTerminateEvent();
		mReConnect = false;

		fireChangeEvent();

		//((DebugTarget) mTarget).getDebuger().generateDebugInitEvent();
		return;
	}

	// Copies src file to dst file.
	// If the dst file does not exist, it is created
	void copy(File src, File dst, String fAppend) throws IOException
	{
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
		out.write(fAppend.getBytes(), 0, fAppend.length());
		in.close();
		out.close();
	}

	void initPath()
	{
		mProjectDir = null;
		mStartupFile = null;
		mStartupFileAbsolut = null;
	}
    
    public IPath getLocalWorkingDir()
        throws CoreException
    {
        String path = mLaunch.getLaunchConfiguration().getAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
            (String) null);
        
        assert path != null;        
        return new Path(path);
    }

	public void shutdown(boolean unregister)
	{
		if (mShutDownStarted)
			return;
		mReConnect = false;
		mShutDownStarted = true;

		super.shutdown(unregister);
		try
		{
			mCGIProxy.terminate();
		} catch (DebugException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DebugPlugin.getDefault().removeDebugEventListener(this);
	}

	public void handleDebugEvents(DebugEvent[] events)
	{
		for (int i = 0; i < events.length; i++)
		{
			if (events[i].getKind() == DebugEvent.TERMINATE)
				if (events[i].getSource() == mProcess
					|| events[i].getSource() == mCGIProxy)
					DebugPlugin.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						terminate();
					}
				});
		}
	}

	public IProcess getProcess()
	{
		return mCGIProxy;
	}

	/* (non-Javadoc)
	 * @see org.epic.debug.Target#isLocal()
	 */
	boolean isLocal() {
		
		return true;
	}
	
	
}