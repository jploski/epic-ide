package org.epic.perleditor.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Preference constants used in the preference store and for the preference page
 * of mark occurences. Setting of default values.
 * 
 * @author Katrin
 * 
 */
public class MarkOccurrencesPreferences {

	/**
	 * used for Button, to determine, if occurrences should be highlighted
	 */
	public static final String MARK_OCCURRENCES = "Occurrences.markOccurrences";

	/**
	 * used for Button, to determine, if occurrences should be kept when
	 * selection changed
	 */
	public static final String KEEP_MARKS = "Occurrences.keepMarks";

	/**
	 * used for Button, to determine, if occurrences in comments will be marked
	 */
	public final static String COMMENT = "Occurrences.COMMENT";

	/**
	 * used for Button, to determine, if occurrences in pod will be marked
	 */
	public final static String POD = "Occurrences.POD";

	/**
	 * used for Button, to determine, if occurrences in keywords will be marked
	 */
	public final static String KEYWORD = "Occurrences.KEYWORD";

	/**
	 * used for Button, to determine, if occurrences in variables will be marked
	 */
	public final static String VARIABLE = "Occurrences.VARIABLE";

	/**
	 * used for Button, to determine, if occurrences in literals will be marked
	 */
	public final static String LITERAL = "Occurrences.LITERAL";

	/**
	 * used for Button, to determine, if occurrences in number will be marked
	 */
	public final static String NUMBER = "Occurrences.NUMBER";

	/**
	 * used for Button, to determine, if occurrences in operators will be marked
	 */
	public final static String OPERATOR = "Occurrences.OPERATOR";

	/**
	 * Used to set the default values in the given preference store.
	 * @param store
	 */
	public static void initializeDefaultValues(IPreferenceStore store) {
		store.setDefault(MARK_OCCURRENCES, true);
		store.setDefault(COMMENT, true);
		store.setDefault(VARIABLE, true);
		store.setDefault(LITERAL, true);
		store.setDefault(NUMBER, false);
		store.setDefault(OPERATOR, false);
		store.setDefault(POD, false);
		store.setDefault(KEYWORD, false);
		store.setDefault(KEEP_MARKS, false);
	}
}
