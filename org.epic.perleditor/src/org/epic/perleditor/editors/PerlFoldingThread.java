package org.epic.perleditor.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.util.SourceParser;

public class PerlFoldingThread extends Thread implements IdleTimerListener {

	private final Object lock1 = new Object();

	private ISourceViewer fSourceViewer;

	private TextEditor fTextEditor;

	private String text = null;

	private static final Integer ANNOTATION_NEW = new Integer(1);

	private static final Integer ANNOTATION_EXISTS = new Integer(2);

	public PerlFoldingThread(TextEditor textEditor, ISourceViewer viewer) {
		super();
		this.fTextEditor = textEditor;
		this.fSourceViewer = viewer;
	}

	public void onEditorIdle(ISourceViewer viewer) {
		this.fSourceViewer = viewer;
		//this.setText(fSourceViewer.getTextWidget().getText());
		this.setText(fSourceViewer.getDocument().get());
	}

	public void setText(String newText) {
		synchronized (this.lock1) {
			this.text = newText;
			this.lock1.notifyAll();
		}
	}

	public void run() {
		try {
			while (!Thread.interrupted()) {
				synchronized (this.lock1) {
					this.lock1.wait();
				}

				try {
					this.updateFoldingAnnotations();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
			//everything is fine, and this thread will terminate
		}
	}

	public void updateFoldingAnnotations() {
    IAnnotationModel model = (IAnnotationModel) fTextEditor.getAdapter(ProjectionAnnotationModel.class);

    String lineSep = null;
    try {
      lineSep = fSourceViewer.getDocument().getLineDelimiter(0);
      
      if (model != null) {
      HashMap positionHash = new HashMap();
        // Remove all annotations
        //			for (Iterator i = model.getAnnotationIterator(); i.hasNext();) {
        //				ProjectionAnnotation annotation = (ProjectionAnnotation) i
        //						.next();
        //				model.removeAnnotation(annotation);
        //			}

        IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();

        boolean foldingEnabled = store.getBoolean(PreferenceConstants.SOURCE_FOLDING);

        List podList = new ArrayList();
        List subList = new ArrayList();

        if (foldingEnabled) {

          // Get all pod comments
          String podExpr = "^(=head.*(" + lineSep + ".*)+?" + lineSep + "=cut)$";
          podList = SourceParser.getElements(text, podExpr, "", "", SourceParser.DO_NOT_DELETE_COMMENT_POD);

          // Get all subroutines          String subExpr = "^[\\s]*sub\\s+([^\\n\\r{#]+)";
          List subListTmp = SourceParser.getElements(text, subExpr, "", "", SourceParser.DELETE_COMMENT | SourceParser.DELETE_POD);          
          subList = new ArrayList();
          // Find closing brackets for subs manually                    int lastBracket=0;
          for (int i = 0; i < subListTmp.size(); i++) {
            Model sub = (Model) subListTmp.get(i);                        // only consider subs which are NOT within other Subs             // and not within a Quote or Remark            if (sub.getStart() > lastBracket &&                 ((PerlEditor) fTextEditor).isNormalText(sub.getStart())                ) {	
	            int bracketPos = text.indexOf("{", sub.getStart()) + 1;              // now let's check if the { is within the next word	            int j = sub.getStart() + sub.getName().length();	            while (j < bracketPos && Character.isWhitespace(text.charAt(j))) {                ++j;              } 	            if (text.charAt(j) == '{') {		            int pos = ((PerlEditor) fTextEditor).findNextOccurance(fSourceViewer.getDocument(), '{', bracketPos);
				            if (pos >= 0	                  && fSourceViewer.getDocument().getLineOfOffset(sub.getStart()) 	                    < 	                     (j= fSourceViewer.getDocument().getLineOfOffset(pos))) {		              //fixes Bug [ 1104489 ] to add as well the next line-feed		              if (j == fSourceViewer.getDocument().getNumberOfLines() ) {		                pos  = fSourceViewer.getDocument().getLineLength( j) + 		                     fSourceViewer.getDocument().getLineOffset( j);		              } else {		                pos  = fSourceViewer.getDocument().getLineOffset( j + 1) - 1;		              }	                // we found something and this the closing bracket is in a new line		              subList.add(new Model(sub.getName(), sub.getStart(), pos	                    - sub.getStart() + 1));		                lastBracket = pos + 1;		              }	            }            }          }
        }

        List completeList = new ArrayList();
        completeList.add(podList);
        completeList.add(subList);

        for (int i = 0; i < completeList.size(); i++) {
          if (!(completeList.get(i) instanceof List)) {
            continue;
          }
          List list = (List) completeList.get(i);

          for (int j = 0; j < list.size(); j++) {
            if (!(list.get(j) instanceof Model)) {
              continue;
            }
            int start = ((Model) list.get(j)).getStart();
            int length = ((Model) list.get(j)).getLength();

            Position position = new Position(start, length);
            //model.addAnnotation(new ProjectionAnnotation(),
            // position);
            positionHash.put(position, ANNOTATION_NEW);
          }

        }

        // Delete unnecessary annotations and mark existig annotations
        for (Iterator i = model.getAnnotationIterator(); i.hasNext();) {
          ProjectionAnnotation annotation = (ProjectionAnnotation) i.next();

          Position pos = model.getPosition(annotation);

          if (positionHash.get(pos) == null) {
            model.removeAnnotation(annotation);
          } else {
            positionHash.put(pos, ANNOTATION_EXISTS);
          }
        }

        // Add new annotations
        for (Iterator it = positionHash.keySet().iterator(); it.hasNext();) {
          Position pos = (Position) it.next();
          Integer val = (Integer) positionHash.get(pos);
          if (val != ANNOTATION_EXISTS) {
            model.addAnnotation(new ProjectionAnnotation(), pos);
          }
        }
      }
    } catch (BadLocationException e1) {
      /*
       * nothing to do, cause LineDel for 0 should be always present and if there is an exception, then we are in a new file, first line
       * with no LineDel
       */
    }
  }
	
	public void dispose() throws InterruptedException {
		this.interrupt();
		this.join();
	}

}