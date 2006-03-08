// NOTE: this is org.epic.core.util.StringReaderThread, duplicated here to
// work around classpath issues (EpicCGIHandler has no access to
// org.epic.perleditor code). TODO: think up a better solution.

package org.epic.debug.cgi;

import java.io.IOException;
import java.io.Reader;

/**
 * Asynchronously reads String content from a specified Reader.
 * This is mostly useful for interprocess communication, when output
 * from a subprocess must be consumed while delivering input data to it
 * in order to avoid blocking due to full IO buffers.
 */
class StringReaderThread extends Thread
{
    private final Object lock = new Object();
    
    private Reader reader;
    private String result;
    private IOException exception;
    private boolean disposed;

    public StringReaderThread(String name)
    {
        super("EPIC:StringReader" + name);
        this.start();
    }
    
    public StringReaderThread()
    {
        this("");        
    }

    /**
     * Terminates this StringReaderThread.
     * If any invocations of {@link #getResult} are in progress,
     * they will throw an InterruptedException.
     *
     * @exception java.lang.InterruptedException
     *            if the <code>dispose</code> operation is interrupted
     *            while waiting for thread's termination
     */
    public void dispose() throws InterruptedException
    {
        synchronized (lock)
        {
            disposed = true;
            lock.notifyAll();
        }
        join();
    }

    /**
     * Starts reading from the specified source.
     * Consecutive invocations of this method are disallowed.
     * The next method invocation after <code>read</code> must
     * be either {@link #getResult} or {@link #dispose}.
     */
    public void read(Reader r)
    {
        synchronized (lock)
        {
            if (disposed) throw new IllegalStateException("thread disposed");
            if (reader != null) throw new IllegalStateException(
                "this thread is already reading; call getResult or dispose next");
                
            reader = r;
            result = null;
            exception = null;
            
            lock.notifyAll();
        }
    }
    
    /**
     * Blocks until the reading is finished or aborted.
     * After this method returns, another {@link #read} may be started
     * (possibly from a different source).
     * 
     * @return the content which has been read, if reading finished successfully
     * @exception java.lang.InterruptedException
     *            if this StringReaderThread has been disposed
     * @exception java.io.IOException
     *            if an exception occured during the reading
     */
    public String getResult() throws IOException, InterruptedException
    {
        synchronized (lock)
        {
            while (reader != null && !disposed)
            {
                try { lock.wait(); }
                catch (InterruptedException e)
                {
                    /* should not happen, we don't care if it does */
                }
            }
            if (disposed) throw new InterruptedException(
                "StringReaderThread disposed during getResult");
            if (exception != null) throw exception;
            return result;
        }
    }
    
    public void run()
    {
        while (!disposed)
        {
            // wait for read() to be invoked
            Reader r;
            synchronized (lock)
            {
                while (reader == null && !disposed)
                {
                    try { lock.wait(); }
                    catch (InterruptedException e)
                    {
                        /* should not happen, we don't care if it does */
                    }
                }
                if (disposed) break;
                r = reader;
            }
            
            // process read() request
            StringBuffer sb = new StringBuffer();
            IOException e = null;
            char[] b = new char[1024];
            
            try
            {
                int bread;
                while ((bread = r.read(b)) >= 0) sb.append(b, 0, bread);
            }
            catch (IOException e2)
            {
                sb.setLength(0);
                e = e2;
            }

            synchronized (lock)
            {
                reader = null;
                result = sb.toString();
                exception = e;
                lock.notifyAll();
            }
        }
    }
}
