package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;

import org.epic.perleditor.editors.util.SourceFormatter;
import org.epic.perleditor.editors.PerlEditor;


public class FormatSourceAction extends Action {
	PerlEditor editor = null;
	
	/**
	 * Constructs and updates the action.
	 */
	public FormatSourceAction() {
		super();
	}
	
	
	public void run() {
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		ISourceViewer viewer = editor.getViewer();
		int topIndex =  viewer.getTextWidget().getTopIndex();
		int carretOffset = viewer.getTextWidget().getCaretOffset();
        String text = new SourceFormatter().doConversion(document.get());
        
        if(text != null) {
        	document.set(text);
        	viewer.getTextWidget().setTopIndex(topIndex);
        	viewer.getTextWidget().setCaretOffset(carretOffset);
        	viewer.getTextWidget().redraw();
        	// Re-validate Syntax
        	if(editor != null) {
        		editor.revalidateSyntax(true);
        	}
        }
	}
	
	public void setEditor(PerlEditor editor) {
		this.editor = editor;
	}
	
	
	
	
}
