/*
 * This file is a duplicate of org.epic.perleditor.preferences.PerlEditorPreferencePage
 * to allow the use of the Colorer plugin
 * Changes are marked with todo tags!
 */
package org.epic.ext.unsupported.colorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;

import org.epic.perleditor.editors.util.SourceFormatter;


public class FormatSourceAction extends Action {
	PerlEditor editor = null;
	
	/**
	 * Constructs and updates the action.
	 */
	public FormatSourceAction(String label) {
		super(label);
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
        	
        		// TODO update colorer
        		editor.relinkColorer();
       		}
        }
	}
	
	public void setEditor(PerlEditor editor) {
		this.editor = editor;
	}
	
	
	
	
}
