package org.epic.perleditor.preferences;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.jface.preference.IPreferenceStore;

import org.epic.perleditor.editors.util.IPerlColorConstants;

/**
 * Constants used as keys for preferences in org.epic.perleditor plugin's
 * preference store. This class is also responsible for setting the
 * "factory defaults" for all preferences.
 */
public class PreferenceConstants
{
    private PreferenceConstants()
    {
    }

    /**
     * Initial URL displayed in the Browser view
     */
    public static final String BROWSER_START_URL = "WEB_BROWSER";
    
    /**
     * Comma-separated list of hash keys to include first in the content
     * preview of Variables view, useful for ensuring that keys such as
     * "id", "name" etc. are visible at a glance.
     */
    public static final String DEBUG_PREVIEW_KEYS = "PREVIEW_KEYS";

    /**
     * Whether or not the experimental debug console should be enabled when
     * running Perl scripts
     */
    public static final String DEBUG_DEBUG_CONSOLE = "ENABLE_DEBUG_CONSOLE";

    /**
     * Perl interpreter type (Cygwin or standard)
     */
    public static final String DEBUG_INTERPRETER_TYPE = "INTERPRETER_TYPE";

    /**
     * One of the allowed values for DEBUG_INTERPRETER_TYPE
     */
    public static final String DEBUG_INTERPRETER_TYPE_STANDARD = "Standard";

    /**
     * One of the allowed values for DEBUG_INTERPRETER_TYPE
     */
    public static final String DEBUG_INTERPRETER_TYPE_CYGWIN = "Cygwin";

    /**
     * Path to the Perl interpreter
     */
    public static final String DEBUG_PERL_EXECUTABLE = "PERL_EXECUTABLE";

    /**
     * Whether or not the -w option should be passed to Perl during syntax
     * validation and script execution
     */
    public static final String DEBUG_SHOW_WARNINGS = "SHOW_WARNINGS";

    /**
     * Whether or not the lexer should parse 'func' and 'method' as subroutine keywords (from Method::Signatures and the like)
     */
    public static final String DEBUG_METHOD_SIGNATURES = "METHOD_SIGNATURES";

    /**
     * Whether or not the debugger should suspend at the first executable
     * statement
     */
    public static final String DEBUG_SUSPEND_AT_FIRST = "SUSPEND_AT_FIRST_CONSOLE";

    /**
     * Whether or not the -T option should be passed to Perl during syntax
     * checking and script execution
     */
    public static final String DEBUG_TAINT_MODE = "USE_TAINT_MODE";

    /**
     * Whether or not updated variables should be highlighted in the Variables
     * debug view
     */
    public static final String DEBUG_HIGHLIGHT_UPDATED_VARS = "highlightUpdatedVars";

    /**
     * Whether or not "global" (package) variables should be displayed in the
     * Variables debug view
     */
    public static final String DEBUG_SHOW_GLOBAL_VARS = "showGlobalVars";

    /**
     * Whether or not Perl internal (built-in) variables should be displayed in
     * the Variables debug view
     */
    public static final String DEBUG_SHOW_INTERNAL_VARS = "showInternalVars";

    /**
     * Whether or not "local" (lexical) variables should be displayed in the
     * Variables debug view
     */
    public static final String DEBUG_SHOW_LOCAL_VARS = "showLocalVars";

    /**
     * Whether or not memory addresses of variables should be displayed in the
     * Variables debug view
     */
    public static final String DEBUG_SHOW_VARS_ADDRESS = "showVarsAddress";

    /**
     * A named preference that holds the background color of the vertical ruler
     * in which fold icons are displayed.
     */
    public final static String EDITOR_FOLD_COLUMN_BG_COLOR = "foldColumnBackground"; //$NON-NLS-1$
    public final static RGB EDITOR_FOLD_COLUMN_BG_COLOR_DEFAULT = new RGB(255,
        255, 255);

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
     * Whether or not syntax validation should be performed in the editor
     */
    public static final String EDITOR_SYNTAX_VALIDATION = "SYNTAX_VALIDATION_PREFERENCE"; //$NON-NLS-1$

    /**
     * Inactivity duration (in milliseconds) after which syntax validation is
     * triggered in the editor (if it is enabled)
     */
    public static final String EDITOR_SYNTAX_VALIDATION_INTERVAL = "SYNTAX_VALIDATION_IDLE_INTERVAL"; //$NON-NLS-1$

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
     * A named preference that controls outline view initial module collapse
     * behavior
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public static final String OUTLINE_MODULE_FOLDING = "Outline.moduleFolding"; // $NON-NLS-1$

    /**
     * A named preference that controls outline views subroutine collapse
     * behavior
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public static final String OUTLINE_SUBROUTINE_FOLDING = "Outline.SubFolding"; // $NON-NLS-1$;

    /**
     * A named preference that controls outline view sort order
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public static final String OUTLINE_SORT = "outline.sort"; // $NON-NLS-1$

    /**
     * A named preference that controls outline view collapse behaviour
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public static final String OUTLINE_COLLAPSE_ALL = "PerlOutlinePage.CollapseAllAction"; // $NON-NLS-1$

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
     * A named preference that controls if the overview ruler is shown in the
     * UI.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    public final static String EDITOR_OVERVIEW_RULER = "overviewRuler"; //$NON-NLS-1$

    public final static String INSERT_TABS_ON_INDENT = "insertTabOnIndent"; //$NON-NLS-1$

    public final static String SPACES_INSTEAD_OF_TABS = "spacesInsteadOfTabs"; //$NON-NLS-1$

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

    /**
     * Preference key suffix for bold text style preference keys.
     */
    public static final String EDITOR_BOLD_SUFFIX = "Bold";

    /**
     * Preference key suffix for italic text style preference keys.
     */
    public static final String EDITOR_ITALIC_SUFFIX = "Italic";

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

    public static final String EDITOR_STRING_COLOR_ITALIC = IPerlColorConstants.STRING_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_KEYWORD1_COLOR_ITALIC = IPerlColorConstants.KEYWORD1_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_KEYWORD2_COLOR_ITALIC = IPerlColorConstants.KEYWORD2_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_VARIABLE_COLOR_ITALIC = IPerlColorConstants.VARIABLE_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_COMMENT1_COLOR_ITALIC = IPerlColorConstants.COMMENT1_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_COMMENT2_COLOR_ITALIC = IPerlColorConstants.COMMENT2_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_LITERAL1_COLOR_ITALIC = IPerlColorConstants.LITERAL1_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_LITERAL2_COLOR_ITALIC = IPerlColorConstants.LITERAL2_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_LABEL_COLOR_ITALIC = IPerlColorConstants.LABEL_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_FUNCTION_COLOR_ITALIC = IPerlColorConstants.FUNCTION_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_MARKUP_COLOR_ITALIC = IPerlColorConstants.MARKUP_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_OPERATOR_COLOR_ITALIC = IPerlColorConstants.OPERATOR_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_NUMBER_COLOR_ITALIC = IPerlColorConstants.NUMBER_COLOR
        + EDITOR_ITALIC_SUFFIX;
    public static final String EDITOR_INVALID_COLOR_ITALIC = IPerlColorConstants.INVALID_COLOR
        + EDITOR_ITALIC_SUFFIX;

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

    public static final String SOURCE_CRITIC_JOB_ENABLED = "sourceCriticJobEnabled"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_DEFAULT_LOCATION = "sourceCriticDefaultLocation"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_LOCATION = "sourceCriticLocation"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_SEVERITY_LEVEL = "sourceCriticLevel"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_SEVERITY = "sourceCriticSeverity"; //$NON-NLS-1$

    public static final String SOURCE_CRITIC_OTHEROPTIONS = "sourceCriticOtherOptions"; //$NON-NLS-1$

    public static final String MODULE_STARTER_ENABLED = "moduleStarterEnabled"; //$NON-NLS-1$
    public static final String MODULE_STARTER_DEFAULT_LOCATION = "moduleStarterDefaultLocation"; //$NON-NLS-1$
    public static final String MODULE_STARTER_LOCATION = "moduleStarterLocation"; //$NON-NLS-1$
    public static final String MODULE_STARTER_OVERRIDE_CONFIG = "moduleStarterOverrideConfig"; //$NON-NLS-1$
    public static final String MODULE_STARTER_EMAIL = "moduleStarterEmail"; //$NON-NLS-1$
    public static final String MODULE_STARTER_AUTHOR = "moduleStarterAuthor"; //$NON-NLS-1$
    public static final String MODULE_STARTER_ADDN_OPTS = "moduleStarterAddnOpts"; //$NON-NLS-1$

    private static final Object[] DEFAULT_COLORS = {
        EDITOR_MATCHING_BRACKETS_COLOR, new RGB(192, 192, 192),
        EDITOR_FOLD_COLUMN_BG_COLOR, EDITOR_FOLD_COLUMN_BG_COLOR_DEFAULT,
        EDITOR_LINKED_POSITION_COLOR, new RGB(0, 200, 100),

        EDITOR_STRING_COLOR, new RGB(0, 0, 0), EDITOR_KEYWORD1_COLOR,
        new RGB(160, 32, 240), EDITOR_KEYWORD2_COLOR, new RGB(160, 0, 240),
        EDITOR_VARIABLE_COLOR, new RGB(160, 0, 240), EDITOR_COMMENT1_COLOR,
        new RGB(178, 0, 34), EDITOR_COMMENT2_COLOR, new RGB(178, 34, 0),
        EDITOR_LITERAL1_COLOR, new RGB(0, 0, 255), EDITOR_LITERAL2_COLOR,
        new RGB(160, 32, 240), EDITOR_LABEL_COLOR, new RGB(160, 0, 240),
        EDITOR_FUNCTION_COLOR, new RGB(160, 32, 0), EDITOR_MARKUP_COLOR,
        new RGB(178, 0, 34), EDITOR_OPERATOR_COLOR, new RGB(178, 34, 0),
        EDITOR_NUMBER_COLOR, new RGB(160, 32, 0), EDITOR_INVALID_COLOR,
        new RGB(178, 0, 34), };

    /**
     * Initializes the given preference store with the default values.
     * 
     * @param store
     *            the preference store to be initialized
     */
    public static void initializeDefaultValues(IPreferenceStore store)
    {
        store.setDefault(BROWSER_START_URL, "http://"); //$NON-NLS-1$

        store.setDefault(DEBUG_INTERPRETER_TYPE,
            DEBUG_INTERPRETER_TYPE_STANDARD);
        store.setDefault(DEBUG_PERL_EXECUTABLE, "perl"); //$NON-NLS-1$
        store.setDefault(DEBUG_SHOW_WARNINGS, true);
        store.setDefault(DEBUG_METHOD_SIGNATURES, false);
        store.setDefault(DEBUG_TAINT_MODE, false);
        store.setDefault(DEBUG_DEBUG_CONSOLE, false);
        store.setDefault(DEBUG_SUSPEND_AT_FIRST, true);

        // Variables debug view:
        store.setDefault(DEBUG_HIGHLIGHT_UPDATED_VARS, false);
        store.setDefault(DEBUG_SHOW_GLOBAL_VARS, false);
        store.setDefault(DEBUG_SHOW_INTERNAL_VARS, false);
        store.setDefault(DEBUG_SHOW_LOCAL_VARS, true);
        store.setDefault(DEBUG_SHOW_VARS_ADDRESS, false);

        store.setDefault(EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, true);
        store.setDefault(EDITOR_SYNTAX_VALIDATION, true);
        store.setDefault(EDITOR_SYNTAX_VALIDATION_INTERVAL, 400);
        store.setDefault(EDITOR_OVERVIEW_RULER, true);
        store.setDefault(EDITOR_TAB_WIDTH, 4);
        store.setDefault(EDITOR_SMART_HOME_END, true);
        store.setDefault(EDITOR_SUB_WORD_NAVIGATION, true);
        store.setDefault(EDITOR_LINE_WRAP, false);
        store.setDefault(INSERT_TABS_ON_INDENT, 1);
        store.setDefault(SPACES_INSTEAD_OF_TABS, false);
        store.setDefault(SOURCE_FOLDING, true);
        store.setDefault(PERLDOC_FOLDING, false);
        store.setDefault(SUBROUTINE_FOLDING, false);
        store.setDefault(OUTLINE_MODULE_FOLDING, true);
        store.setDefault(OUTLINE_SUBROUTINE_FOLDING, false);
        store.setDefault(OUTLINE_SORT, false);
        store.setDefault(OUTLINE_COLLAPSE_ALL, false);
        store.setDefault(SOURCE_CRITIC_ENABLED, false);
        store.setDefault(SOURCE_CRITIC_JOB_ENABLED, false);
        store.setDefault(SOURCE_CRITIC_DEFAULT_LOCATION, true);
        store.setDefault(SOURCE_CRITIC_LOCATION, "");
        store.setDefault(AUTO_COMPLETION_QUOTE1, true);
        store.setDefault(AUTO_COMPLETION_QUOTE2, true);
        store.setDefault(AUTO_COMPLETION_BRACKET1, true);
        store.setDefault(AUTO_COMPLETION_BRACKET2, true);
        store.setDefault(AUTO_COMPLETION_BRACKET3, true);
        store.setDefault(AUTO_COMPLETION_BRACKET4, true);
        store.setDefault(EDITOR_MATCHING_BRACKETS, true);

        for (int i = 0; i < DEFAULT_COLORS.length; i += 2)
        {
            PreferenceConverter.setDefault(store, (String) DEFAULT_COLORS[i],
                (RGB) DEFAULT_COLORS[i + 1]);
        }

        store.setDefault(EDITOR_STRING_COLOR_BOLD, false);
        store.setDefault(EDITOR_KEYWORD1_COLOR_BOLD, false);
        store.setDefault(EDITOR_KEYWORD2_COLOR_BOLD, false);
        store.setDefault(EDITOR_VARIABLE_COLOR_BOLD, false);
        store.setDefault(EDITOR_COMMENT1_COLOR_BOLD, false);
        store.setDefault(EDITOR_COMMENT2_COLOR_BOLD, true);
        store.setDefault(EDITOR_LITERAL1_COLOR_BOLD, false);
        store.setDefault(EDITOR_LITERAL2_COLOR_BOLD, false);
        store.setDefault(EDITOR_LABEL_COLOR_BOLD, false);
        store.setDefault(EDITOR_FUNCTION_COLOR_BOLD, false);
        store.setDefault(EDITOR_MARKUP_COLOR_BOLD, false);
        store.setDefault(EDITOR_OPERATOR_COLOR_BOLD, false);
        store.setDefault(EDITOR_NUMBER_COLOR_BOLD, false);
        store.setDefault(EDITOR_INVALID_COLOR_BOLD, false);

        store.setDefault(MODULE_STARTER_ENABLED, false);
        store.setDefault(MODULE_STARTER_DEFAULT_LOCATION, true);
        store.setDefault(MODULE_STARTER_LOCATION, ""); //$NON-NLS-1$
        store.setDefault(MODULE_STARTER_OVERRIDE_CONFIG, true);
        store.setDefault(MODULE_STARTER_EMAIL, ""); //$NON-NLS-1$
        store
            .setDefault(MODULE_STARTER_AUTHOR, System.getProperty("user.name")); //$NON-NLS-1$
        store.setDefault(MODULE_STARTER_ADDN_OPTS, ""); //$NON-NLS-1$
    }
}
