package org.epic.debug.util;

import java.io.*;
import java.net.*;

import org.epic.debug.PerlDebugPlugin;

public class RemotePort
{
    private static final int DEFAULT_LOW_PORT = 5000;
    private static final int DEFAULT_HIGH_PORT = 10000;
    
    public static final int WAIT_OK = 1;
    public static final int WAIT_TERMINATE = 2;
    public static final int WAIT_ERROR = 3;
    
    private ServerSocket mServer;
	private Socket mClient;
	private PrintWriter mWriter;
	private BufferedReader mReader;
	private OutputStream mOutStream;
	private InputStream mInStream;
    private String name; // for debugging only
    private int lastUsedPort;

	private Thread mConnectionThread;
	private volatile boolean mStop;

	private final int mStartPortSearch;
	private final int mEndPortSearch;

    public RemotePort(String name, int startPortSearch, int endPortSearch)
    {
        this.name = name;
        this.mStartPortSearch = startPortSearch;
        this.mEndPortSearch = endPortSearch;
    }
    
	public RemotePort(String name)
	{
        this(name, DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT);
    }
    
    public static int findFreePort() 
    {
        // TODO this method is ugly. Detecting unused port numbers like that
        // is suspect to race conditions, nothing but luck guarantees that the
        // port stays available long enough after return.

        boolean found = false;
        ServerSocket s = null;

        for (int i = DEFAULT_LOW_PORT; i <= DEFAULT_HIGH_PORT && !found; i++)
        {
            try
            {
                s = new ServerSocket(i);
                found = true;
            }
            catch (IOException e) { }
        }

        if (!found)
        {
            PerlDebugPlugin.log(
                new Exception("Couldn't not listen on server port: no free port available?"));
            return -1;
        }
        int port = s.getLocalPort();
        try { s.close(); } catch (IOException e) { PerlDebugPlugin.log(e); }
        return port;
    }
    
    public InputStream getInStream()
    {
        assert isConnected();

        if (mInStream == null)
        {
            try
            {
                mInStream = mClient.getInputStream();
            }
            catch (IOException e)
            {
                PerlDebugPlugin.log(e);
            }
        }
        return mInStream;
    }
    
    public OutputStream getOutStream()
    {
        assert isConnected();

        if (mOutStream == null)
        {
            try
            {
                mOutStream = new BufferedOutputStream(mClient.getOutputStream());
            }
            catch (IOException e)
            {
                PerlDebugPlugin.log(e);
            }
        }
        return mOutStream;
    }
    
    public BufferedReader getReadStream()
    {
        assert isConnected();

        if (mReader == null) createReader();
        return mReader;
    }
    
    public int getServerPort()
    {
        return mServer.getLocalPort();
    }
    
    public PrintWriter getWriteStream()
    {
        if (!isConnected()) return null;

        if (mWriter == null)
            mWriter = new PrintWriter(getOutStream(), false);

        return mWriter;
    }
    
    public boolean isConnected()
    {
        return mClient != null;
    }

	public void shutdown()
	{
        //System.err.println("*************** " + this + " shutdown");
        mServer = null;
        reset();
        mStop = true;
	}

    public boolean startConnect()
    {
        return startConnect(false);
    }
    
	public boolean startReconnect()
	{
		return startConnect(true);
	}

    public int waitForConnect(boolean fTimeOut)
    {
        return waitForConnect(fTimeOut, true);
    }

	public int waitForConnect(boolean fTimeOut, boolean shutdownOnTimeout)
	{
		int port =  mServer.getLocalPort();
		try
		{
			for (int x = 0;
				((x < 100) || (!fTimeOut)) && (mClient == null);
				++x)
			{
				if (mStop) break;

/*                if ((x % 10) == 0 && fTimeOut)
    				System.out.println(
    					"Waiting for connect Port"
    						+ port
    						+ "(Try "
    						+ x
    						+ " of 100)\n");*/
						
				Thread.sleep(100);
			}

			if (mClient == null)
			{
				if (shutdownOnTimeout) shutdown();
				if (mStop) return WAIT_TERMINATE;
				else return WAIT_ERROR;
			}

			mWriter = new PrintWriter(mClient.getOutputStream(), true);
			mReader = createReader();
		}
        catch (IOException e)
		{
			PerlDebugPlugin.log(e);
			return WAIT_ERROR;
		}
        catch (InterruptedException e)
		{
			PerlDebugPlugin.log(e);
			return WAIT_ERROR;
		}
		return WAIT_OK;
	}
    
    private BufferedReader createReader()
    {
        try
        {
            return new BufferedReader(
                new InputStreamReader(getInStream(), "UTF8"));
        }
        catch (UnsupportedEncodingException e)
        {
            PerlDebugPlugin.log(e); // impossible
            return new BufferedReader(new InputStreamReader(getInStream()));
        }
    }

    private void reset()
    {
        if (mReader != null)
        {
            try { mReader.close(); } catch (IOException e) { PerlDebugPlugin.log(e); }
            mReader = null;
        }
        if (mWriter != null)
        {
            mWriter.close();
            mWriter = null;
        }
        if (mClient != null)
        {
            try { mClient.close(); } catch (IOException e) { PerlDebugPlugin.log(e); }
            mClient = null;
        }
        
        mStop = false;
        mInStream = null;
        mOutStream = null;
        
        if (mConnectionThread != null)
        {
            mConnectionThread.interrupt();
            mConnectionThread = null;
        }
    }
    
    private boolean startConnect(boolean fReconnect)
    {
        //System.err.println("*************** " + this + " startConnect(" + fReconnect + ")");
        
        boolean found;
        
        if (fReconnect)
        {            
            reset();
            assert mServer != null; // we're still listening
            found = true;
        }
        else
        {
            reset();

            found = false;
            for (int i = mStartPortSearch; i <= mEndPortSearch && !found; i++)
            {
                try
                {
                    mServer = new ServerSocket(i);
                    mServer.setSoTimeout(5000);
                    lastUsedPort = i;
                    found = true;
                }
                catch (IOException e) { }
            }
        }
        
        if (!found)
        {
            PerlDebugPlugin.log(
                new Exception("Couldn't not listen on server port: no free port available?"));
            return false;
        }
        mClient = null;
        mConnectionThread = new Thread("EPIC-Debugger:RemotePort.startConnect")
        {
            public void run()
            {
                try
                {                    
                    ServerSocket server = mServer;
                    while (server != null && !isInterrupted())
                    {
                        try { mClient = server.accept(); }
                        catch (SocketTimeoutException e) { }
                    }
                    if (mServer == null)
                    {
                        server.close();
                    }
                }
                catch (IOException e)
                {
                    PerlDebugPlugin.log(e);
                }
            }
        };

        mConnectionThread.start();

        return true;
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("RemotePort[");
        buf.append(name);        
        if (mServer != null)
        {
            buf.append(':');
            buf.append(mServer.getLocalPort());
        }
        buf.append(']');
        return buf.toString();
    }
}
