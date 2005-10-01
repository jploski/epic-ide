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
    public static final String OPEN_SUB = "org.epic.perleditor.popupmenus.OpenSubAction";
    public static final String PERL_DOC = "org.epic.perleditor.popupmenus.PerlDocAction";
    public static final String MATCHING_BRACKET = "org.epic.perleditor.Jump2Bracket";
    
    private PerlEditorActionIds() { }
}
