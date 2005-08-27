/* $Id$ */
package org.epic.perleditor.editors.util;

import java.io.IOException;
import java.io.Reader;

public class StringReaderThread extends Thread
{
    private final Object lock = new Object();
    
    private Reader reader;
    private String result;
    private IOException exception;

    public StringReaderThread(String name)
    {
        super("EPIC:StringReader" + name);
        this.start();
    }
    
    public StringReaderThread()
    {
        this("");        
    }
        
    public void dispose() throws InterruptedException {
        this.interrupt();
        this.join();
    }

    public void read(Reader r)
    {
        synchronized (this.lock)
        {
            if (this.reader!=null)
                throw new RuntimeException("already in use");
                
            this.reader = r;
            this.result = null;
            this.exception = null;
            
            this.lock.notify();
        }
    }
    
    public String getResult() throws IOException, InterruptedException
    {
        synchronized (this.lock)
        {
            while (this.reader!=null)
                this.lock.wait();
            
            if (this.exception!=null)
                throw this.exception;
            if (this.result!=null)
                return this.result;
            
            throw new RuntimeException("no result");
        }
    }
    
    public void run()
    {
        try
        {
            while (!Thread.interrupted())
            {
                Reader r;
                synchronized (this.lock)
                {
                    while (this.reader==null)
                        this.lock.wait();
                    
                    r = this.reader;
                }
                
                StringBuffer sb = new StringBuffer();
                IOException e = null;
                char[] b = new char[1024];
                
                try
                {
                    while (true)
                    {
                        int i = r.read(b);
                        if (i<0) break;
                        sb.append(b, 0, i);
                    }
                }
                catch (IOException e2)
                {
                    sb.setLength(0);
                    e = e2;
                }

                synchronized (this.lock)
                {
                    this.reader = null;
                    this.result = sb.toString();
                    this.exception = e;
                    this.lock.notifyAll();
                }
            }
        }
        catch (InterruptedException e)
        {
            //everything is fine, and this thread will terminate
  				if (e.getMessage() == null) {
  				  System.out.println("Thread interrupted due to normal workflow in " +this.getClass().getName());
  				} else {
  				  e.printStackTrace();
  				}
        }
    }
}
