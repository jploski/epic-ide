/*
 * Created on 03.05.2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.perleditor.editors.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.epic.perleditor.editors.AddEditorMarker;

/**
 * @author luelljoc
 *
 * Handles Marker setting and deletion 
 * 
 */
public class MarkerUtil {
	
	//private ISourceViewer fViewer;
	//private TextEditor fTextEditor;
	private IResource fResource;
	
	private static final String EPIC_MARKER_USED_FLAG = "epic.markerUsedFlag";
	

//	public MarkerUtil(TextEditor textEditor, ISourceViewer viewer) {
//		this.fTextEditor = textEditor;
//		this.fViewer = viewer;
//		resource = (IResource) ((IAdaptable) fTextEditor.getEditorInput()).getAdapter(IResource.class);
//	}
	
	/**
	 * Constructor
	 * 
	 * @param resource The resource
	 */
	public MarkerUtil(IResource resource) {
		fResource = resource;
	}
	
	/**
	 *  Sets used flag to false of all markers that have the specified marker type
	 * 
	 * @param markerType The Marker type
	 */
	public void clearAllUsedFlags(String markerType) {
		clearAllUsedFlags(markerType, null);
	}
	
	/**
	 *  Sets used flag to false of all markers that have the specified marker type
	 * 
	 * @param markerType The Marker type
	 * @param additionalAttribute Additional attribute to check for
	 */
	public void clearAllUsedFlags(String markerType, String additionalAttribute) {
		try {
			IMarker[] markers = fResource.findMarkers(markerType, true, IResource.DEPTH_ONE);
			for(int i = 0; i < markers.length; i++) {
				if(additionalAttribute != null) {
	   				// If additional attribute is not present check next marker
	   				if(markers[i].getAttribute(additionalAttribute) == null) {
	   					continue;
	   				}
	   			}
                markers[i].setAttribute(EPIC_MARKER_USED_FLAG, false);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Deletes all unused markers of given type
	 * 
	 * @param markerType Marker Type
	 */
	public void removeUnusedMarkers(String markerType) {
		removeUnusedMarkers(markerType, null);
	}
	
	/**
	 * Deletes all unused markers of given type
	 * 
	 * @param markerType Marker Type
	 * @param additionalAttribute Additional attribut to check for
	 */
	public void removeUnusedMarkers(String markerType, String additionalAttribute) {
		try {
			IMarker[] markers = fResource.findMarkers(markerType, true, IResource.DEPTH_ONE);
			for(int i = 0; i < markers.length; i++) {
				if(additionalAttribute != null) {
	   				// If additional attribute is not present check next marker
	   				if(markers[i].getAttribute(additionalAttribute) == null) {
	   					continue;
	   				}
	   			}
				if(!markers[i].getAttribute(EPIC_MARKER_USED_FLAG, false)) {
					markers[i].delete();
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a marker
	 * 
	 * @param attributes	Marker attributes
	 * @param markerType Marker type
	 */
	public void addMarker(Map attributes, String markerType) {
		attributes.put(EPIC_MARKER_USED_FLAG, Boolean.TRUE);
		AddEditorMarker ed = new AddEditorMarker();
		ed.addMarker(fResource, attributes, markerType); 
	}
	
	/**
	 * @param makerType Marker Type
	 * @param line Line Number
	 * @param text Text to copare
	 * @param setUsedFlag Set to true if used flag should be set
	 * @return <code>true</code> if maker is already presen otherwise <code>false</code>
	 */
	public boolean isMarkerPresent(String makerType, int line, String text,  boolean setUsedFlag) {
		return isMarkerPresent(makerType, line, text, null, setUsedFlag);
	}
	
	/**
	 * @param makerType Marker Type
	 * @param line Line Number
	 * @param text Text to copare
	 * @param additionalAtrribute Additional attribute to check for
	 * @param setUsedFlag Set to true if used flag should be set
	 * @return <code>true</code> if maker is already presen otherwise <code>false</code>
	 */
	public boolean isMarkerPresent(String makerType, int line, String text, String additionalAttribute, boolean setUsedFlag) {
		boolean found = false;
		
		List markers = getMarkersForLine(fResource, line);
	   for(int i = 0; i < markers.size(); i++) {
	   		Marker marker = (Marker) markers.get(i);
	   		String markerText = marker.getAttribute(IMarker.MESSAGE, (String) null);
	   		
	   		try {
	   			if(!marker.getType().equals(makerType)) {
		   			continue;
		   		}
	   			
	   			if(additionalAttribute != null) {
	   				// If additional attribute is not present check next marker
	   				if(marker.getAttribute(additionalAttribute) == null) {
	   					continue;
	   				}
	   			}
	   			
				if(markerText.equals(text)) {
					found = true;
					if(setUsedFlag) {
						try {
							marker.setAttribute(EPIC_MARKER_USED_FLAG, true);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   }
	   
	   return found;
	}
	
	/**
	 *  Returns all markers which includes the ruler's line of activity.
	 * 
	 * @param resource The reource
	 * @param aLine The line number
	 * @return List of all makers for given line
	 */
	public static List getMarkersForLine(IResource resource, int aLine) {
		List markers = new ArrayList();
		try {
			IMarker[] allMarkers = resource.findMarkers(null, true, IResource.DEPTH_ONE);
			for(int i = 0; i < allMarkers.length; i++) {
				IMarker marker = allMarkers[i];
                Integer lineNr = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
				if(lineNr != null && lineNr.intValue() == aLine) {
					markers.add(marker);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return markers;
	}
	
//	/**
//	 * Returns all markers which includes the ruler's line of activity.
//	*
//	 * @param aLine Line number
//	 * @return List of all markers
//	 */
//	public static List getMarkersForLine(ISourceViewer viewer, int aLine) {
//		List markers = new ArrayList();
//		IAnnotationModel model = viewer.getAnnotationModel();
//		if (model != null) {
//			Iterator e = model.getAnnotationIterator();
//			while (e.hasNext()) {
//				Object o = e.next();
//				if (o instanceof MarkerAnnotation) {
//					MarkerAnnotation a = (MarkerAnnotation) o;
//					if (compareRulerLine(model.getPosition(a),
//						viewer.getDocument(),
//						aLine)
//						!= 0) {
//						markers.add(a.getMarker());
//					}
//				}
//			}
//		}
//		return markers;
//	}
//	
//	/**
//	 * Returns distance of given line to specified position (1 = same line,
//	 * 2 = included in given position, 0 = not related).
//	 * 
//	 * @param aPosition Position
//	 * @param aDocument Document
//	 * @param aLine Line number
//	 * @return
//	 */
//	protected static int compareRulerLine(
//		Position aPosition,
//		IDocument aDocument,
//		int aLine) {
//		int distance = 0;
//		if (aPosition.getOffset() > -1 && aPosition.getLength() > -1) {
//			try {
//				int markerLine =
//					aDocument.getLineOfOffset(aPosition.getOffset());
//				if (aLine == markerLine) {
//					distance = 1;
//				} else if (
//					markerLine <= aLine
//						&& aLine
//							<= aDocument.getLineOfOffset(
//								aPosition.getOffset()
//									+ aPosition.getLength())) {
//					distance = 2;
//				}
//			} catch (BadLocationException e) {
//			}
//		}
//		return distance;
//	}

}
