package org.epic.perl.editor.test;

import org.eclipse.core.runtime.*;
import org.osgi.framework.Bundle;

public class Log implements ILog
{
    public void addLogListener(ILogListener listener)
    {
        throw new RuntimeException("unimplemented");
    }

    public Bundle getBundle()
    {
        throw new RuntimeException("unimplemented");
    }

    public void log(IStatus status)
    {
        System.err.println(status.getMessage());        
        Throwable t = status.getException();
        if (t != null) t.printStackTrace();
        if (status.getSeverity() == IStatus.ERROR)
        {
            if (t != null) throw new RuntimeException(t);
            else throw new RuntimeException(status.getMessage());
        }
    }

    public void removeLogListener(ILogListener listener)
    {
        throw new RuntimeException("unimplemented");
    }
}
