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
	 * Used to determine if occurrences should be highlighted at all.
	 */
	public static final String MARK_OCCURRENCES = "Occurrences.markOccurrences";

	/**
	 * Used to determine if occurrences should be kept when the selection changes.
	 */
	public static final String KEEP_MARKS = "Occurrences.keepMarks";

	public final static String COMMENT = "Occurrences.COMMENT";
	public final static String POD = "Occurrences.POD";
	public final static String KEYWORD = "Occurrences.KEYWORD";
	public final static String VARIABLE = "Occurrences.VARIABLE";
	public final static String LITERAL = "Occurrences.LITERAL";
	public final static String NUMBER = "Occurrences.NUMBER";
	public final static String OPERATOR = "Occurrences.OPERATOR";
    public final static String NAME = "Occurrences.NAME";
    
	/**
	 * Used to set the default values in the given preference store.
	 * @param store
	 */
	public static void initializeDefaultValues(IPreferenceStore store) {
		store.setDefault(MARK_OCCURRENCES, true);
		store.setDefault(COMMENT, true);
		store.setDefault(VARIABLE, true);
		store.setDefault(LITERAL, true);
        store.setDefault(NAME, true);
		store.setDefault(NUMBER, false);
		store.setDefault(OPERATOR, false);
		store.setDefault(POD, false);
		store.setDefault(KEYWORD, false);
		store.setDefault(KEEP_MARKS, false);
	}
}
