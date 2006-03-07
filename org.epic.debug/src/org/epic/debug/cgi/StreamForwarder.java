package org.epic.debug.cgi;

import java.io.*;

/**
 * A thread which forwards all bytes read from a source InputStream
 * to a destination OutputStream. After the thread terminates,
 * {@link getError} can be called to check if the the InputStream
 * was closed properly or an exception has occured.
 */
class StreamForwarder extends Thread
{
    private final InputStream src;
    private final OutputStream dst;
    private IOException error;
    
    public StreamForwarder(String name, InputStream src, OutputStream dst)
    {
        super(name);
        this.src = src;
        this.dst = dst;
    }
    
    public IOException getError()
    {
        return error;
    }
    
    public void run()
    {
        byte[] buf = new byte[1024];
        try
        {
            int bread;
            while ((bread = src.read(buf, 0, buf.length)) > 0)
            {
                dst.write(buf, 0, bread);                            
                dst.flush();
            }
        }
        catch (IOException e)
        {
            error = e;
        }
    }
}