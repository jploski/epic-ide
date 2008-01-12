package org.epic.perleditor.preferences;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.jface.preference.IPreferenceStore;

import org.epic.perleditor.editors.util.IPerlColorConstants;

/**
 * Constants used as keys for preferences in org.epic.perleditor
 * plugin's preference store. This class is also responsible for
 * setting the "factory defaults" for all preferences.
 */
public class PreferenceConstants
{
    private PreferenceConstants()
    {
    }

    /**
     * A named preference that controls whether the current line highlighting is
     * turned on or off.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_CURRENT_LINE = "currentLine"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to highlight the current
     * line.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public final static String EDITOR_CURRENT_LINE_COLOR = "currentLineColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the print margin is turned on or
     * off.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_PRINT_MARGIN = "printMargin"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the outline view selection
     * should stay in sync with with the element at the current cursor position.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE = "syncOutlineOnCursorMove"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the 'smart home-end' feature is
     * enabled.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_SMART_HOME_END = AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END;

    /**
     * A named preference that controls whether the 'sub-word navigation'
     * feature is enabled.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_SUB_WORD_NAVIGATION = "subWordNavigation"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the 'line wrap' feature is
     * enabled.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_LINE_WRAP = "lineWrap"; //$NON-NLS-1$

    /**
     * A named preference that controls whether source folding is turned on or
     * off.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String SOURCE_FOLDING = "sourceFolding"; //$NON-NLS-1$

    public static final String PERLDOC_FOLDING = "sourceFolding.perldoc"; //$NON-NLS-1$

    public static final String SUBROUTINE_FOLDING = "sourceFolding.subroutine"; //$NON-NLS-1$

    /**
     * A named preference that controls whether auto completion of quotes etc.
     * is turned on or off.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String AUTO_COMPLETION_QUOTE1 = "autoCompletionQuote1"; //$NON-NLS-1$
    public final static String AUTO_COMPLETION_QUOTE2 = "autoCompletionQuote2"; //$NON-NLS-1$
    public final static String AUTO_COMPLETION_BRACKET1 = "autoCompletionBracket1"; //$NON-NLS-1$
    public final static String AUTO_COMPLETION_BRACKET2 = "autoCompletionBracket2"; //$NON-NLS-1$
    public final static String AUTO_COMPLETION_BRACKET3 = "autoCompletionBracket3"; //$NON-NLS-1$
    public final static String AUTO_COMPLETION_BRACKET4 = "autoCompletionBracket4"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render the print margin.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public final static String EDITOR_PRINT_MARGIN_COLOR = "printMarginColor"; //$NON-NLS-1$

    /**
     * Print margin column. Int value.
     */
    public final static String EDITOR_PRINT_MARGIN_COLUMN = "printMarginColumn"; //$NON-NLS-1$

    /**
     * A named preference that holds the number of spaces used per tab in the
     * editor.
     * <p>
     * Value is of type <code>Int</code>: positive int value specifying the
     * number of spaces per tab.
     * </p>
     */
    public final static String EDITOR_TAB_WIDTH = "editorTabWidth"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the editor shows problem
     * indicators in text (squiggly lines).
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_PROBLEM_INDICATION = "errorIndication"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render problem
     * indicators.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see #EDITOR_PROBLEM_INDICATION
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public final static String EDITOR_PROBLEM_INDICATION_COLOR = "errorIndicationColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the editor shows warning
     * indicators in text (squiggly lines).
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_WARNING_INDICATION = "warningIndication"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render warning
     * indicators.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see #EDITOR_WARNING_INDICATION
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     * @since 2.1
     */
    public final static String EDITOR_WARNING_INDICATION_COLOR = "warningIndicationColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the editor shows task indicators
     * in text (squiggly lines).
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_TASK_INDICATION = "taskIndication"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render task indicators.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see #EDITOR_TASK_INDICATION
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     * @since 2.1
     */
    public final static String EDITOR_TASK_INDICATION_COLOR = "taskIndicationColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the editor shows bookmark
     * indicators in text (squiggly lines).
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_BOOKMARK_INDICATION = "bookmarkIndication"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render bookmark
     * indicators.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see #EDITOR_BOOKMARK_INDICATION
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     * @since 2.1
     */
    public final static String EDITOR_BOOKMARK_INDICATION_COLOR = "bookmarkIndicationColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the editor shows search
     * indicators in text (squiggly lines).
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_SEARCH_RESULT_INDICATION = "searchResultIndication"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render search indicators.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see #EDITOR_SEARCH_RESULT_INDICATION
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     * @since 2.1
     */
    public final static String EDITOR_SEARCH_RESULT_INDICATION_COLOR = "searchResultIndicationColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the editor shows unknown
     * indicators in text (squiggly lines).
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_UNKNOWN_INDICATION = "othersIndication"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render unknown
     * indicators.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see #EDITOR_UNKNOWN_INDICATION
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     * @since 2.1
     */
    public final static String EDITOR_UNKNOWN_INDICATION_COLOR = "othersIndicationColor"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the overview ruler shows error
     * indicators.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER = "errorIndicationInOverviewRuler"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the overview ruler shows warning
     * indicators.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER = "warningIndicationInOverviewRuler"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the overview ruler shows task
     * indicators.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER = "taskIndicationInOverviewRuler"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the overview ruler shows
     * bookmark indicators.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER = "bookmarkIndicationInOverviewRuler"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the overview ruler shows search
     * result indicators.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER = "searchResultIndicationInOverviewRuler"; //$NON-NLS-1$

    /**
     * A named preference that controls whether the overview ruler shows unknown
     * indicators.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * 
     * @since 2.1
     */
    public final static String EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER = "othersIndicationInOverviewRuler"; //$NON-NLS-1$

    /**
     * A named preference that controls if the overview ruler is shown in the
     * UI.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_OVERVIEW_RULER = "overviewRuler"; //$NON-NLS-1$

    /**
     * A named preference that controls if the line number ruler is shown in the
     * UI.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_LINE_NUMBER_RULER = "lineNumberRuler"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render line numbers
     * inside the line number ruler.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     * @see #EDITOR_LINE_NUMBER_RULER
     */
    public final static String EDITOR_LINE_NUMBER_RULER_COLOR = "lineNumberColor"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to render linked positions
     * inside code templates.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public final static String EDITOR_LINKED_POSITION_COLOR = "linkedPositionColor"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used as the text foreground. This
     * value has not effect if the system default color is used.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public final static String EDITOR_FOREGROUND_COLOR = AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND;

    /**
     * A named preference that describes if the system default foreground color
     * is used as the text foreground.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_FOREGROUND_DEFAULT_COLOR = AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT;

    /**
     * A named preference that holds the color used as the text background. This
     * value has not effect if the system default color is used.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public final static String EDITOR_BACKGROUND_COLOR = AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND;

    /**
     * A named preference that describes if the system default background color
     * is used as the text background.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_BACKGROUND_DEFAULT_COLOR = AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT;

    public final static String INSERT_TABS_ON_INDENT = "insertTabOnIndent"; //$NON-NLS-1$

    public final static String SPACES_INSTEAD_OF_TABS = "spacesInsteadOfTabs"; //$NON-NLS-1$

    /**
     * Preference key suffix for bold text style preference keys.
     * 
     * @since 2.1
     */
    public static final String EDITOR_BOLD_SUFFIX = "Bold";

    public static final String EDITOR_STRING_COLOR = IPerlColorConstants.STRING_COLOR;
    public static final String EDITOR_KEYWORD1_COLOR = IPerlColorConstants.KEYWORD1_COLOR;
    public static final String EDITOR_KEYWORD2_COLOR = IPerlColorConstants.KEYWORD2_COLOR;
    public static final String EDITOR_VARIABLE_COLOR = IPerlColorConstants.VARIABLE_COLOR;
    public static final String EDITOR_COMMENT1_COLOR = IPerlColorConstants.COMMENT1_COLOR;
    public static final String EDITOR_COMMENT2_COLOR = IPerlColorConstants.COMMENT2_COLOR;
    public static final String EDITOR_LITERAL1_COLOR = IPerlColorConstants.LITERAL1_COLOR;
    public static final String EDITOR_LITERAL2_COLOR = IPerlColorConstants.LITERAL2_COLOR;
    public static final String EDITOR_LABEL_COLOR = IPerlColorConstants.LABEL_COLOR;
    public static final String EDITOR_FUNCTION_COLOR = IPerlColorConstants.FUNCTION_COLOR;
    public static final String EDITOR_MARKUP_COLOR = IPerlColorConstants.MARKUP_COLOR;
    public static final String EDITOR_OPERATOR_COLOR = IPerlColorConstants.OPERATOR_COLOR;
    public static final String EDITOR_NUMBER_COLOR = IPerlColorConstants.NUMBER_COLOR;
    public static final String EDITOR_INVALID_COLOR = IPerlColorConstants.INVALID_COLOR;

    public static final String EDITOR_STRING_COLOR_BOLD = IPerlColorConstants.STRING_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_KEYWORD1_COLOR_BOLD = IPerlColorConstants.KEYWORD1_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_KEYWORD2_COLOR_BOLD = IPerlColorConstants.KEYWORD2_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_VARIABLE_COLOR_BOLD = IPerlColorConstants.VARIABLE_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_COMMENT1_COLOR_BOLD = IPerlColorConstants.COMMENT1_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_COMMENT2_COLOR_BOLD = IPerlColorConstants.COMMENT2_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_LITERAL1_COLOR_BOLD = IPerlColorConstants.LITERAL1_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_LITERAL2_COLOR_BOLD = IPerlColorConstants.LITERAL2_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_LABEL_COLOR_BOLD = IPerlColorConstants.LABEL_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_FUNCTION_COLOR_BOLD = IPerlColorConstants.FUNCTION_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_MARKUP_COLOR_BOLD = IPerlColorConstants.MARKUP_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_OPERATOR_COLOR_BOLD = IPerlColorConstants.OPERATOR_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_NUMBER_COLOR_BOLD = IPerlColorConstants.NUMBER_COLOR
        + EDITOR_BOLD_SUFFIX;
    public static final String EDITOR_INVALID_COLOR_BOLD = IPerlColorConstants.INVALID_COLOR
        + EDITOR_BOLD_SUFFIX;

    /**
     * A named preference that controls whether bracket matching highlighting is
     * turned on or off.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets"; //$NON-NLS-1$

    /**
     * A named preference that holds the color used to highlight matching
     * brackets.
     * <p>
     * Value is of type <code>String</code>. A RGB color value encoded as a
     * string using class <code>PreferenceConverter</code>
     * </p>
     * 
     * @see org.eclipse.jface.resource.StringConverter
     * @see org.eclipse.jface.preference.PreferenceConverter
     */
    public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_ENABLED = "sourceCriticEnabled"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_DEFAULT_LOCATION = "sourceCriticDefaultLocation"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_LOCATION = "sourceCriticLocation"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_SEVERITY_LEVEL = "sourceCriticLevel"; //$NON-NLS-1$
    
    
    private static final Object[] DEFAULT_COLORS = {
        PreferenceConstants.EDITOR_CURRENT_LINE_COLOR, new RGB(225, 235, 224),
        PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR, new RGB(176, 180, 185), 
        PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR, new RGB(255, 0, 128),
        PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR, new RGB(244, 200, 45),
        PreferenceConstants.EDITOR_TASK_INDICATION_COLOR, new RGB(0, 128, 255),
        PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR, new RGB(34, 164, 99),
        PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR, new RGB(192, 192, 192),
        PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR, new RGB(0, 0, 0),
        PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR, new RGB(0, 0, 0),
        PreferenceConstants.EDITOR_LINKED_POSITION_COLOR, new RGB(0, 200, 100),
        PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, new RGB(192, 192, 192),
        
        PreferenceConstants.EDITOR_STRING_COLOR, new RGB(0, 0, 0),
        PreferenceConstants.EDITOR_KEYWORD1_COLOR, new RGB(160, 32, 240),
        PreferenceConstants.EDITOR_KEYWORD2_COLOR, new RGB(160, 0, 240),
        PreferenceConstants.EDITOR_VARIABLE_COLOR, new RGB(160, 0, 240),
        PreferenceConstants.EDITOR_COMMENT1_COLOR, new RGB(178, 0, 34),
        PreferenceConstants.EDITOR_COMMENT2_COLOR, new RGB(178, 34, 0),
        PreferenceConstants.EDITOR_LITERAL1_COLOR, new RGB(0, 0, 255),
        PreferenceConstants.EDITOR_LITERAL2_COLOR, new RGB(160, 32, 240),
        PreferenceConstants.EDITOR_LABEL_COLOR, new RGB(160, 0, 240),
        PreferenceConstants.EDITOR_FUNCTION_COLOR, new RGB(160, 32, 0),
        PreferenceConstants.EDITOR_MARKUP_COLOR, new RGB(178, 0, 34),
        PreferenceConstants.EDITOR_OPERATOR_COLOR, new RGB(178, 34, 0),
        PreferenceConstants.EDITOR_NUMBER_COLOR, new RGB(160, 32, 0),
        PreferenceConstants.EDITOR_INVALID_COLOR, new RGB(178, 0, 34),
        };

    /**
     * Initializes the given preference store with the default values.
     * 
     * @param store  the preference store to be initialized
     */
    public static void initializeDefaultValues(IPreferenceStore store)
    {
        store.setDefault(PreferenceConstants.EDITOR_CURRENT_LINE, true);
        store.setDefault(PreferenceConstants.EDITOR_PRINT_MARGIN, false);
        store.setDefault(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, 80);
        store.setDefault(PreferenceConstants.EDITOR_PROBLEM_INDICATION, true);
        store.setDefault(PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER, true);
        store.setDefault(PreferenceConstants.EDITOR_WARNING_INDICATION, true);
        store.setDefault(PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER, true);
        store.setDefault(PreferenceConstants.EDITOR_TASK_INDICATION, false);
        store.setDefault(PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER, true);
        store.setDefault(PreferenceConstants.EDITOR_BOOKMARK_INDICATION, false);
        store.setDefault(PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER, true);
        store.setDefault(PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION, true);
        store.setDefault(PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER, true);
        store.setDefault(PreferenceConstants.EDITOR_UNKNOWN_INDICATION, false);
        store.setDefault(PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER, false);
        store.setDefault(PreferenceConstants.EDITOR_OVERVIEW_RULER, true);
        store.setDefault(PreferenceConstants.EDITOR_LINE_NUMBER_RULER, false);
        store.setDefault(PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR, true);
        store.setDefault(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR, true);
        store.setDefault(PreferenceConstants.EDITOR_TAB_WIDTH, 4);
        store.setDefault(PreferenceConstants.EDITOR_SMART_HOME_END, true);
        store.setDefault(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION, true);
        store.setDefault(PreferenceConstants.EDITOR_LINE_WRAP, false);
        store.setDefault(PreferenceConstants.INSERT_TABS_ON_INDENT, 1);
        store.setDefault(PreferenceConstants.SPACES_INSTEAD_OF_TABS, false);
        store.setDefault(PreferenceConstants.SOURCE_FOLDING, true);
        store.setDefault(PreferenceConstants.PERLDOC_FOLDING, false);
        store.setDefault(PreferenceConstants.SUBROUTINE_FOLDING, false);
        store.setDefault(PreferenceConstants.SOURCE_CRITIC_ENABLED, false);
        store.setDefault(PreferenceConstants.SOURCE_CRITIC_DEFAULT_LOCATION, true);
        store.setDefault(PreferenceConstants.SOURCE_CRITIC_LOCATION, "");
        store.setDefault(PreferenceConstants.AUTO_COMPLETION_QUOTE1, true);
        store.setDefault(PreferenceConstants.AUTO_COMPLETION_QUOTE2, true);
        store.setDefault(PreferenceConstants.AUTO_COMPLETION_BRACKET1, true);
        store.setDefault(PreferenceConstants.AUTO_COMPLETION_BRACKET2, true);
        store.setDefault(PreferenceConstants.AUTO_COMPLETION_BRACKET3, true);
        store.setDefault(PreferenceConstants.AUTO_COMPLETION_BRACKET4, true);
        store.setDefault(PreferenceConstants.EDITOR_MATCHING_BRACKETS, true);

        for (int i = 0; i < DEFAULT_COLORS.length; i += 2)
        {
            PreferenceConverter.setDefault(
                store,
                (String) DEFAULT_COLORS[i],
                (RGB) DEFAULT_COLORS[i+1]);
        }
        
        store.setDefault(PreferenceConstants.EDITOR_STRING_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_KEYWORD1_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_KEYWORD2_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_VARIABLE_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_COMMENT1_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_COMMENT2_COLOR_BOLD, true);
        store.setDefault(PreferenceConstants.EDITOR_LITERAL1_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_LITERAL2_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_LABEL_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_FUNCTION_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_MARKUP_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_OPERATOR_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_NUMBER_COLOR_BOLD, false);
        store.setDefault(PreferenceConstants.EDITOR_INVALID_COLOR_BOLD, false);
    }
}
