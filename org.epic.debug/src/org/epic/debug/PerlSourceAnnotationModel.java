/*
 * Created on 12.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlSourceAnnotationModel extends ResourceMarkerAnnotationModel {


			
			    public PerlSourceAnnotationModel(IFileEditorInput input) {
					super(input.getFile());
				}

				protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
					return new PerlMarkerAnnotation(marker);
				}

			
			
			
}
