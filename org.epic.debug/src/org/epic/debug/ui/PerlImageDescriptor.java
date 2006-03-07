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
package org.epic.debug.ui;


import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * A PerlImageDescriptor consists of a main icon and several adornments. The adornments
 * are computed according to flags set on creation of the descriptor.
 */
public class PerlImageDescriptor extends CompositeImageDescriptor {

	/** Flag to render the installed breakpoint adornment */
	public final static int INSTALLED= 					0x0004;
	/** Flag to render the entry method breakpoint adornment */
	public final static int ENTRY=				 		0x0008;
	/** Flag to render the exit method breakpoint adornment */
	public final static int EXIT=				 		0x0010;
	/** Flag to render the enabled breakpoint adornment */
	public final static int ENABLED=						0x0020;
	/** Flag to render the conditional breakpoint adornment */
	public final static int CONDITIONAL=					0x0040;
	/** Flag to render the caught breakpoint adornment */
	public final static int CAUGHT=						0x0080;
	/** Flag to render the uncaught breakpoint adornment */
	public final static int UNCAUGHT=					0x0100;
	/** Flag to render the scoped breakpoint adornment */
	public final static int SCOPED=						0x0200;


	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;

	/**
	 * Create a new PerlImageDescriptor.
	 *
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered
	 *
	 */
	public PerlImageDescriptor(ImageDescriptor baseImage, int flags) {
		setBaseImage(baseImage);
		setFlags(flags);
	}

	/**
	 * @see CompositeImageDescriptor#getSize()
	 */
	protected Point getSize() {
		if (fSize == null) {
			ImageData data= getBaseImage().getImageData();
			setSize(new Point(data.width, data.height));
		}
		return fSize;
	}

	/**
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (!(object instanceof PerlImageDescriptor)){
			return false;
		}

		PerlImageDescriptor other= (PerlImageDescriptor)object;
		return (getBaseImage().equals(other.getBaseImage()) && getFlags() == other.getFlags());
	}

	/**
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return getBaseImage().hashCode() | getFlags();
	}

	/**
	 * @see CompositeImageDescriptor#drawCompositeImage(int, int)
	 */
	protected void drawCompositeImage(int width, int height) {
		ImageData bg= getBaseImage().getImageData();
		if (bg == null) {
			bg= DEFAULT_IMAGE_DATA;
		}
		drawImage(bg, 0, 0);
		drawBreakpointOverlays();
	}

	

	protected void drawBreakpointOverlays() {
		int flags= getFlags();
		int x= 0;
		int y= 0;
		ImageData data= null;
		if ((flags & INSTALLED) != 0) {
			x= 0;
			y= getSize().y;
			if ((flags & ENABLED) !=0) {
				data= PerlDebugImages.DESC_OBJS_BREAKPOINT_INSTALLED.getImageData();
			} else {
				data= PerlDebugImages.DESC_OBJS_BREAKPOINT_INSTALLED_DISABLED.getImageData();
			}

			y -= data.height;
			drawImage(data, x, y);
		}
		if ((flags & CAUGHT) != 0) {
			if ((flags & ENABLED) !=0) {
			data= PerlDebugImages.DESC_OBJS_CAUGHT_BREAKPOINT.getImageData();
			} else {
				data= PerlDebugImages.DESC_OBJS_CAUGHT_BREAKPOINT_DISABLED.getImageData();
			}
			x= 0;
			y= 0;
			drawImage(data, x, y);
		}
		if ((flags & UNCAUGHT) != 0) {
			if ((flags & ENABLED) !=0) {
				data= PerlDebugImages.DESC_OBJS_UNCAUGHT_BREAKPOINT.getImageData();
			} else {
				data= PerlDebugImages.DESC_OBJS_UNCAUGHT_BREAKPOINT_DISABLED.getImageData();
			}
			x= data.width;
			y= data.height;
			drawImage(data, x, y);
		}
		if ((flags & SCOPED) != 0) {
			if ((flags & ENABLED) !=0) {
				data= PerlDebugImages.DESC_OBJS_SCOPED_BREAKPOINT.getImageData();
			} else {
				data= PerlDebugImages.DESC_OBJS_SCOPED_BREAKPOINT_DISABLED.getImageData();
			}
			x= 0;
			y= getSize().y;
			y-= data.height;
			drawImage(data, x, y);
		}
		if ((flags & CONDITIONAL) != 0) {
			x= getSize().x;
			y= 0;
			if ((flags & ENABLED) !=0) {
				data= PerlDebugImages.DESC_OBJS_CONDITIONAL_BREAKPOINT.getImageData();
			} else {
				data= PerlDebugImages.DESC_OBJS_CONDITIONAL_BREAKPOINT_DISABLED.getImageData();
			}
			x -= data.width;
			drawImage(data, x, y);
		} else {
			if ((flags & ENTRY) != 0) {
				x= getSize().x;
				y= 0;
				if ((flags & ENABLED) !=0) {
					data= PerlDebugImages.DESC_OBJS_METHOD_BREAKPOINT_ENTRY.getImageData();
				} else {
					data= PerlDebugImages.DESC_OBJS_METHOD_BREAKPOINT_ENTRY_DISABLED.getImageData();
				}
				x -= data.width;
				drawImage(data, x, y);
			}
			if ((flags & EXIT)  != 0){
				x= getSize().x;
				y= getSize().y;
				if ((flags & ENABLED) != 0) { 
					data= PerlDebugImages.DESC_OBJS_METHOD_BREAKPOINT_EXIT.getImageData();
				} else {
					data= PerlDebugImages.DESC_OBJS_METHOD_BREAKPOINT_EXIT_DISABLED.getImageData();
				}
				x -= data.width;
				y -= data.height;
				drawImage(data, x, y);
			}
		}
	}
	protected ImageDescriptor getBaseImage() {
		return fBaseImage;
	}

	protected void setBaseImage(ImageDescriptor baseImage) {
		fBaseImage = baseImage;
	}

	protected int getFlags() {
		return fFlags;
	}

	protected void setFlags(int flags) {
		fFlags = flags;
	}

	protected void setSize(Point size) {
		fSize = size;
	}
}