package org.epic.debug.cgi.server;

/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.epic.debug.PerlDebugPlugin;

/**
 * Used to receive output from processes
 * 
 * Pulled from the Eclipse private API stock and avoid using private non-API
 *   until proper solution can be found
 */
public class StreamConsumer extends Thread
{
    BufferedReader bReader;

    private String lastLine;

    public StreamConsumer(InputStream inputStream)
    {
        super();
        setDaemon(true);
        bReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public void run()
    {
        try
        {
            String line;

            while ( null != ( line = bReader.readLine()))
            {
                lastLine = line;
            //  BrowserLog.log(line);  // It's just some dumb log file, what up!
            }

            bReader.close();
        }
        catch ( IOException ioe )
        {
            logError("Exception occurred reading from web browser.", ioe); //$NON-NLS-1$
        }
    }


    /**
     * Logs an Error message with an exception. Note that the message should
     * already be localized to proper locale. ie: Resources.getString() should
     * already have been called
     */
    private synchronized void logError( String message, Throwable ex )
    {
        if ( message == null ) message = ""; //$NON-NLS-1$

        Status errorStatus = new Status(
                IStatus.ERROR
              , PerlDebugPlugin.getPluginId()
              , IStatus.OK
              , message
              , ex );

        PerlDebugPlugin.getDefault().getLog().log(errorStatus);
    }

    /**
     * @return last line obtained or null
     */
    public String getLastLine() {
        return lastLine;
    }
}

// END