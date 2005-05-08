package org.epic.perleditor.editors;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.editors.util.MarkerUtil;

/** 
 * The JavaAnnotationHover provides the hover support for java editors.
 */

public class PerlAnnotationHover implements IAnnotationHover {

	static final int MAX_INFO_LENGTH = 80;
	private TextEditor fTextEditor;
	
	public  PerlAnnotationHover(TextEditor editor) {
		super();
		fTextEditor = editor;
	}

	/**
	 * @see org.eclipse.jface.text.source.IAnnotationHover#getHoverInfo(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	
	public String getHoverInfo(ISourceViewer viewer, int line) {
		String info = null;
		
		IResource resource = (IResource) ((IAdaptable) fTextEditor.getEditorInput()).getAdapter(IResource.class);
		
		List markers = MarkerUtil.getMarkersForLine(resource, line+1);
		if (markers != null) {
			info = "";
			for (int i =  0; i < markers.size(); i++) {
				IMarker marker = (IMarker) markers.get(i);
				String message =
					marker.getAttribute(IMarker.MESSAGE, (String) null);
				if (message != null && message.trim().length() > 0) {
					
					if (message.length() > MAX_INFO_LENGTH) {
						message = splitMessage(message);
					}
					info += message;
					
					if(i != markers.size() - 1) {
						 info += "\n";
					}
				}
			}
		}
		return info;
	}

	private String splitMessage(String message) {
		String result = "";
		
		if(message.length() <= MAX_INFO_LENGTH) {
			return message;
		}
		
		String tmpStr = new String(message);
		
		while(tmpStr.length() > MAX_INFO_LENGTH) {
			
			int spacepos = tmpStr.indexOf(" ", MAX_INFO_LENGTH);
			
			if(spacepos != -1) {
				result += tmpStr.substring(0, spacepos) + "\n";
				tmpStr = tmpStr.substring(spacepos);
			}
			else {
				result += tmpStr.substring(0, MAX_INFO_LENGTH) + "\n";
				tmpStr = tmpStr.substring(MAX_INFO_LENGTH);
			}
			
			
		    	
		}
		
		result += tmpStr;
		
		return result;
	}


}
