/* $Id$ */
package org.epic.perleditor.editors.util;

import java.io.IOException;
import java.io.Reader;

public class StringReaderThread extends Thread
{
    private final Object lock = new Object();
    
    private Reader r;
    private String s;
    private IOException e;
    
    public StringReaderThread()
    {
        super();
        this.start();
    }
        
    public void dispose() throws InterruptedException {
        this.interrupt();
        this.join();
    }

    public void read(Reader r)
    {
        synchronized (this.lock)
        {
            if (this.r!=null)
                throw new RuntimeException("already in use");
                
            this.r = r;
            this.s = null;
            this.e = null;
            
            this.lock.notify();
        }
    }
    
    public String getResult() throws IOException, InterruptedException
    {
        synchronized (this.lock)
        {
            while (this.r!=null)
                this.lock.wait();
            
            if (this.e!=null)
                throw this.e;
            if (this.s!=null)
                return this.s;
            
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
                    while (this.r==null)
                        this.lock.wait();
                    
                    r = this.r;
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
                    this.r = null;
                    this.s = sb.toString();
                    this.e = e;                    
                    this.lock.notifyAll();
                }
            }
        }
        catch (InterruptedException e)
        {
            //everything is fine, and this thread will terminate
            e.printStackTrace();
        }
    }
}
