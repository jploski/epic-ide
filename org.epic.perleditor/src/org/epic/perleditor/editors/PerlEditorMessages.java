/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.editors;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class PerlEditorMessages {

	private static final String RESOURCE_BUNDLE= "org.epic.perleditor.PerlEditorMessages";//$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private PerlEditorMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		}
	}
	
	public static ResourceBundle getResourceBundle() {
		return fgResourceBundle;
	}
}
