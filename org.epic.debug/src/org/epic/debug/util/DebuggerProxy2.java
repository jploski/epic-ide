package org.epic.debug.util;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.swt.widgets.Display;

public class DebuggerProxy2 extends PlatformObject
    implements IProcess, ITerminate, IStreamsProxy
{
	private final ILaunch launch;
    private boolean terminated;
    private DebugInProxy debugIn;
    private DebugOutProxy debugOut;

	public DebuggerProxy2(
        PrintWriter debugIn,
        BufferedReader debugOut,
        ILaunch launch)
    {
		this.launch = launch;
        this.debugIn = new DebugInProxy(debugIn);
        this.debugOut = new DebugOutProxy(debugOut);
		fireCreationEvent();
	}
    
    public PrintWriter getDebugIn()
    {
        return debugIn;
    }
    
    public BufferedReader getDebugOut()
    {
        return debugOut;
    }

	public String getLabel()
    {
		return "perl -d";
	}

	public ILaunch getLaunch()
    {
		return launch;
	}

	public IStreamsProxy getStreamsProxy()
    {
		return this;
	}
 
	public void setAttribute(String key, String value)
    {
		launch.setAttribute(key, value);
	}

	public String getAttribute(String key)
    {
		return launch.getAttribute(key);
	}

	public int getExitValue() throws DebugException
    {
		return 0;
	}

	public Object getAdapter(Class adapter)
    {
		if (adapter.equals(IProcess.class)) return this;

		if (adapter.equals(IDebugTarget.class))
        {
			ILaunch launch = getLaunch();
			IDebugTarget[] targets = launch.getDebugTargets();
            
			for (int i = 0; i < targets.length; i++)
				if (this.equals(targets[i].getProcess())) return targets[i];

			return null;
		}
		return super.getAdapter(adapter);
	}

	public boolean canTerminate()
    {
		return true;
	}

	public boolean isTerminated()
    {
        return terminated;
	}

	public void terminate() throws DebugException
    {
        fireTerminateEvent();
        terminated = true;
	}

	public IStreamMonitor getErrorStreamMonitor()
    {
		return debugIn.getStreamMonitor();
	}

	public IStreamMonitor getOutputStreamMonitor()
    {
		return debugOut.getStreamMonitor();
	}

	public void write(String input) throws IOException
    {
        // TODO
	}

	/**
	 * Fire a debug event marking the creation of this element.
	 */
	private void fireCreationEvent()
    {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	/**
	 * Fire a debug event
	 */
	private void fireEvent(DebugEvent event)
    {
		DebugPlugin manager = DebugPlugin.getDefault();
		if (manager != null) manager.fireDebugEventSet(new DebugEvent[] { event });
	}

	/**
	 * Fire a debug event marking the termination of this process.
	 */
	private void fireTerminateEvent()
    {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}
    
    private static class DebugInProxy extends PrintWriter implements IStreamMonitor
    {
        private static final String NL = System.getProperty("line.separator");

        private Set listeners;
        private final PrintWriter w;
        private final StringWriter buf;
        private final PrintWriter bw;
        
        public DebugInProxy(PrintWriter w)
        {
            super(w, true);
            this.w = w;
            listeners = new HashSet();
            buf = new StringWriter();
            bw = new PrintWriter(buf);
        }
        
        public void print(boolean b)
        {
            bw.print(b);
            w.print(b);
            fireAppended(String.valueOf(b));
        }
        
        public void print(char c)
        {
            bw.print(c);
            w.print(c);
            fireAppended(String.valueOf(c));
        }
        
        public void print(char[] s)
        {
            bw.print(s);
            w.print(s);
            fireAppended(new String(s));
        }
        
        public void print(double d)
        {
            bw.print(d);
            w.print(d);
            fireAppended(String.valueOf(d));
        }
        
        public void print(float f)
        {
            bw.print(f);
            w.print(f);
            fireAppended(String.valueOf(f));
        }
        
        public void print(int i)
        {
            bw.print(i);
            w.print(i);
            fireAppended(String.valueOf(i));
        }
        
        public void print(long l)
        {
            bw.print(l);
            w.print(l);
            fireAppended(String.valueOf(l));
        }
        
        public void print(Object obj)
        {
            bw.print(obj);
            w.print(obj);
            fireAppended(String.valueOf(obj));
        }
        
        public void print(String s)
        {
            bw.print(s);
            w.print(s);
            fireAppended(s);
        }
        
        public void println()
        {
            bw.println();
            w.println();
            fireAppended(NL);
        }
        
        public void println(boolean x)  { print(x); println(); }        
        public void println(char x)     { print(x); println(); }        
        public void println(char[] x)   { print(x); println(); }        
        public void println(double x)   { print(x); println(); }
        public void println(float x)    { print(x); println(); }
        public void println(int x)      { print(x); println(); }
        public void println(long x)     { print(x); println(); }        
        public void println(Object x)   { print(x); println(); }
        public void println(String x)   { print(x); println(); }
        
        public void write(char[] buf)
        {
            bw.write(buf);
            w.write(buf);
            fireAppended(new String(buf));
        }
        
        public void write(char[] buf, int off, int len)
        {
            bw.write(buf, off, len);
            w.write(buf, off, len);
            fireAppended(new String(buf, off, len));
        }
        
        public void write(int c)
        {
            bw.write(c);
            w.write(c);
            fireAppended(String.valueOf(c));
        }
        
        public void write(String s)
        {
            bw.write(s);
            w.write(s);
            fireAppended(s);
        }
        
        public void write(String s, int off, int len)
        {
            bw.write(s, off, len);
            w.write(s, off, len);
            fireAppended(s.substring(off, off+len));
        }

        public synchronized void addListener(IStreamListener listener)
        {
            listeners.add(listener);
        }

        public String getContents()
        {
            return buf.toString();
        }
        
        public IStreamMonitor getStreamMonitor()
        {
            return this;
        }

        public synchronized void removeListener(IStreamListener listener)
        {
            listeners.remove(listener);
        }
        
        private synchronized void fireAppended(String text)
        {
            for (Iterator i = listeners.iterator(); i.hasNext();)
            {
                final IStreamListener listener = (IStreamListener) i.next();
                listener.streamAppended(text, this);
            }
        }
    }
    
    private static class DebugOutProxy extends BufferedReader implements IStreamMonitor
    {
        private static final String NL = System.getProperty("line.separator");
        
        private Set listeners;
        private StringBuffer buf;
        
        public DebugOutProxy(Reader in)
        {
            super(in);
            buf = new StringBuffer();
            listeners = new HashSet();
        }
        
        public int read()
            throws IOException
        {
            int ret = super.read();
            if (ret >= 0)
            {
                buf.append((char) ret);
                fireAppended(String.valueOf((char) ret));
            }
            return ret;
        }
        
        public int read(char[] cbuf)
            throws IOException
        {
            return read(cbuf, 0, cbuf.length);
        }
        
        public int read(char[] cbuf, int off, int len)
            throws IOException
        {
            int ret = super.read(cbuf, off, len);
            if (ret > 0)
            {
                String str = new String(cbuf, off, ret);
                buf.append(str);
                fireAppended(str);
            }
            return ret;
        }
        
        public String readLine()
            throws IOException
        {
            String ret = super.readLine();
            if (ret != null)
            {                
                buf.append(ret);
                buf.append(NL);
                fireAppended(ret);
                fireAppended(NL);
            }
            return ret;
        }

        public synchronized void addListener(IStreamListener listener)
        {
            listeners.add(listener);
        }
        
        public String getContents()
        {
            return buf.toString();
        }
        
        public IStreamMonitor getStreamMonitor()
        {
            return this;
        }
        
        public synchronized void removeListener(IStreamListener listener)
        {
            listeners.remove(listener);
        }
        
        private synchronized void fireAppended(String text)
        {
            for (Iterator i = listeners.iterator(); i.hasNext();)
            {
                final IStreamListener listener = (IStreamListener) i.next();
                listener.streamAppended(text, this);
            }
        }
    }
}