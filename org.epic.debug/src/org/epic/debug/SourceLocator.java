package org.epic.debug;

import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

public class SourceLocator implements ISourceLocator
{
    public Object getSourceElement(IStackFrame stackFrame)
    {
        return stackFrame;
    }
}
