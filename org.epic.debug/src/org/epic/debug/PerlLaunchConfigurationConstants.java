package org.epic.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;

/**
 * @author ruehl
 */
public class PerlLaunchConfigurationConstants
{
    public final static String ATTR_PROJECT_NAME = "ATTR_PROJECT_NAME";
    public final static String ATTR_STARTUP_FILE = "ATTR_STARTUP_FILE";
    public final static String ATTR_WORKING_DIRECTORY = "ATTR_WORKING_DIRECTORY";
    public final static String ATTR_AUTO_RECONNECT = "ATTR_AUTO_RECONNECT";
    public final static String ATTR_START_LOCAL = "ATTR_START_LOCAL";
    public final static String ATTR_START_REMOTE = "ATTR_START_REMOTE";
    public final static String ATTR_PERL_PARAMETERS = "ATTR_PERL_PARAMETERS";
    public final static String ATTR_PROGRAM_PARAMETERS = "ATTR_PROGRAM_PARAMETERS";
    public final static String ATTR_DEBUG_HOST = "ATTR_DEBUG_HOST";
    public final static String ATTR_DEBUG_PORT = "ATTR_DEBUG_PORT";
    public final static String ATTR_DEBUG_IO_PORT = "ATTR_DEBUG_IO_PORT";
    public final static String ATTR_DEBUG_ERROR_PORT = "ATTR_DEBUG_ERROR_PORT";
    public final static String ATTR_DEBUG_CGI = "ATTR_DEBUG_CGI";
    public final static String ATTR_CUSTOM_BROWSER_PATH = "ATTR_CUSTOM_BROWSER_PATH";
    public final static String ATTR_BROWSER_ID = "ATTR_BROWSER_ID";
    public final static String ATTR_HTML_ROOT_DIR = "ATTR_HTML_ROOT_DIR";
    public final static String ATTR_HTML_ROOT_FILE = "ATTR_HTML_ROOT_FILE";
    public final static String ATTR_CGI_ROOT_DIR = "ATTR_CGI_ROOT_DIR";
    public final static String ATTR_CGI_ENV = "ATTR_CGI_ENV";
    /* STR */
    public final static String ATTR_CGI_FILE_EXTENSION = "ATTR_CGI_FILE_EXTENSION";
    public static final String ATTR_REMOTE_PORT = "ATTR_REMOTE_PORT";
    public static final String ATTR_REMOTE_DEST = "ATTR_REMOTE_DEST";
    public static final String ATTR_REMOTE_HOST = "ATTR_REMOTE_HOST";
    public static final String ATTR_REMOTE = "ATTR_REMOTE";
    public static final String ATTR_REMOTE_CREATE_DEBUG_PACKAGE = "ATTR_REMOTE_CREATE_DEBUG_PACKAGE";
    public static final String ATTR_REMOTE_CAPTURE_OUTPUT = "ATTR_REMOTE_CAPTURE_OUTPUT";
    public static final String ATTR_REMOTE_DEBUG_PACKAGE_PATH = "ATTR_REMOTE_DEBUG_PACKAGE_PATH";

    public static final String PERL_PROCESS_TYPE = "perl";
    
    public final static String getDebugPort(ILaunch fLaunch)
    {
        try
        {
            return fLaunch.getLaunchConfiguration().getAttribute(
                ATTR_DEBUG_PORT, PerlDebugPlugin.getDefaultDebugPort());
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return null;
        }
    }

    public final static int getDebugPortInt(ILaunch fLaunch)
    {
        return Integer.parseInt(getDebugPort(fLaunch));
    }
}
