package org.epic.perleditor.editors.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class PerlColorProvider {

	/**
	 * Return the Color that is stored in the Color table as rgb.
	 */
	public static Color getColor(RGB rgb) {
			Color color= new Color(Display.getCurrent(), rgb);

		return color;
	}

}
