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
package org.epic.perleditor.editors;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;


/**
 * Action for creating a readme marker with a specfic id 
 * attribute value.
 */
public class AddEditorMarker {
	
	private ITextEditor textEditor;
	
	public AddEditorMarker() {
		 textEditor = null;
	}
	
	public AddEditorMarker(ITextEditor editor) {
	    textEditor = editor;
	}
	
	public void addMarker(IResource resource, Map attributes, String markerType) {
			try {
				MarkerUtilities.createMarker(resource, attributes, markerType);
			} catch (CoreException x) {
				x.printStackTrace();
			}
	}

	public void addMarker(Map attributes, String markerType) {
		if(textEditor == null)
			return;
			
		try {
			MarkerUtilities.createMarker(getResource(), attributes, markerType);
		} catch (CoreException x) {
			x.printStackTrace();
		}
	}
	
	/** 
	 * Returns the resource on which to create the marker, 
	 * or <code>null</code> if there is no applicable resource. This
	 * queries the editor's input using <code>getAdapter(IResource.class)</code>.
	 *
	 * @return the resource to which to attach the newly created marker
	 */
	protected IResource getResource() {
		IEditorInput input= textEditor.getEditorInput();
		return (IResource) ((IAdaptable) input).getAdapter(IResource.class);
	}
}
