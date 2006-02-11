/*
 * Created on 03.04.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug.util;

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

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
	public ServerSocket mServer;
	public Socket mClient;
	public PrintWriter mWriter;
	public BufferedReader mReader;
	public OutputStream mOutStream;
	public InputStream mInStream;
	//public int mServer.getLocalPort();
	private Thread mConnectionThread;
	private volatile boolean mStop;
	public static int mWaitOK = 1;
	public static int mWaitTerminate = 2;
	public static int mWaitError = 3;
	final static int mStartPortSearch = 5000;
	final static int mEndPortSearch = 10000;


	//	public RemotePort(int fPort)
	//	{
	//		reset();
	//		mServer.getLocalPort() = fPort;
	//
	//	}

	public RemotePort()
	{
		reset();
		//mServer.getLocalPort() = -1;

	}

	

	void reset()
	{
		shutdown();
		mStop = false;
		mServer = null;
		mClient = null;
		mWriter = null;
		mReader = null;
		mInStream = null;
		mOutStream = null;
		mConnectionThread = null;

	}
	public void shutdown()
	{
		try
		{
			mStop = true;
			if (mReader != null)
				mReader.close();
			if (mWriter != null)
				mWriter.close();
			if (mClient != null)
				mClient.close();
			if (mServer != null)
				mServer.close();
			//			if (mConnectionThread != null && mConnectionThread.isAlive())
			//			{
			//				mStop = true;
			//				mConnectionThread.join();
			//			}

		} catch (IOException e)
		{
			PerlDebugPlugin.log(e);
		}
	}

	

	public boolean startReconnect()
	{
		return (startConnect(true));
	}
	public boolean startConnect()
	{
		return (startConnect(false));
	}
	public boolean startConnect(boolean fReconnect)
	{
		boolean found;
		
		if (fReconnect)
		{
			int port = mServer.getLocalPort();
			reset();
			found = false;
			int count = 0;
			do{
			   	try
					{
						mServer = new ServerSocket(port);
						found = true;
						System.err.println("*****Reconnect ok***");
					} catch (IOException e)
					{
						System.err.println("*****Reconnect failed***");
						try
						{
							Thread.sleep(100);
						} catch (InterruptedException e1)
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						found = false;
					}
			}while( ! found && count < 100);
			
		} else
		{

			reset();

			found= false;
			for (int i = mStartPortSearch;(i < mEndPortSearch) && !found; i++)
			{
				found = true;
				try
				{
					mServer = new ServerSocket(i);
				} catch (IOException e)
				{
					found = false;
				}
			}
		}
			if (!found)
			{
				PerlDebugPlugin.log(
					new InstantiationException("Couldn't not listen on server port ! No free port available!"));
				return false;
			}
		
		mClient = null;

		mConnectionThread = new Thread()
		{
			public void run()
			{
				int port = mServer.getLocalPort();
				
				try
				{
					System.out.println(
						"Trying to Accept on Port" + mServer.getLocalPort());
					mClient = mServer.accept();
					System.out.println(
						"Accept on Port "
							+ mServer.getLocalPort()
							+ "!!!!!!!\n");
				} catch (IOException e)
				{
					System.out.println(
						"Accept failed: " + port);
				}
			}
		};

		mConnectionThread.start();

		return true;
	}

	public int waitForConnect(boolean fTimeOut)
	{
		int port =  mServer.getLocalPort();
		try
		{
			//synchronized (this)
			{
				for (int x = 0;
					((x < 1000) || (!fTimeOut)) && (mClient == null);
					++x)
				{
					if (mStop)
						break;
					System.out.println(
						"Waiting for connect Port"
							+ port
							+ "(Try "
							+ x
							+ " of 1000)\n");
							
					Thread.sleep(100);
					
				}
			}

			if (mClient == null)
			{
				shutdown();
				if (mStop)
					return (mWaitTerminate);
				else
					return mWaitError;
			}

			mWriter = new PrintWriter(mClient.getOutputStream(), true);
			mReader =
				new BufferedReader(
					new InputStreamReader(mClient.getInputStream()));
		} catch (IOException e)
		{
			PerlDebugPlugin.log(
				new InstantiationException("Failing establish Communication with Debug Process  !!!"));
			return mWaitError;
		} catch (InterruptedException e)
		{
			PerlDebugPlugin.log(
				new InstantiationException("Failing establish Communication with Debug Process  !!!"));
			return mWaitError;
		}

		return mWaitOK;
	}

	public boolean isConnected()
	{
		return (mClient != null);
	}

	public OutputStream getOutStream()
	{
		if (!isConnected())
			return null;

		if (mOutStream == null)
			try
			{
				mOutStream = mClient.getOutputStream();
			} catch (IOException e)
			{
				PerlDebugPlugin.log(e);
			}

		return mOutStream;
	}
	public PrintWriter getWriteStream()
	{
		if (!isConnected())
			return null;

		if (mWriter == null)
			mWriter = new PrintWriter(getOutStream(), true);

		return mWriter;
	}

	public InputStream getInStream()
	{
		if (!isConnected())
			return null;

		if (mInStream == null)
			try
			{
				mInStream = mClient.getInputStream();
			} catch (IOException e)
			{
				PerlDebugPlugin.log(e);
			}
		return mInStream;

	}

	public BufferedReader getReadStream()
	{
		if (!isConnected())
			return null;

		if (mReader == null)
			mReader = new BufferedReader(new InputStreamReader(getInStream()));

		return mReader;

	}

	public int getServerPort()
	{
		return mServer.getLocalPort();
	}
	
	public static int findFreePort() 
	{
		boolean found;
		ServerSocket s = null;
		
		found= false;
					for (int i = mStartPortSearch;(i < mEndPortSearch) && !found; i++)
					{
						found = true;
						try
						{
							s = new ServerSocket(i);
						} catch (IOException e)
						{
							found = false;
						}
					}

					if (!found)
					{
						PerlDebugPlugin.log(
							new InstantiationException("Couldn't not listen on server port ! No free port available!"));
						return -1;
					}
		int port = 	s.getLocalPort();
		try
		{
			s.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return port;
	}
	
	
}
