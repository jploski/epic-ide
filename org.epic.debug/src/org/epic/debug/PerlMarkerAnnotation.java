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

package org.epic.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * @author ruehl
 */
public class PerlMarkerAnnotation extends MarkerAnnotation
{
    private static final int NO_IMAGE = 0;
    private static final int BREAKPOINT_IMAGE = 6;

    private IDebugModelPresentation fPresentation;
    private AnnotationType fType;
    private int fImageType;

    public PerlMarkerAnnotation(IMarker marker)
    {
        super(marker);
    }

    /**
     * Initializes the annotation's icon representation and its drawing layer
     * based upon the properties of the underlying marker.
     */
    protected void initialize()
    {
        fImageType = NO_IMAGE;
        IMarker marker = getMarker();

        if (MarkerUtilities.isMarkerType(marker, IBreakpoint.BREAKPOINT_MARKER))
        {

            if (fPresentation == null)
                fPresentation = DebugUITools.newDebugModelPresentation();

            setImage(null); // see bug 32469
            setLayer(4);
            fImageType = BREAKPOINT_IMAGE;

            fType = AnnotationType.UNKNOWN;

        }
        else
        {
            fType = AnnotationType.UNKNOWN;
            if (marker.exists())
            {
                try
                {
                    if (marker.isSubtypeOf(IMarker.PROBLEM))
                    {
                        int severity = marker
                            .getAttribute(IMarker.SEVERITY, -1);
                        switch (severity)
                        {
                        case IMarker.SEVERITY_ERROR:
                            fType = AnnotationType.ERROR;
                            break;
                        case IMarker.SEVERITY_WARNING:
                            fType = AnnotationType.WARNING;
                            break;
                        }
                    }
                    else if (marker.isSubtypeOf(IMarker.TASK)) fType = AnnotationType.TASK;
                    else if (marker.isSubtypeOf(IMarker.BOOKMARK))
                        fType = AnnotationType.BOOKMARK;

                }
                catch (CoreException e)
                {
                    PerlDebugPlugin.log(e);
                }
            }
            super.initialize();
        }
    }

    public String getMessage()
    {
        IMarker marker = getMarker();
        if (marker == null || !marker.exists()) return ""; //$NON-NLS-1$
        else return marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
    }

    public boolean isTemporary()
    {
        return false;
    }

    public Image getImage(Display display)
    {
        if (fImageType == BREAKPOINT_IMAGE)
        {
            Image result = super.getImage(display);
            if (result == null)
            {
                IMarker marker = getMarker();
                if (marker != null && marker.exists())
                {
                    result = fPresentation.getImage(getMarker());
                    setImage(result);
                }
            }
            return result;
        }

        return super.getImage(display);
    }

    public AnnotationType getAnnotationType()
    {
        return fType;
    }
}