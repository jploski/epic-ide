package org.epic.perleditor.actions;

import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public interface IPerlEditorActionDefinitionIds extends ITextEditorActionDefinitionIds {
	/**
	 * Action definition ID of the source -> comment action
	 * (value <code>"org.epic.perleditor.comment"</code>).
	 */
	public static final String COMMENT= "org.epic.perleditor.comment";
	
	/**
	 * Action definition ID of the source -> uncomment action
	 * (value <code>"org.epic.perleditor.uncomment"</code>).
	 */
	public static final String UNCOMMENT= "org.epic.perleditor.uncomment";
}