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
	public ServerSocket 	mServer;
	public Socket			mClient;
	public PrintWriter 		mWriter;
	public BufferedReader	mReader;
	public OutputStream 	mOutStream;
	public InputStream 		mInStream;
	public int 				mPort;
		
		
	public RemotePort(int fPort)
	{
		reset();
		mPort = fPort;
		
	}
		
	void reset()
	{
		shutdown();
		mServer = null;
		mClient = null;
		mWriter = null;
		mReader = null;
		mInStream = null;
		mOutStream = null;
	}
	public void shutdown()
	{
		try {
		if (mReader != null)
			mReader.close();
		if (mWriter != null)
			mWriter.close();
		if (mClient != null)
			mClient.close();
		if (mServer != null)
			mServer.close();
		} catch (IOException e) {PerlDebugPlugin.log(e); }
	}

	public boolean startConnect()
	{
		
		
		reset();
		
		try {
			 mServer = new ServerSocket(mPort);
			} catch (IOException e) {
				PerlDebugPlugin.log( new InstantiationException("Couldn't listen to Port"+mPort));
				return false;
				}
		
		mClient = null;
				
		Thread connect = 
			new Thread() 
			{ 
				public void run()
				{
					try {
							System.out.println("Trying to Accept on Port"+mPort);
							mClient = mServer.accept();
							System.out.println("Accept on Port "+mPort+"!!!!!!!\n");
						} catch (IOException e) 
							{System.out.println("Accept failed: "+mPort);}
				}
			};
				
		connect.start();
								 
		return true;
	} 
		
		
	public boolean waitForConnect(boolean fTimeOut)
	{
		
		
			
		try{			
				synchronized(this)
				{
				for( int x=0; ((x < 1000) || fTimeOut) && (mClient == null) ; ++x)
				{
					System.out.println("Waiting for connect Port"+mPort+"(Try "+x+" of 100)\n");
						 wait(100);
				}
				}
	
				if( mClient == null)
				{ 
					shutdown();
					return false;		
				}
		
				mWriter = new PrintWriter(mClient.getOutputStream(), true);
				mReader = new BufferedReader(
						new InputStreamReader( mClient.getInputStream())
						);
				}catch(IOException e){PerlDebugPlugin.log( new InstantiationException("Failing establish Communication with Debug Process  !!!")); return false;}
				 catch(InterruptedException e){PerlDebugPlugin.log( new InstantiationException("Failing establish Communication with Debug Process  !!!")); return false;}
					 
		return true;
	} 

		
	public boolean isConnected()
	{
		return( mClient != null);
	}
		
		
	public OutputStream getOutStream()
		{
			if( ! isConnected() )
				return null;
				
			if( mOutStream == null)
			  try {
				mOutStream = mClient.getOutputStream();
			} catch (IOException e) {
				PerlDebugPlugin.log(e);
			}
			  
			return mOutStream;
	}
	public PrintWriter getWriteStream()
	{
		if( ! isConnected() )
			return null;
				
		if( mWriter == null)
	
		 mWriter = new PrintWriter(getOutStream(), true);
			  
		return mWriter;
}

public InputStream getInStream()
		{
			if( ! isConnected() )
				return null;
				
			if( mInStream == null)
			  try {
				mInStream = mClient.getInputStream();
			} catch (IOException e) {
				PerlDebugPlugin.log(e);
			}
			return mInStream;
				
	}
	
public BufferedReader getReadStream()
		{
			if( ! isConnected() )
				return null;
				
			if( mReader == null)
			  mReader = new BufferedReader(new InputStreamReader(getInStream()));
			
			return mReader;
				
	}

	
}	
