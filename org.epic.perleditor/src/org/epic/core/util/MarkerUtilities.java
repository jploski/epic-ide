package org.epic.core.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;


/**
 */
public class MarkerUtilities
{
    //~ Static fields/initializers

    private static IMarker[] EMPTY_ARRAY = new IMarker[0];

    //~ Instance fields

    private ILog log;
    private String pluginId;
    //private IResource resource;

    //~ Constructors

    public MarkerUtilities(ILog log, String pluginId)
    {
        this.log = log;
        this.pluginId = pluginId;
    }

    //~ Methods

    public void createMarker(IResource resource, String type, Map<String, ?> attributes)
    {
        assert attributes != null;

        try
        {
            IMarker marker = resource.createMarker(type);
            marker.setAttributes(attributes);
        }
        catch (CoreException e)
        {
            // should not happen
            log.log(StatusFactory.createError(pluginId, "unable to create marker", e));
        }
    }

    public IMarker[] getMarkersForLine(IResource resource, int lineNumber)
    {
        return getMarkersForLine(resource, null, lineNumber);
    }

    public boolean isMarkerPresent(IResource resource, String markerType, String text, int lineNumber)
    {
        IMarker[] markers = getMarkersForLine(resource, markerType, lineNumber);

        if (markers.length == 0) { return false; }

        for (int i = 0; i < markers.length; i++)
        {
            String markerText = (String) getAttribute(markers[i], IMarker.MESSAGE);
            if (markerText != null && markerText.equals(text))
            {
                return true;
            }
        }

        return false;
    }

    public IMarker[] getMarkersForLine(IResource resource, String markerType, int lineNumber)
    {
        IMarker[] markers = findMarkers(resource, markerType);

        if (markers.length == 0) { return EMPTY_ARRAY; }

        ArrayList<IMarker> list = new ArrayList<IMarker>();
        for (int i = 0; i < markers.length; i++)
        {
            Integer markerLineNumber = (Integer) getAttribute(markers[i], IMarker.LINE_NUMBER);
            if (markerLineNumber != null && markerLineNumber.intValue() == lineNumber)
            {
                list.add(markers[i]);
            }
        }

        return list.toArray(new IMarker[list.size()]);
    }

    private Object getAttribute(IMarker marker, String key)
    {
        try
        {
            return marker.getAttribute(key);
        }
        catch (CoreException e)
        {
            // ignore - the marker no longer exists
            return null;
        }
    }

    public void deleteMarkers(IResource resource, String type)
    {
        assert isNotNullOrEmpty(type);

        IMarker[] toDelete = findMarkers(resource, type);

        // no markers found
        if (toDelete.length == 0) { return; }

        try
        {
            resource.getWorkspace().deleteMarkers(toDelete);
        }
        catch (CoreException e)
        {
            log.log(StatusFactory.createError(pluginId, "unable to delete markers", e));
        }
    }

    public void setLineNumber(Map<String, Serializable> attributes, int lineNumber)
    {
        attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
    }

    public void setMessage(Map<String, Serializable> attributes, String message)
    {
        attributes.put(IMarker.MESSAGE, message);
    }

    /**
     * set the marker severity to one of the following values:
     *
     * <ol>
     *   <li>IMarker.INFO</li>
     *   <li>IMarker.WARN</li>
     *   <li>IMarker.ERROR</li>
     * </ol>
     */
    public void setSeverity(Map<String, Serializable> attributes, int severity)
    {
        assert (severity >= 0 && severity <= 2);
        attributes.put(IMarker.SEVERITY, Integer.valueOf(severity));
    }

    public void setStartEnd(Map<String, Serializable> attributes, int start, int end)
    {
        assert ((start >= 0) && (end >= 0));
        attributes.put(IMarker.CHAR_START, Integer.valueOf(start));

        if (end == 0)
        {
            end = start;
        }

        attributes.put(IMarker.CHAR_END, Integer.valueOf(end));
    }

    private IMarker[] findMarkers(IResource resource, String type)
    {
        try
        {
            return resource.findMarkers(type, true, IResource.DEPTH_INFINITE);
        }
        catch (CoreException e)
        {
            log.log(StatusFactory.createError(pluginId, "unable to find markers", e));
            return EMPTY_ARRAY;
        }
    }

    private boolean isNotNullOrEmpty(String s)
    {
        return ((s != null) && ! "".equals(s)) ? true : false;
    }

}
