package org.epic.perleditor.editors.util;

import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PreferenceUtil {

	/**
	 * @return
	 */
	public static String getTab(int column) {
		boolean useSpaces =
			PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
				PreferenceConstants.SPACES_INSTEAD_OF_TABS);


		String tabString = null;

		if (useSpaces) {
			int numSpaces =
			PerlEditorPlugin.getDefault().getPreferenceStore().getInt(
				PreferenceConstants.INSERT_TABS_ON_INDENT);
			char[] indentChars = new char[numSpaces - (column % numSpaces)];
			
			
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
		boolean useSpaces =
			PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
				PreferenceConstants.SPACES_INSTEAD_OF_TABS);
			
	    String indentString = getTab(0);
		
        if (!useSpaces) {
			for (int i = 0; i < tabCount; i++) {
				buf.append(indentString);
			}
		}
		
		return buf.toString();
	}
}