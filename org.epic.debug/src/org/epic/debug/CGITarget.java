package org.epic.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.browser.BrowserDescriptor;
import org.eclipse.help.internal.browser.BrowserManager;
import org.epic.debug.cgi.CustomBrowser;
import org.epic.debug.util.CGIProxy;
import org.epic.debug.util.RemotePort;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CGITarget extends DebugTarget implements IDebugEventSetListener
{

	private boolean mShutDownStarted;

	private boolean mReConnect;

	private Process mBrazilProcess;
	private CGITarget mTarget;
	private IBrowser mBrowser;
	private CGIProxy mCGIProxy;
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
		mProcessName = "CGI Perl Debugger";
		DebugPlugin.getDefault().addDebugEventListener(this);

	}

	public void start()
	{
		mReConnect = true;

		if (!startTarget())
			terminate();

		if (!startSession())
			terminate();
	}

	boolean startTarget()
	{

		String htmlRootDir = null;
		String htmlRootFile = null;
		String cgiRootDir = null;
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

		} catch (CoreException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		Path htmlDirPath = new Path(htmlRootDir);
		htmlRootDir = htmlDirPath.toString();

		Path htmlFilePath = new Path(htmlRootFile);
		htmlRootFile = htmlFilePath.toString();

		Path cgiDirPath = new Path(cgiRootDir);
		cgiRootDir = cgiDirPath.toString();

		String htmlRootFileRel =
			htmlFilePath
				.setDevice(null)
				.removeFirstSegments(htmlDirPath.segments().length)
				.toString();

		/* start web-server*/
		/* create config file*/
		String brazilProps =
			"root="
				+ htmlRootDir
				+ "\n"
				+ "cgi.root="
				+ cgiRootDir
				+ "\n"
				+ "file.default="
				+ htmlRootFileRel
				+ "\n"
				+ "cgi.ENV_"
				+ PerlDebugPlugin.getPerlDebugEnv(mLaunch)
				+ "\n"
				+ "cgi.executable="
				+ PerlExecutableUtilities.getPerlExecutableCommandLine().get(0);
		File templ =
			new File(getPlugInDir() + File.separator + "brazil_cgi_templ.cfg");
		File dest =
			new File(
				PerlDebugPlugin.getDefault().getStateLocation()
					+ File.separator
					+ "brazil.cfg");

		try
		{
			copy(templ, dest, brazilProps);
		} catch (IOException e)
		{
			e.printStackTrace();
			PerlDebugPlugin.getDefault().logError(
				"Could not create configuration file for WEB server",
				e);
			return false;
		}

		mCGIProxy = new CGIProxy(mLaunch, "CGI-Process");

		// Brazil command line parameters
		String javaExec =
			getJavaHome() + File.separator + "bin" + File.separator + "java";
		File workingDir =
			PerlDebugPlugin.getDefault().getStateLocation().toFile();

		String[] cmdParams =
			{
				javaExec,
				"-classpath",
				getPlugInDir()
					+ "brazil_mini.jar"
					+ File.pathSeparator
					+ getPlugInDir()
					+ "debug.jar"
					+ File.pathSeparator
					+ getPlugInDir()
					+ "bin",
				"sunlabs.brazil.server.Main",
				"-c",
				"brazil.cfg" };

		try
		{
			String params = " ";
			for (int x = 0; x < cmdParams.length; ++x)
				params = params + " " + cmdParams[x];

			PerlDebugPlugin.getDefault().logError(
				"CMDline:" + params + "\n" + workingDir);
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
		

		startBrowser();
		/* start console-proxy*/
		return true;

	}
	/**
		 * Fire a debug event marking the creation of this element.
		 */
	private void fireCreationEvent(Object fSource)
	{
		fireEvent(new DebugEvent(fSource, DebugEvent.CREATE));
	}

	/**
	 * Fire a debug event
	 */
	private void fireEvent(DebugEvent event)
	{
		DebugPlugin manager = DebugPlugin.getDefault();
		if (manager != null)
		{
			manager.fireDebugEventSet(new DebugEvent[] { event });
		}
	}

	void startBrowser()
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
		BrowserDescriptor[] browserDescr =
			BrowserManager.getInstance().getBrowserDescriptors();
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
			mBrowser.displayURL("http://localhost:8080/");
		} catch (Exception e)
		{
			PerlDebugPlugin.getDefault().logError(
				"Could not start browser for CGI debugging",
				e);
		}

	}

	boolean startSession()
	{
		/* start debugger*/
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

		if (mRemotePort != null)
			mRemotePort.shutdown();

		//	 mTarget = new CGITarget(mLaunch);
		//	 mTarget.mProcessName ="New";

		mTarget = this;

		Thread term = new Thread()
		{
			public void run()
			{
				mTarget.startSession();
				mLaunch.addDebugTarget(mTarget);
				((DebugTarget) mTarget).getDebuger().generateDebugInitEvent();
				getDebuger().generateDebugInitEvent();
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

		try
		{
			mWorkingDir =
				new Path(
					mLaunch.getLaunchConfiguration().getAttribute(
						PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
						(String) null));
		} catch (CoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mStartupFile = null;

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
}