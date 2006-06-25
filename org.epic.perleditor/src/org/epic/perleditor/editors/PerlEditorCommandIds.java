package org.epic.perleditor.editors;

import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;


/**
 * Constants identifying commands contributed by the org.epic.perleditor plug-in. These command ids
 * (aka ActionDefinitionIds) are used by the PerlEditor.
 *
 * <p>The overall idea is that both user-configurable keyboard shortcuts and plug-in actions are
 * associated with command ids, creating the opportunity of uniform keyboard shortcuts across
 * different plug-ins. However, EPIC's actions are quite specific and thus currently do not match
 * many workbench commands. Instead, EPIC provides its own set of commands, at risk of having the
 * user configure the same keyboard shortcuts twice (e.g. for JDT and EPIC).</p>
 *
 * @author jploski
 */
public class PerlEditorCommandIds
{
    //~ Static fields/initializers

    /** org.epic.perleditor.commands.critiqueSource */
    public static final String CRITIQUE_SOURCE = "org.epic.perleditor.commands.critiqueSource";

    /** org.epic.perleditor.commands.formatSource */
    public static final String FORMAT_SOURCE = "org.epic.perleditor.commands.formatSource";

    /**
     * @see ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS
     */
    public static final String CONTENT_ASSIST =
        ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS;

    /** org.epic.perleditor.commands.htmlExport */
    public static final String HTML_EXPORT = "org.epic.perleditor.commands.htmlExport";

    /** org.epic.perleditor.commands.validateSyntax */
    public static final String VALIDATE_SYNTAX = "org.epic.perleditor.commands.validateSyntax";

    /** org.epic.perleditor.commands.toggleComment */
    public static final String TOGGLE_COMMENT = "org.epic.perleditor.commands.toggleComment";

    /** org.epic.perleditor.commands.openDeclaration */
    public static final String OPEN_SUB = "org.epic.perleditor.commands.openDeclaration";

    /** org.epic.perleditor.commands.searchPerlDoc */
    public static final String PERL_DOC = "org.epic.perleditor.commands.searchPerlDoc";

    /** org.epic.perleditor.jump2Bracket */
    public static final String MATCHING_BRACKET = "org.epic.perleditor.commands.jump2Bracket";

    /** org.epic.perleditor.commands.toggleMarkOccurrences */
    public static final String TOGGLE_MARK_OCCURRENCES =
        "org.epic.perleditor.commands.toggleMarkOccurrences";

    /** org.epic.perleditor.commands.extractSubroutine */
    public static final String EXTRACT_SUBROUTINE =
        "org.epic.perleditor.commands.extractSubroutine";

    //~ Constructors

    private PerlEditorCommandIds()
    {
    }
}
