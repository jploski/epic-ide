package org.epic.perleditor.editors.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.epic.core.PerlProject;
import org.epic.core.util.PerlExecutor;
import org.epic.core.util.ProcessExecutor;
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
    private static ProcessExecutor processExecutor;
    private static PerlExecutor perlExecutor;
    
    public static boolean gotBrokenPipe;
    
    static
    {
        processExecutor = new ProcessExecutor() {
            protected void brokenPipe(IOException e)
                throws IOException
            {
                if (e.getMessage().indexOf("Broken pipe") == 0 ||
                    e.getMessage().indexOf("Die Pipe ") == 0)
                {
                    PerlValidatorStub.gotBrokenPipe = true;
                }
                else throw new RuntimeException(e);
            } };
            
        perlExecutor = new PerlExecutor(processExecutor) {
            protected List<String> getPerlCommandLine(PerlProject project)
            {
                List<String> ret = new ArrayList<String>();
                ret.add(BaseTestCase.getProperty("perl"));
                ret.add("-Itest.in");
                ret.add("-c");
                return ret;
            }
            
            protected File getPerlWorkingDir(IResource resource)
            {
                return new File(".");
            }
        };
    }

    public PerlValidatorStub()
    {
        super(new Log(), perlExecutor);
    }
    
    public void dispose()
    {
        perlExecutor.dispose();
    }
    
    protected void addMarker(IResource resource, Map<String, Serializable> attributes)
    {                
    }
    
    protected void clearAllUsedMarkers(IResource resource)
    {
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
