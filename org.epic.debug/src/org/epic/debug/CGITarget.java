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

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.browser.BrowserManager;
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
public class CGITarget extends DebugTarget
{

	private boolean mReConnect;

	private Process mBrazilProcess;
	private CGITarget mTarget;
	private IBrowser mBrowser;
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
		mProcessName="CGI Perl Debugger";
		

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
		/* start web-server*/
		/* create config file*/
		String brazilProps =
		"root=" +this.getWorkingDir().toString() + "\n" +
		"file.default=" + getStartupFile()+"\n" +
			"cgi.ENV_"
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
			for(int x= 0; x < cmdParams.length; ++x )
				params = params + " "+ cmdParams[x];
				
			PerlDebugPlugin.getDefault().logError("CMDline:"+params+"\n"+workingDir);
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
		mProcess = DebugPlugin.newProcess(mLaunch, mBrazilProcess, "WEB-Server");
		
		
		mBrowser = BrowserManager.getInstance().createBrowser();
		

	
			try
			{
				mBrowser.displayURL("http://localhost:8080/");
			} catch (Exception e)
			{
				PerlDebugPlugin.getDefault().logError("Could not start browser for CGI debugging", e);
			}
		/* start console-proxy*/
		return true;

	}

	boolean startSession()
	{
		/* start debugger*/
		if (!connectDebugger(false))
			return false;
			return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated()
	{
		if( mPerlDB == null) return(false);
		
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
		if( mBrowser != null )
			mBrowser.close();
			
		shutdown();
	}

	public void shutdown(boolean unregister)
	{
		if (!mPerlDB.isTerminated())
			mPerlDB.shutdown();
		mRemotePort.shutdown();

		super.shutdown(unregister);
	}

	Process startPerlProcess()
	{
		return null;
	}

	void debugSessionTerminated()
	{
			
		if( mRemotePort != null )
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

}