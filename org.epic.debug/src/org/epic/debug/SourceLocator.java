package org.epic.debug;

import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SourceLocator implements ISourceLocator {

	/**
	 * Constructor for SourceLocator.
	 */
	public SourceLocator() {
		super();
	}

	/**
	 * @see org.eclipse.debug.core.model.ISourceLocator#getSourceElement(IStackFrame)
	 */
	public Object getSourceElement(IStackFrame stackFrame) {
		return stackFrame;
	}

}
