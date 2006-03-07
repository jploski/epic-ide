package org.epic.debug;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IProcess;
import org.epic.core.PerlCore;
import org.epic.core.PerlProject;
import org.epic.core.util.PerlExecutableUtilities;
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
    private int mWebserverPort;
	private boolean mDebug;
	private boolean mShutDownStarted;
	private boolean mReConnect;
	private Process mBrazilProcess;
	private CGITarget mTarget;
	private CGIProxy mCGIProxy;
    private CGIBrowser mBrowser;

	public CGITarget(ILaunch launch)
	{
		super(launch);
		mDebug = launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE);
        mProcessName = mDebug ? "CGI Perl Debugger" : "CGI Perl";
		DebugPlugin.getDefault().addDebugEventListener(this);
	}
    
    public IPath getLocalWorkingDir()
        throws CoreException
    {
        String path = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR,
            false);
        
        assert path != null;        
        return new Path(path);
    }
    
    public IProcess getProcess()
    {
        return mCGIProxy;
    }
    
    public void handleDebugEvents(DebugEvent[] events)
    {
        for (int i = 0; i < events.length; i++)
        {
            if (events[i].getKind() == DebugEvent.TERMINATE &&
                (events[i].getSource() == mProcess ||
                 events[i].getSource() == mCGIProxy))
            {
                DebugPlugin.getDefault().asyncExec(new Runnable() {
                    public void run() { terminate(); } });
            }
        }
    }
    
    public boolean isTerminated()
    {
        if (mPerlDB == null) return !mReConnect;
        return mPerlDB.isTerminated(this) && !mReConnect;
    }
    
    public void shutdown(boolean unregister)
    {
        if (mShutDownStarted) return;
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

	public void start()
	{
		mReConnect = true;

		if (!startTarget()) terminate();
		if (mDebug)
		{
			if (!startSession()) terminate();
		}
	}

	public void terminate()
	{
		mReConnect = false;

		if (mBrazilProcess != null) mBrazilProcess.destroy();
		if (mBrowser != null) mBrowser.close();

		shutdown();
	}

	void debugSessionTerminated()
	{
		mTarget = this;
        mLaunch.removeDebugTarget(this);
        mReConnect = false;
        
		Thread term = new Thread("EPIC-Debugger:waitForDebuggerReconnect")
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
		fireChangeEvent();
	}

	void initPath()
	{
		mProjectDir = null;
		mStartupFile = null;
		mStartupFileAbsolut = null;
	}

    protected Process startPerlProcess()
    {
        return null;
    }

	boolean isLocal()
    {
		return true;
	}
    
    private BrazilProps createBrazilProps() throws CoreException
    {
        String htmlRootDir = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR, true);

        String cgiRootDir = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR, true);

        List cgiEnv = (List) mLaunch.getLaunchConfiguration().getAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_ENV,
            (List) null);
                    
        String cgiFileExtension = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_CGI_FILE_EXTENSION, false);

        String projectName = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME, false);
        
        PerlProject project = PerlCore.create(
            PerlDebugPlugin.getWorkspace().getRoot().getProject(projectName));

        String perlPath = PerlExecutableUtilities.getPerlInterpreterPath();
        if (perlPath == null) perlPath = ""; // TODO report an error?

        BrazilProps props = new BrazilProps();
        
        props.add("cgi.InPort", mCGIProxy.getInPort());
        props.add("cgi.OutPort", mCGIProxy.getOutPort());
        props.add("cgi.ErrorPort", mCGIProxy.getErrorPort());
        props.add("cgi.Debug", mDebug);
        props.add("root", htmlRootDir);
        props.add("port", mWebserverPort);
        props.add("cgi.root", cgiRootDir);
        props.add("cgi.executable", perlPath);
        props.add("cgi.suffix", cgiFileExtension);
        props.add("cgi.DebugInclude", " -I" + PerlDebugPlugin.getPlugInDir());
        props.add("cgi.RunInclude", PerlExecutableUtilities.getPerlIncArgs(project));

        if (mDebug) props.add("cgi.ENV_" + PerlDebugPlugin.getPerlDebugEnv(this));
        
        if (cgiEnv != null)
        {
            for (Iterator i = cgiEnv.iterator(); i.hasNext();)
                props.add("cgi.ENV_" + (String) i.next());
        }
        return props;    
    }
    
    /**
     * Fire a debug event marking the creation of this element.
     */
    private void fireCreationEvent(Object fSource)
    {
        fireEvent(new DebugEvent(fSource, DebugEvent.CREATE));
    }
    
    private String getLaunchAttribute(String attrName, boolean isPath)
        throws CoreException
    {
        String attrValue = mLaunch.getLaunchConfiguration().getAttribute(
            attrName, (String) null);
        
        if (attrValue == null) return null;        
        else return new Path(attrValue).toString();
    }
    
    private String getRelativeURL() throws CoreException
    {
        String htmlRootFile = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE, true);
        
        String htmlRootDir = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR, true);
        
        return
            new Path(htmlRootFile)
                .setDevice(null)
                .removeFirstSegments(new Path(htmlRootDir).segments().length)
                .toString();
    }
    
    private void startBrazil() throws IOException
    {
        String javaExec =
            getJavaHome() + File.separator + "bin" + File.separator + "java";
        File workingDir =
            PerlDebugPlugin.getDefault().getStateLocation().toFile();

        String[] cmdParams = {
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
        
        mBrazilProcess =
            Runtime.getRuntime().exec(cmdParams, null, workingDir);
    }

    private boolean startSession()
    {
        /* start debugger */
        if (connectDebugger(false) != RemotePort.mWaitOK) return false;
        return true;
    }
    
    private boolean startTarget()
    {
        if (mDebug)
        {
            mDebugPort = new RemotePort("CGITarget.mDebugPort");
            mDebugPort.startConnect();
        }

        mCGIProxy = new CGIProxy(mLaunch, "CGI-Process");
        mWebserverPort = RemotePort.findFreePort();

        try { createBrazilProps().save(); }
        catch (CoreException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not read launch configuration attributes.",
                e);
            return false;
        }
        catch (IOException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not create configuration file for web server.",
                e);
            return false;
        }
        
        try { startBrazil(); }
        catch (IOException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not start web server",
                e);
            return false;
        }

        mProcess = DebugPlugin.newProcess(mLaunch, mBrazilProcess, "WEB-Server");
        fireCreationEvent(mProcess);
        
        mCGIProxy.waitForConnect();
        if (!mCGIProxy.isConnected())
        {
            PerlDebugPlugin.getDefault().logError(
                "(CGI-Target) Could not connect to CGI-Proxy");
            return false;
        }
        mLaunch.addProcess(mCGIProxy);
        fireCreationEvent(mCGIProxy);
        
        try
        {
            mBrowser = new CGIBrowser(mLaunch, getRelativeURL(), mWebserverPort);
            mBrowser.open();
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not start web browser for CGI debugging.",
                e);
            return false;
        }
        return true;
    }
    
    private static class BrazilProps
    {
        private StringBuffer props;
        
        public BrazilProps()
        {
            props = new StringBuffer();                        
        }
        
        public void add(String name, boolean value)
        {
            add(name, String.valueOf(value));
        }
        
        public void add(String name, int value)
        {
            add(name, String.valueOf(value));
        }
        
        public void add(String name, List values)
        {
            int j = 0;
            for (Iterator i = values.iterator(); i.hasNext(); j++)
                add(name + "[" + j + "]", i.next().toString());
        }
        
        public void add(String value)
        {
            props.append(value);
            props.append('\n');
        }
        
        public void add(String name, String value)
        {
            props.append(name);
            props.append('=');
            props.append(value);
            props.append('\n');
        }
        
        public void save() throws IOException
        {
            File templ = new File(
                PerlDebugPlugin.getPlugInDir(),
                "brazil_cgi_templ.cfg");
            File dest = new File(
                PerlDebugPlugin.getDefault().getStateLocation().toString(),
                "brazil.cfg");
            
            InputStream in = null;
            OutputStream out = null;

            try
            {
                in = new FileInputStream(templ);
                out = new FileOutputStream(dest);
                
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                byte[] propsBytes = props.toString().getBytes();
                out.write(propsBytes, 0, propsBytes.length);
            }
            finally
            {
                if (in != null) try { in.close(); } catch (Exception e) { }
                if (out != null) try { out.close(); } catch (Exception e) { }
            }
        }
        
        public String toString()
        {
            return props.toString();
        }
    }
}