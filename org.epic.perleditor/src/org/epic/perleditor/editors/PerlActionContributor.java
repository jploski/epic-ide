package org.epic.perleditor.editors;


import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditorActionContributor;

import org.epic.perleditor.actions.ExportHtmlSourceAction;
import org.epic.perleditor.actions.FormatSourceAction;


/**
 * Contributes interesting Java actions to the desktop's Edit menu and the toolbar.
 */
public class PerlActionContributor extends TextEditorActionContributor {

	protected FormatSourceAction formatSourceAction;
	protected ExportHtmlSourceAction htmExportAction;

	/**
	 * Default constructor.
	 */
	public PerlActionContributor() {
		super();
		formatSourceAction = new FormatSourceAction();
		htmExportAction = new ExportHtmlSourceAction();
		//formatSourceAction.setActionDefinitionId("org.epic.perledior.formatsource");
	}

	/*
	 * @see IEditorActionBarContributor#init(IActionBars)
	 */
	public void init(IActionBars bars) {
		super.init(bars);
	}

	/*
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		//doSetActiveEditor(part);
		
		PerlEditor editor = null;
		if (part instanceof PerlEditor) {
				editor = (PerlEditor) part;
				
				// Only add handlers if in Perl mode
				if(editor.isPerlMode()) {
					formatSourceAction.setActionDefinitionId("org.epic.perledior.formatsource");
					formatSourceAction.setEditor(editor);
					
					htmExportAction.setActionDefinitionId("org.epic.perleditor.htmlexport");
				}
						
				IActionBars bars= getActionBars();
				bars.setGlobalActionHandler("org.epic.perleditor.ContentAssist", getAction(editor, "org.epic.perleditor.ContentAssist"));
				bars.setGlobalActionHandler("org.epic.perleditor.Comment", getAction(editor, "org.epic.perleditor.Comment"));
				bars.setGlobalActionHandler("org.epic.perleditor.Uncomment", getAction(editor, "org.epic.perleditor.Uncomment"));
				
				if(editor.isPerlMode()) {
					bars.setGlobalActionHandler("org.epic.perleditor.FormatSource", formatSourceAction);
					bars.setGlobalActionHandler("org.epic.perleditor.HtmlExport", htmExportAction);
				}
				else {
					bars.setGlobalActionHandler("org.epic.perleditor.FormatSource", null);
					bars.setGlobalActionHandler("org.epic.perleditor.HtmlExport", null);
				}
		}
				
		
	}

	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {
		formatSourceAction.setEditor(null);
		super.dispose();
	}
}
