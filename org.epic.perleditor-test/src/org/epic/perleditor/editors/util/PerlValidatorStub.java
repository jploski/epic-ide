package org.epic.perleditor.editors.util;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perl.editor.test.Log;

/**
 * Simplest possible subclass of PerlValidatorBase, independent of Eclipse
 * runtime environment and suitable for testing PerlValidatorBase's
 * functionality.
 * 
 * @author jploski
 */
public class PerlValidatorStub extends PerlValidatorBase
{
    public boolean gotBrokenPipe;

    public PerlValidatorStub()
    {
        super(new Log());
    }
    
    protected void addMarker(IResource resource, Map attributes)
    {                
    }
    
    protected void brokenPipe(IOException e)
    {
        if (e.getMessage().indexOf("Broken pipe") == 0 ||
            e.getMessage().indexOf("Die Pipe wurde beendet") == 0)
        {
            gotBrokenPipe = true;
        }
        else throw new RuntimeException(e);
    }

    protected void clearAllUsedMarkers(IResource resource)
    {
    }

    protected List getPerlCommandLine(IResource resource)
    {
        List ret = new ArrayList();
        ret.add(BaseTestCase.getProperty("perl"));
        ret.add("-Itest.in");
        ret.add("-c");
        return ret;
    }

    protected File getPerlWorkingDir(IResource resource)
    {
        return new File(".");
    }

    protected boolean isProblemMarkerPresent(ParsedErrorLine line, IResource resource)
    {
        return false;
    }

    protected void removeUnusedMarkers(IResource resource)
    {
    }
    
    protected boolean shouldUnderlineError(IResource resource, int lineNr)
    {
        return true;
    }
}
