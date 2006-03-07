/* 
 * Created on 12.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug.ui;


import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */



/**
 * A registry that maps <code>ImageDescriptors</code> to <code>Image</code>.
 */
public class PerlImageDescriptorRegistry {

	private HashMap fRegistry= new HashMap(10);
	private Display fDisplay;

	/**
	 * Creates a new image descriptor registry for the current or default display,
	 * respectively.
	 */
	public PerlImageDescriptorRegistry() {
		this(getStandardDisplay());
	}

	/**
	 * Creates a new image descriptor registry for the given display. All images
	 * managed by this registry will be disposed when the display gets disposed.
	 *
	 * @param diaplay the display the images managed by this registry are allocated for
	 */
	public PerlImageDescriptorRegistry(Display display) {
		fDisplay= display;
		Assert.isNotNull(fDisplay);
		hookDisplay();
	}

	/**
	 * Returns the image assiciated with the given image descriptor.
	 *
	 * @param descriptor the image descriptor for which the registry manages an image
	 * @return the image associated with the image descriptor or <code>null</code>
	 *  if the image descriptor can't create the requested image.
	 */
	public Image get(ImageDescriptor descriptor) {
		if (descriptor == null)
			descriptor= ImageDescriptor.getMissingImageDescriptor();

		Image result= (Image)fRegistry.get(descriptor);
		if (result != null)
			return result;

		Assert.isTrue(fDisplay == getStandardDisplay(), "Allocating image for wrong display."); //$NON-NLS-1$
		result= descriptor.createImage();
		if (result != null)
			fRegistry.put(descriptor, result);
		return result;
	}

	/**
	 * Disposes all images managed by this registry.
	 */
	public void dispose() {
		for (Iterator iter= fRegistry.values().iterator(); iter.hasNext(); ) {
			Image image= (Image)iter.next();
			image.dispose();
		}
		fRegistry.clear();
	}

	private void hookDisplay() {
//		fDisplay.disposeExec(new Runnable() {
//			public void run() {
//				dispose();
//			}
//		});
	}
	
	/**
		 * Returns the standard display to be used. The method first checks, if
		 * the thread calling this method has an associated disaply. If so, this
		 * display is returned. Otherwise the method returns the default display.
		 */
		public static Display getStandardDisplay() {
			Display display;
			display= Display.getCurrent();
			if (display == null)
				display= Display.getDefault();
			return display;
		}
}
