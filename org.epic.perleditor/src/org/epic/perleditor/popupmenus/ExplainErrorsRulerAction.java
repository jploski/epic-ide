/*
 * Created on Jun 7, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.perleditor.popupmenus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.views.ExplainErrorsView;

/**
 * @author luelljoc
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ExplainErrorsRulerAction extends Action implements IUpdate {
	
	IVerticalRulerInfo ruler;
	ITextEditor editor;
	ArrayList markers;

	public ExplainErrorsRulerAction(IVerticalRulerInfo ruler, ITextEditor editor) {
		this.ruler = ruler;
		this.editor = editor;
		getMarkersForLine(ruler.getLineOfLastMouseButtonActivity()+1);
	}
	
	/**
	 * @see Action#run()
	 */
	public void run() {
		ExplainErrorsView view = null;
		IWorkbenchPage activePage =
			PerlEditorPlugin
				.getWorkbenchWindow()
				.getActivePage();
		try
		{
		view = (ExplainErrorsView)	activePage.showView(
				"org.epic.perleditor.views.ExplainErrorsView");
		} catch (PartInitException e)
		{
			e.printStackTrace();
		}
		view.explain(markers);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		setEnabled(markers.size() > 0 ? true : false);
	}
	
	/**
	 * Returns all markers which includes the ruler's line of activity.
	 */
	private List getMarkersForLine(int aLine) {
		
		markers = new ArrayList();
		IDocumentProvider provider= editor.getDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(editor.getEditorInput());

		if (model != null) {
			Iterator e = model.getAnnotationIterator();
			while (e.hasNext()) {
				Object o = e.next();
				if (o instanceof MarkerAnnotation) {
					MarkerAnnotation a = (MarkerAnnotation) o;
					try {
						IMarker marker = a.getMarker();
						int markerLineNumber = ((Integer) marker.getAttribute(IMarker.LINE_NUMBER)).intValue();
						if (marker.getType() == IMarker.PROBLEM && markerLineNumber == aLine) {
							markers.add(marker);
							//System.out.println("Marker: " + marker.getAttribute(IMarker.MESSAGE));
						}
					} catch (CoreException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		return markers;
	}


}
