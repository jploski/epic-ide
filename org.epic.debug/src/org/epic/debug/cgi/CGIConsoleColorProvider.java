/*
 * Created on 17.04.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug.cgi;

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



import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleColorProvider;
import org.eclipse.swt.graphics.Color;

/**
 * Default console color provider for a processs. Colors output to standard
 * out, in, and error, as specified by user preferences.
 * <p>
 * Clients implementing a console color provider should subclass this class.
 * </p>
 * @since 2.1
 */
public class CGIConsoleColorProvider implements IConsoleColorProvider {

    private IProcess fProcess;
    private IConsole fConsole;

    /**
     * @see IConsoleColorProvider#connect(IProcess, IConsole)
     */
    public void connect(IProcess process, IConsole 	console) {
        fProcess = process;
        fConsole = console;
        console.connect(((CGIProxy)process).getErrorStreamMonitor(),IDebugUIConstants.ID_STANDARD_ERROR_STREAM);
        console.connect(((CGIProxy)process).getInputStreamMonitor(),IDebugUIConstants.ID_STANDARD_INPUT_STREAM);
        console.connect(((CGIProxy)process).getOutputStreamMonitor(),IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM);
    }

    /**
     * @see IConsoleColorProvider#disconnect()
     */
    public void disconnect() {
        fConsole = null;
        fProcess = null;
    }

    /**
     * @see IConsoleColorProvider#isReadOnly()
     */
    public boolean isReadOnly() {
        return true;
        //return fProcess == null || fProcess.isTerminated();
    }

    /**
     * @see IConsoleColorProvider#getColor(String)
     */
    public Color getColor(String streamIdentifer) {
        if (IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM.equals(streamIdentifer)) {
            return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR);
        }
        if (IDebugUIConstants.ID_STANDARD_ERROR_STREAM.equals(streamIdentifer)) {
            return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR);
        }
        if (IDebugUIConstants.ID_STANDARD_INPUT_STREAM.equals(streamIdentifer)) {
            return DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_IN_COLOR);
        }
        return null;
    }

    /**
     * Returns the process this color provider is providing color for, or
     * <code>null</code> if none.
     *
     * @return the process this color provider is providing color for, or
     * <code>null</code> if none
     */
    protected IProcess getProcess() {
        return fProcess;
    }

    /**
     * Returns the console this color provider is connected to, or
     * <code>null</code> if none.
     *
     * @return IConsole the console this color provider is connected to, or
     * <code>null</code> if none
     */
    protected IConsole getConsole() {
        return fConsole;
    }
}