package org.epic.perleditor.editors.util;

import java.util.*;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class PerlColorProvider
{
    private final Map colors;
    
    public PerlColorProvider()
    {
        colors = new HashMap();
    }
    
    public synchronized void dispose()
    {
        for (Iterator i = colors.keySet().iterator(); i.hasNext();)
        {
            Object key = i.next();
            ((Color) colors.get(key)).dispose();
            i.remove();
        }
    }

	/**
	 * Return the Color that is stored in the Color table as rgb.
	 */
	public synchronized Color getColor(RGB rgb)
    {
        Color c = (Color) colors.get(rgb);
        if (c == null)
        {
            c = new Color(Display.getCurrent(), rgb);
            colors.put(rgb, c);
        }
        return c;
	}
}
