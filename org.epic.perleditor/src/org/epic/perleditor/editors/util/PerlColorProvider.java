package org.epic.perleditor.editors.util;

import java.util.*;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class PerlColorProvider
{
    private final Map<RGB, Color> colors;
    
    public PerlColorProvider()
    {
        colors = new HashMap<RGB, Color>();
    }
    
    public synchronized void dispose()
    {
        for (Iterator<RGB> i = colors.keySet().iterator(); i.hasNext();)
        {
            Object key = i.next();
            colors.get(key).dispose();
            i.remove();
        }
    }

	/**
	 * Return the Color that is stored in the Color table as rgb.
	 */
	public synchronized Color getColor(RGB rgb)
    {
        Color c = colors.get(rgb);
        if (c == null)
        {
            c = new Color(Display.getCurrent(), rgb);
            colors.put(rgb, c);
        }
        return c;
	}
}
