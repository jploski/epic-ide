/*
 * Created on 03.04.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug.util;

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;

/**
 * Monitors the output stream of a system process and notifies listeners of
 * additions to the stream.
 * 
 * The output stream monitor reads system out (or err) via and input stream.
 */
public class OutputStreamMonitor implements IFlushableStreamMonitor {
    /**
     * The stream being monitored (connected system out or err).
     */
    private InputStream fStream;

    /**
     * A collection of listeners
     */
    private ListenerList fListeners = new ListenerList(1);

    /**
     * Whether content is being buffered
     */
    private boolean fBuffered = true;

    /**
     * The local copy of the stream contents
     */
    private StringBuilder fContents;

    /**
     * The thread which reads from the stream
     */
    private Thread fThread;

    /**
     * The size of the read buffer
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Whether or not this monitor has been killed. When the monitor is killed,
     * it stops reading from the stream immediately.
     */
    private boolean fKilled = false;

    /**
     * Creates an output stream monitor on the given stream (connected to system
     * out or err).
     */
    public OutputStreamMonitor(InputStream stream) {
        fStream = stream;
        fContents = new StringBuilder();
    }
    public OutputStreamMonitor() {
        fContents = new StringBuilder();
    }

    public void setStream(InputStream stream) {
        fStream = stream;
    }
    /**
     * @see IStreamMonitor#addListener(IStreamListener)
     */
    public void addListener(IStreamListener listener) {
        fListeners.add(listener);
    }

    /**
     * Causes the monitor to close all communications between it and the
     * underlying stream by waiting for the thread to terminate.
     */
    protected void close() {
        if (fThread != null) {
            Thread thread = fThread;
            fThread = null;
            try {
                thread.join();
            } catch (InterruptedException ie) {
            }
            fListeners.removeAll();
        }
    }

    /**
     * Notifies the listeners that text has been appended to the stream.
     */
    private void fireStreamAppended(String text) {
        getNotifier().notifyAppend(text);
    }

    /**
     * @see IStreamMonitor#getContents()
     */
    public String getContents() {
        return fContents.toString();
    }

    /**
     * Continually reads from the stream.
     * <p>
     * This method, along with the <code>startReading</code> method is used to
     * allow <code>OutputStreamMonitor</code> to implement
     * <code>Runnable</code> without publicly exposing a <code>run</code>
     * method.
     */
    private void read() {
        byte[] bytes = new byte[BUFFER_SIZE];
        int read = 0;
        while (read >= 0) {
            try {
                if (fKilled) {
                    break;
                }
                read = fStream.read(bytes);
                if (read > 0) {
                    String text = new String(bytes, 0, read);
                    if (isBuffered()) {
                        fContents.append(text);
                    }
                    fireStreamAppended(text);
                }
            } catch (IOException ioe) {
                //DebugPlugin.log(ioe);
                return;
            } catch (Exception e) {
                // killing the stream monitor while reading can cause an NPE
                // when reading from the stream
                //				if (!fKilled || fThread != null) {
                //					DebugPlugin.log(e);
                //				}
                return;
            }
        }
        try {
            fStream.close();
        } catch (IOException e) {
            DebugPlugin.log(e);
        }
    }

    public void kill() {
        fKilled = true;
    }

    /**
     * @see IStreamMonitor#removeListener(IStreamListener)
     */
    public void removeListener(IStreamListener listener) {
        fListeners.remove(listener);
    }

    /**
     * Starts a thread which reads from the stream
     */
    public void startMonitoring() {
        if (fThread == null) {
            fThread = new Thread(new Runnable() {
                public void run() {
                    read();
                }
            }, //DebugCoreMessages.getString("OutputStreamMonitor.label")); //$NON-NLS-1$
            "Output Stream Monitor");
            fThread.start();
        }
    }

    /**
     * @see org.eclipse.debug.core.model.IFlushableStreamMonitor#setBuffered(boolean)
     */
    public void setBuffered(boolean buffer) {
        fBuffered = buffer;
    }

    /**
     * @see org.eclipse.debug.core.model.IFlushableStreamMonitor#flushContents()
     */
    public void flushContents() {
        fContents.setLength(0);
    }

    /**
     * @see IFlushableStreamMonitor#isBuffered()
     */
    public boolean isBuffered() {
        return fBuffered;
    }

    private ContentNotifier getNotifier() {
        return new ContentNotifier();
    }

    class ContentNotifier implements ISafeRunnable {

        private IStreamListener fListener;
        private String fText;

        /**
         * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
         */
        public void handleException(Throwable exception) {
        }

        /**
         * @see org.eclipse.core.runtime.ISafeRunnable#run()
         */
        public void run() throws Exception {
            fListener.streamAppended(fText, OutputStreamMonitor.this);
        }

        public void notifyAppend(String text) {
            if (text == null)
                return;
            fText = text;
            Object[] copiedListeners = fListeners.getListeners();
            for (int i = 0; i < copiedListeners.length; i++) {
                fListener = (IStreamListener) copiedListeners[i];
                SafeRunner.run(this);
            }
            fListener = null;
            fText = null;
        }
    }
}