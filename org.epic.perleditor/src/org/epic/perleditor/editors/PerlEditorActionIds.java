package org.epic.perleditor.editors;

/**
 * Constants identifying actions contributed by the org.epic.perleditor plug-in.
 * These action ids are shared by the plug-in manifest, PerlActionContributor,
 * PerlEditor and PerlEditorAction.
 * 
 * @author jploski
 */
public class PerlEditorActionIds
{
    public static final String CONTENT_ASSIST = "org.epic.perleditor.ContentAssist";
    public static final String HTML_EXPORT = "org.epic.perleditor.HtmlExport";
    public static final String VALIDATE_SYNTAX = "org.epic.perleditor.ValidateSyntax";
    public static final String FORMAT_SOURCE = "org.epic.perleditor.FormatSource";
    public static final String TOGGLE_COMMENT = "org.epic.perleditor.ToggleComment";
    public static final String OPEN_SUB = "org.epic.perleditor.OpenSubAction";
    public static final String PERL_DOC = "org.epic.perleditor.PerlDocAction";
    public static final String MATCHING_BRACKET = "org.epic.perleditor.Jump2Bracket";
    public static final String TOGGLE_MARK_OCCURRENCES = "org.epic.perleditor.ToggleMarkOccurrencesAction";
    
    private PerlEditorActionIds() { }
    
    /**
     * @return IDs of all {@link org.epic.perleditor.actions.PerlEditorAction}s
     */
    public static final String[] get()
    {
        return new String[] {
            HTML_EXPORT,
            VALIDATE_SYNTAX,
            FORMAT_SOURCE,
            TOGGLE_COMMENT,
            OPEN_SUB,
            PERL_DOC,
            MATCHING_BRACKET
            };
    }
}
