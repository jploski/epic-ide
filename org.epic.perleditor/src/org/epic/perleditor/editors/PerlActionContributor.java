package org.epic.perleditor.editors;


import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditorActionContributor;

import org.epic.perleditor.actions.ExportHtmlSourceAction;
import org.epic.perleditor.actions.FormatSourceAction;
import org.epic.perleditor.actions.Jump2BracketAction;
import org.epic.perleditor.actions.ToggleCommentAction;
import org.epic.perleditor.actions.ValidateSourceAction;
import org.epic.perleditor.popupmenus.OpenDeclaration;


/**
 * Contributes interesting Java actions to the desktop's Edit menu and the toolbar.
 */
public class PerlActionContributor extends TextEditorActionContributor {

	protected FormatSourceAction formatSourceAction;
	protected ExportHtmlSourceAction htmExportAction;
	protected ValidateSourceAction validateSourceAction;
	protected OpenDeclaration openDeclarationAction;
	protected ToggleCommentAction toggleCommentAction;
	protected Jump2BracketAction jump2BracketAction;

	/**
	 * Default constructor.
	 */
	public PerlActionContributor() {
		super();
		formatSourceAction = new FormatSourceAction();
		htmExportAction = new ExportHtmlSourceAction();
		validateSourceAction = new ValidateSourceAction();
		openDeclarationAction = new OpenDeclaration();
		toggleCommentAction = new ToggleCommentAction();
		jump2BracketAction = new Jump2BracketAction();
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

				
				if(editor.isPerlMode()) {
					bars.setGlobalActionHandler("org.epic.perleditor.ToggleComment", toggleCommentAction);
					bars.setGlobalActionHandler("org.epic.perleditor.FormatSource", formatSourceAction);
					bars.setGlobalActionHandler("org.epic.perleditor.HtmlExport", htmExportAction);
					bars.setGlobalActionHandler("org.epic.perleditor.ValidateSyntax", validateSourceAction);
					bars.setGlobalActionHandler("org.epic.perleditor.popupmenus.OpenSubAction", openDeclarationAction);
					bars.setGlobalActionHandler("org.epic.perleditor.Jump2Bracket", jump2BracketAction);
				}
				else {
					bars.setGlobalActionHandler("org.epic.perleditor.FormatSource", null);
					bars.setGlobalActionHandler("org.epic.perleditor.HtmlExport", null);
					bars.setGlobalActionHandler("org.epic.perleditor.ValidateSyntax", null);
					bars.setGlobalActionHandler("org.epic.perleditor.popupmenus.OpenSubAction", null);
					bars.setGlobalActionHandler("org.epic.perleditor.ToggleComment", null);
					bars.setGlobalActionHandler("org.epic.perleditor.Jump2Bracket", null);
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
