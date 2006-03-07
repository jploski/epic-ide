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
package org.epic.debug.ui.action;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.debug.PerlRegExpBreakpoint;

public class ManageRegExpBreakpointRulerAction
	extends ManageBreakpointRulerAction
{

	public ManageRegExpBreakpointRulerAction(
		IVerticalRulerInfo ruler,
		ITextEditor editor)
	{
		super(ruler, editor);
		fAddLabel = "Add RegExp Breakpoint";
		fRemoveLabel = "Remove RegExp Breakpoint";
	}



 void createBreakPoint(int fLine) throws DebugException, CoreException
 {
 	
	new PerlRegExpBreakpoint(getResource(), fLine);
 	
 }
 
 public void update() {
		 fMarkers= getMarkers();
		 setText(fAddLabel);
		 this.setEnabled(fMarkers.isEmpty());
	 }
}
