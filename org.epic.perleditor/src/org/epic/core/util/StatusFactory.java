package org.epic.core.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Convience class to create {@link IStatus IStatus} objects used for logging
 *
 * <p>Any method defined in this class that does not specify a <code>status</code> parameter
 * uses <code>IStatus.OK</code></p>
 */
public class StatusFactory
{
    /** @see #createError(String pluginId, int status, String message, Throwable throwable) */
    public static IStatus createError(String pluginId, String message)
    {
        return createError(pluginId, message, null);
    }

    /** @see #createError(String pluginId, int status, String message, Throwable throwable) */
    public static IStatus createError(String pluginId, String message, int status)
    {
        return createError(pluginId, status, message, null);
    }

    /** @see #createError(String pluginId, int status, String message, Throwable throwable) */
    public static IStatus createError(String pluginId, String message, Throwable throwable)
    {
        return createError(pluginId, IStatus.OK, message, throwable);
    }

    /**
     * creates an <code>IStatus.ERROR</code> object
     *
     * @param pluginId plugin id
     * @param status status code
     * @param message error message
     * @param throwable exception or null if not applicable
     *
     * @return Status object
     */
    public static IStatus createError(String pluginId, int status, String message, Throwable throwable)
    {
        return createStatus(IStatus.ERROR, pluginId, status, message, throwable);
    }

    /** @see #createWarning(String pluginId, int status, String message, Throwable throwable) */
    public static IStatus createWarning(String pluginId, String message)
    {
        return createWarning(pluginId, message, null);
    }

    /** @see #createWarning(String pluginId, int status, String message, Throwable throwable) */
    public static IStatus createWarning(String pluginId, String message, int status)
    {
        return createWarning(pluginId, status, message, null);
    }

    /** @see #createWarning(String pluginId, int status, String message, Throwable throwable) */
    public static IStatus createWarning(String pluginId, String message, Throwable throwable)
    {
        return createWarning(pluginId, IStatus.OK, message, throwable);
    }

    /**
     * creates an <code>IStatus.WARNING</code> object
     *
     * @param pluginId plugin id
     * @param status status code
     * @param message warning message
     * @param throwable exception or null if not applicable
     *
     * @return Status object
     */
    public static IStatus createWarning(String pluginId, int status, String message, Throwable throwable)
    {
        return createStatus(IStatus.WARNING, pluginId, status, message, throwable);
    }

    private static IStatus createStatus(int severity, String pluginId, int code, String message, Throwable throwable)
    {
        /*
         * TODO: support for i8n
         *
         * this should be very easy to add here w/o any changes to the interface and - lookup
         * the message in the resource bundle based on the input message param - if it's not found,
         * default to using the message param.  -jae
         */
        return new Status(severity, pluginId, code, message, throwable);
    }

}
