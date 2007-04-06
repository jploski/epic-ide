package org.epic.perleditor.editors;

/**
 * Constants identifying actions contributed by the org.epic.perleditor plug-in. These action ids
 * are shared by the plug-in manifest, PerlActionContributor, PerlEditor and PerlEditorAction.
 *
 *@author jploski
 */
public class PerlEditorActionIds
{
    //~ Static fields/initializers

    /** org.epic.perleditor.actions.ClearMarkerAction#All */
    public static final String CLEAR_ALL_MARKERS =
        "org.epic.perleditor.actions.ClearMarkerAction#AllMarkers";

    /** org.epic.perleditor.actions.ClearMarkerAction#PodChecker */
    public static final String CLEAR_POD_MARKERS =
        "org.epic.perleditor.actions.ClearMarkerAction#PodChecker";

    /** org.epic.perleditor.actions.ClearMarkerAction#Critic */
    public static final String CLEAR_CRITIC_MARKERS =
        "org.epic.perleditor.actions.ClearMarkerAction#Critic";

    /** org.epic.perleditor.actions.PodCheckerAction */
    public static final String POD_CHECKER = "org.epic.perleditor.actions.PodCheckerAction";

    /** org.epic.perleditor.actions.PerlCriticAction */
    public static final String PERL_CRITIC = "org.epic.perleditor.actions.PerlCriticAction";

    /** org.epic.perleditor.actions.FormatSourceAction */
    public static final String FORMAT_SOURCE = "org.epic.perleditor.actions.FormatSourceAction";

    /** org.epic.perleditor.actions.HtmlExportAction */
    public static final String HTML_EXPORT = "org.epic.perleditor.actions.ExportHtmlAction";

    /** org.epic.perleditor.actions.ContentAssistAction */
    public static final String CONTENT_ASSIST = "org.epic.perleditor.actions.ContentAssistAction";

    /** org.epic.perleditor.actions.ValidateSyntaxAction */
    public static final String VALIDATE_SYNTAX = "org.epic.perleditor.actions.ValidateSyntaxAction";

    /** org.epic.perleditor.actions.ToggleCommentAction */
    public static final String TOGGLE_COMMENT = "org.epic.perleditor.actions.ToggleCommentAction";

    /** org.epic.perleditor.actions.OpenDeclarationAction */
    public static final String OPEN_DECLARATION =
        "org.epic.perleditor.actions.OpenDeclarationAction";

    /** org.epic.perleditor.actions.PerlDocAction */
    public static final String PERL_DOC = "org.epic.perleditor.actions.PerlDocAction";

    /** org.epic.perleditor.actions.Jump2BracketAction */
    public static final String MATCHING_BRACKET = "org.epic.perleditor.actions.Jump2BracketAction";

    /** org.epic.perleditor.actions.ToggleMarkOccurrencesAction */
    public static final String TOGGLE_MARK_OCCURRENCES =
        "org.epic.perleditor.actions.ToggleMarkOccurrencesAction";

    /** org.epic.perleditor.commands.extractSubroutine */
    public static final String EXTRACT_SUBROUTINE =
        "org.epic.perleditor.actions.ExtractSubroutineAction";

    //~ Constructors

    private PerlEditorActionIds()
    {
        // empty impl
    }

    //~ Methods

    /**
     * @return Ids of all {@link org.epic.perleditor.actions.PerlEditorAction PerlEditorAction}s
     *         that are owned by the
     *         {@link org.epic.perleditor.editors.PerlActionContributor PerlActionContributor}
     */
    public static final String[] getContributorActions()
    {
        return new String[] { TOGGLE_MARK_OCCURRENCES };
    }

    /**
     * @return Ids of all {@link org.epic.perleditor.actions.PerlEditorAction}s that are owned by
     *         the {@link org.epic.perleditor.editors.PerlEditor PerlEditor}
     */
    public static final String[] getEditorActions()
    {
        return new String[]
            {
                CLEAR_ALL_MARKERS,
                CLEAR_POD_MARKERS,
                CLEAR_CRITIC_MARKERS,
                POD_CHECKER,
                CONTENT_ASSIST,
                PERL_CRITIC,
                EXTRACT_SUBROUTINE,
                FORMAT_SOURCE,
                HTML_EXPORT,
                MATCHING_BRACKET,
                OPEN_DECLARATION,
                PERL_DOC,
                TOGGLE_COMMENT,
                VALIDATE_SYNTAX
            };
    }
}
