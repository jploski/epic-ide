package org.epic.perleditor.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
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
	  IAnnotationModel model = (IAnnotationModel) fTextEditor
	  .getAdapter(ProjectionAnnotationModel.class);
	  
	  String lineSep = null;
	  try {
	    lineSep = fSourceViewer.getDocument().getLineDelimiter(0);
	    HashMap positionHash = new HashMap();
	    
	    if (model != null) {
	      // Remove all annotations
	      //			for (Iterator i = model.getAnnotationIterator(); i.hasNext();) {
	      //				ProjectionAnnotation annotation = (ProjectionAnnotation) i
	      //						.next();
	      //				model.removeAnnotation(annotation);
	      //			}
	      
	      IPreferenceStore store = PerlEditorPlugin.getDefault()
	      .getPreferenceStore();
	      
	      boolean foldingEnabled = store
	      .getBoolean(PreferenceConstants.SOURCE_FOLDING);
	      
	      List podList = new ArrayList();
	      List subList = new ArrayList();
	      
	      if (foldingEnabled) {

	        // Get all pod comments
	        String podExpr = "^(=head.*(" + lineSep + ".*)+?" + lineSep
	        + "=cut)$";
	        podList = SourceParser.getElements(text, podExpr, "", "",
	            SourceParser.DO_NOT_DELETE_COMMENT_POD);
	        
	        // Get all subroutines
	        //String subExpr = "^\\s*sub\\s+(.*{.*(" + lineSep +
	        // ".*)+?.*\\n})";
	        String subExpr = "^[\\s]*(sub\\s+[^\\n\\r{]+)";
	        List subListTmp = SourceParser.getElements(text, subExpr, "",
	            "", SourceParser.DELETE_COMMENT
	            | SourceParser.DELETE_POD);
	        subList = new ArrayList();
	        
	        // Find closing brackets for subs manually
	        for (int i = 0; i < subListTmp.size(); i++) {
	         try {
	            Model sub = (Model) subListTmp.get(i);
//	            int pos = searchForClosingBracket(sub.getStart()
//	                + sub.getLength() + 1, '{', '}', fSourceViewer
//	                .getDocument());

	            // Make sure to get the starting bracket
	            FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(fSourceViewer.getDocument());
	            IRegion region =finder.find(sub.getStart(), "{", true, false, false, false);
	            int pos = ((PerlEditor)fTextEditor).findNextOccurance(fSourceViewer.getDocument(), '{', region.getOffset() +1);

	            if (pos != -1) {
	              subList.add(new Model(sub.getName(),
	                  sub.getStart(), pos - sub.getStart() + 1));
	            }
	            
	          } catch (BadLocationException e) {
	            
	         }
	          
	        }
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
	      
	    }  /* LeO: macht das Ende hier einen Sinn? model kann ja auch null sein! (zumindest theoretisch und 
	    * praktisch wenn man was falsch ausprogrammiert hat!
	    */
	    
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
	    
	  } catch (BadLocationException e1) {
	    /* nothing to do, cause LineDel for 0 should be always present
	     * and if there is an exception, then we are in a new file, first line with no LineDel
	     */ 
	  }
	}

	/**
	 * Returns the position of the closing bracket after startPosition.
	 * 
	 * @returns the location of the closing bracket.
	 * @param startPosition -
	 *            the beginning position
	 * @param openBracket -
	 *            the character that represents the open bracket
	 * @param closeBracket -
	 *            the character that represents the close bracket
	 * @param document -
	 *            the document being searched
	 */
	protected int searchForClosingBracket(int startPosition, char openBracket,
			char closeBracket, IDocument document) throws BadLocationException {
		int stack = 1;
		int closePosition = startPosition + 1;
		int length = document.getLength();
		char nextChar;

		while (closePosition < length && stack > 0) {
			nextChar = document.getChar(closePosition);
			if (nextChar == openBracket && nextChar != closeBracket)
				stack++;
			else if (nextChar == closeBracket)
				stack--;
			closePosition++;
		}

		if (stack == 0)
			return closePosition;
		else
			return -1;

	}
	

	public void dispose() throws InterruptedException {
		this.interrupt();
		this.join();
	}

}