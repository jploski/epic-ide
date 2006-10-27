package org.epic.core.util;

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

    private static String OWNER_KEY = "org.epic.core.resource.marker.owner";

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

    public void createProblemMarker(IResource resource, String owner, Map attributes)
    {
        createMarker(resource, IMarker.PROBLEM, owner, attributes);
    }

//    public void setResource(IResource resource)
//    {
//        this.resource = resource;
//    }
//
//    public IResource getResource()
//    {
//        return this.resource;
//    }

    public void createMarker(IResource resource, String type, String owner, Map attributes)
    {
        assert isNotNullOrEmpty(owner);
        assert attributes != null;

        attributes.put(OWNER_KEY, owner);

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

        ArrayList list = new ArrayList();
        for (int i = 0; i < markers.length; i++)
        {
            Integer markerLineNumber = (Integer) getAttribute(markers[i], IMarker.LINE_NUMBER);
            if (markerLineNumber != null && markerLineNumber.intValue() == lineNumber)
            {
                list.add(markers[i]);
            }
        }

        return (IMarker[]) list.toArray(new IMarker[list.size()]);
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

    public void deleteProblemMarkers(IResource resource, String owner)
    {
        deleteMarkers(resource, IMarker.PROBLEM, owner);
    }

    public void deleteMarkers(IResource resource, String type, String owner)
    {
        assert isNotNullOrEmpty(type);
        assert isNotNullOrEmpty(owner);

        IMarker[] markers = findMarkers(resource, type);

        // no markers found
        if (markers.length == 0) { return; }

        IMarker[] toDelete = new IMarker[markers.length];

        int deleteIndex = 0;
        for (int i = 0; i < markers.length; i++)
        {
            if (shouldDelete(markers[i], owner))
            {
                toDelete[deleteIndex++] = markers[i];
            }
        }

        try
        {
            resource.getWorkspace().deleteMarkers(toDelete);
        }
        catch (CoreException e)
        {
            log.log(StatusFactory.createError(pluginId, "unable to delete markers", e));
        }
    }

    public void setLineNumber(Map attributes, int lineNumber)
    {
        attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
    }

    public void setMessage(Map attributes, String message)
    {
        attributes.put(IMarker.MESSAGE, message);
    }

    /**
     * @see MarkerUtilities#setSeverity(Map, Integer)
     */
    public void setSeverity(Map attributes, int severity)
    {
        setSeverity(attributes, new Integer(severity));
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
    public void setSeverity(Map attributes, Integer severity)
    {
        assert ((severity.intValue() >= 0) && (severity.intValue() <= 2));
        attributes.put(IMarker.SEVERITY, severity);
    }

    public void setStartEnd(Map attributes, int start, int end)
    {
        assert ((start >= 0) && (end >= 0));
        attributes.put(IMarker.CHAR_START, new Integer(start));

        if (end == 0)
        {
            end = start;
        }

        attributes.put(IMarker.CHAR_END, new Integer(end));
    }

//    private IMarker[] findMarkers(String type)
//    {
//        try
//        {
//            return resource.findMarkers(type, true, IResource.DEPTH_ZERO);
//        }
//        catch (CoreException e)
//        {
//            log.log(StatusFactory.createError(pluginId, "unable to find markers", e));
//            return EMPTY_ARRAY;
//        }
//    }

    private IMarker[] findMarkers(IResource resource, String type)
    {
        try
        {
            return resource.findMarkers(type, true, IResource.DEPTH_ZERO);
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

    private boolean shouldDelete(IMarker marker, String owner)
    {
        try
        {
            return (owner.equals(marker.getAttribute(OWNER_KEY))) ? true : false;
        }
        catch (CoreException e)
        {
            log.log(StatusFactory.createError(pluginId, "unable to delete marker", e));
            /*
             * should not happen, but probably better to remove the marker then run the risk of it
             * being stale
             */
            return true;
        }
    }

}
