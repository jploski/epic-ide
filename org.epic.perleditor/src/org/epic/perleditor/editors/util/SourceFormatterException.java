package org.epic.perleditor.editors.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

public class SourceFormatterException extends CoreException
{
    private static final long serialVersionUID = 1L;
    public final String output;
    
    SourceFormatterException(IStatus status)
    {
        super(status);
        this.output = null;
    }
    
    SourceFormatterException(IStatus status, String output)
    {
        super(status);
        this.output = output;
    }
}
