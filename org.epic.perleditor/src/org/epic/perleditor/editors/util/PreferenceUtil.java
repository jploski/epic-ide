package org.epic.perleditor.editors.util;

import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PreferenceUtil {

	/**
	 * @return
	 */
	public static String getTab() {
		boolean useSpaces =
			PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
				PreferenceConstants.SPACES_INSTEAD_OF_TABS);

		int numSpaces =
			PerlEditorPlugin.getDefault().getPreferenceStore().getInt(
				PreferenceConstants.EDITOR_TAB_WIDTH);

		String tabString = null;

		if (useSpaces) {
			char[] indentChars = new char[numSpaces];

			for (int i = 0; i < indentChars.length; i++) {
				indentChars[i] = ' ';
			}

			tabString = String.valueOf(indentChars);
		} else {
			tabString = "\t";
		}

		return tabString;
	}

	/**
		 * @return
		 */
	public static String getIndent() {
		
		StringBuffer buf = new StringBuffer();
		
		int tabCount = 
		PerlEditorPlugin.getDefault().getPreferenceStore().getInt(
			PreferenceConstants.INSERT_TABS_ON_INDENT);
			
	    String indentString = getTab();
		
		for (int i = 0; i < tabCount; i++) {
			buf.append(indentString);
		}
		
		return buf.toString();
	}
}