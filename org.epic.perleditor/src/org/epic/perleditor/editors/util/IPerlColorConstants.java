/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.epic.perleditor.editors.util;

/**
 * Color keys used for syntax highlighting Perl 
 * code and PerlDoc compliant comments. 
 * A <code>IColorManager</code> is responsible for mapping 
 * concrete colors to these keys.
 * <p>
 * This interface declares static final fields only; it is not intended to be 
 * implemented.
 * </p>
 *
 * @see org.eclipse.jdt.ui.text.IColorManager
 */
public interface IPerlColorConstants {
	
	String STRING_COLOR = "nullColor";
	String KEYWORD1_COLOR = "keyword1Color";
	String KEYWORD2_COLOR = "keyword2Color";
	String VARIABLE_COLOR = "variableColor";
	String COMMENT1_COLOR = "comment1Color";
	String COMMENT2_COLOR = "comment2Color";
	String LITERAL1_COLOR = "literal1Color";
	String LITERAL2_COLOR ="literal2Color";
	String LABEL_COLOR = "labelColor";
	String FUNCTION_COLOR = "functionColor";
	String MARKUP_COLOR = "markupColor";
	String OPERATOR_COLOR ="operatorColor";
	String NUMBER_COLOR = "numberColor";
	String INVALID_COLOR = "invalidColor";
}
