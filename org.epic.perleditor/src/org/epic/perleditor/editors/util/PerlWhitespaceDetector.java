package org.epic.perleditor.editors.util;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * A perl aware white space detector.
 */
public class PerlWhitespaceDetector implements IWhitespaceDetector {

	/* 
	 * Method declared on IWhitespaceDetector
	 */
	public boolean isWhitespace(char character) {
		return Character.isWhitespace(character);
	}
}
