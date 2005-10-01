package org.epic.perleditor.editors;

import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Constants identifying commands contributed by the org.epic.perleditor plug-in.
 * These command ids (aka ActionDefinitionIds) are used by the PerlEditor.
 * 
 * The overall idea is that both user-configurable keyboard shortcuts and
 * plug-in actions are associated with command ids, creating the opportunity
 * of uniform keyboard shortcuts across different plug-ins. However, EPIC's
 * actions are quite specific and thus currently do not match many workbench
 * commands. Instead, EPIC provides its own set of commands, at risk of having
 * the user configure the same keyboard shortcuts twice (e.g. for JDT and EPIC).
 * 
 * @author jploski
 */
public class PerlEditorCommandIds
{
    public static final String CONTENT_ASSIST = ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS;
    public static final String HTML_EXPORT = "org.epic.perleditor.htmlexport";
    public static final String VALIDATE_SYNTAX = "org.epic.perleditor.validatesyntax";
    public static final String FORMAT_SOURCE = "org.epic.perleditor.formatsource";
    public static final String TOGGLE_COMMENT = "org.epic.perleditor.togglecomment";
    public static final String OPEN_SUB = "org.epic.perleditor.openDeclaration";
    public static final String PERL_DOC = "org.epic.perleditor.searchPerlDoc";
    public static final String MATCHING_BRACKET = "org.epic.perleditor.jump2Bracket";
    
    private PerlEditorCommandIds() { }
}
