package org.epic.debug.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.epic.debug.PerlDebugPlugin;

public class RemotePort
{
    public static final int mWaitOK = 1;
    public static final int mWaitTerminate = 2;
    public static final int mWaitError = 3;
    
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

	private static final int mStartPortSearch = 5000;
	private static final int mEndPortSearch = 10000;

	public RemotePort(String name)
	{
        this.name = name;
	}
    
    public static int findFreePort() 
    {
        // TODO this method is ugly. Detecting unused port numbers like that
        // is suspect to race conditions, nothing but luck guarantees that the
        // port stays available long enough after return.

        boolean found = false;
        ServerSocket s = null;

        for (int i = mStartPortSearch;(i < mEndPortSearch) && !found; i++)
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
                mOutStream = mClient.getOutputStream();
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

        if (mReader == null)
            mReader = new BufferedReader(new InputStreamReader(getInStream()));

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
            mWriter = new PrintWriter(getOutStream(), true);

        return mWriter;
    }
    
    public boolean isConnected()
    {
        return mClient != null;
    }

	public void shutdown()
	{
        //System.err.println("*************** " + this + " shutdown");
        reset();
        
        if (mServer != null)
        {
            try { mServer.close(); } catch (IOException e) { PerlDebugPlugin.log(e); }
            mServer = null;
        }
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
		int port =  mServer.getLocalPort();
		try
		{
			for (int x = 0;
				((x < 100) || (!fTimeOut)) && (mClient == null);
				++x)
			{
				if (mStop) break;

                if ((x % 10) == 0 && fTimeOut)
    				System.out.println(
    					"Waiting for connect Port"
    						+ port
    						+ "(Try "
    						+ x
    						+ " of 100)\n");
						
				Thread.sleep(100);
			}

			if (mClient == null)
			{
				shutdown();
				if (mStop) return mWaitTerminate;
				else return mWaitError;
			}

			mWriter = new PrintWriter(mClient.getOutputStream(), true);
			mReader = new BufferedReader(
                new InputStreamReader(mClient.getInputStream()));

		}
        catch (IOException e)
		{
			PerlDebugPlugin.log(e);
			return mWaitError;
		}
        catch (InterruptedException e)
		{
			PerlDebugPlugin.log(e);
			return mWaitError;
		}
		return mWaitOK;
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
        mConnectionThread = null;
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
/*        {
            reset();
            found = false;
            int count = 0, port = lastUsedPort;
            do
            {
                try
                {
                    mServer = new ServerSocket(port);
                    lastUsedPort = port;
                    found = true;
                    System.err.println("*****Reconnect ok***");
                }
                catch (IOException e)
                {
                    System.err.println("*****Reconnect failed***");
                    try
                    {
                        Thread.sleep(100);
                        count++;
                    } catch (InterruptedException e1)
                    {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    found = false;
                }
            }
            while (!found && count < 100);
        }*/
        else
        {
            reset();

            found = false;
            for (int i = mStartPortSearch; i < mEndPortSearch && !found; i++)
            {
                try
                {
                    mServer = new ServerSocket(i);
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
                int port = mServer.getLocalPort();
                
                try
                {
                    System.out.println(
                        name + ": Trying to Accept on Port" + mServer.getLocalPort());
                    mClient = mServer.accept();
                    System.out.println(
                        name + ": Accept on Port "
                            + mServer.getLocalPort()
                            + "!!!!!!!\n");
                } catch (IOException e)
                {
                    System.out.println(
                        name + ": Accept failed: " + port);
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
