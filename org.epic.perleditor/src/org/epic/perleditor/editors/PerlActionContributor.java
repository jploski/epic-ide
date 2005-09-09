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
import org.epic.perleditor.popupmenus.PerlDocAction;


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
	protected PerlDocAction perldocAction;

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
		perldocAction = new PerlDocAction();
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
		
        formatSourceAction.setActiveEditor(formatSourceAction, part);
        validateSourceAction.setActiveEditor(validateSourceAction, part);
        openDeclarationAction.setActiveEditor(openDeclarationAction, part);
        toggleCommentAction.setActiveEditor(toggleCommentAction, part);
        jump2BracketAction.setActiveEditor(jump2BracketAction, part);
        perldocAction.setActiveEditor(perldocAction, part);
        
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
					bars.setGlobalActionHandler("org.epic.perleditor.popupmenus.PerlDocAction", perldocAction);
				}
				else {
					bars.setGlobalActionHandler("org.epic.perleditor.FormatSource", null);
					bars.setGlobalActionHandler("org.epic.perleditor.HtmlExport", null);
					bars.setGlobalActionHandler("org.epic.perleditor.ValidateSyntax", null);
					bars.setGlobalActionHandler("org.epic.perleditor.popupmenus.OpenSubAction", null);
					bars.setGlobalActionHandler("org.epic.perleditor.ToggleComment", null);
					bars.setGlobalActionHandler("org.epic.perleditor.Jump2Bracket", null);
					bars.setGlobalActionHandler("org.epic.perleditor.popupmenus.PerlDocAction", null);
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
