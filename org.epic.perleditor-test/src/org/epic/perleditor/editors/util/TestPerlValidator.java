package org.epic.perleditor.editors.util;

import org.easymock.MockControl;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perl.editor.test.Log;

public class TestPerlValidator extends BaseTestCase
{
    public void testBrokenPipe() throws Exception
    {
        MockControl cMockProject = MockControl.createControl(IProject.class);
        IProject mockProject = (IProject) cMockProject.getMock();
        mockProject.getLocation();
        cMockProject.setReturnValue(new Path("."));
        cMockProject.replay();
        
        MockControl cMockResource = MockControl.createControl(IResource.class);
        IResource mockResource = (IResource) cMockResource.getMock();
        mockResource.getProject();
        cMockResource.setReturnValue(mockProject);
        mockResource.getProject();
        cMockResource.setReturnValue(mockProject);
        cMockResource.replay();

        // We expect a "broken pipe" IOException when feeding a large
        // piece of source code containing an error in the header to
        // the Perl interpreter:
        PerlValidatorStub validator = new PerlValidatorStub();
        
        try
        {         
            validator.validate(
                mockResource,
                validator.readSourceFile(getFile("test.in/Tool.pm").getAbsolutePath(), null));
        
            assertTrue(PerlValidatorStub.gotBrokenPipe);
        }
        finally
        {
            validator.dispose();
        }
    }
    
    public void testParsedErrorLine()
    {
        String line =
            "Subroutine foo redefined at /blah/X.pm line 65." +
            " at /foo/Bar.pm line 22.";
        PerlValidatorBase.ParsedErrorLine pline =
            new PerlValidatorBase.ParsedErrorLine(line, new Log());
        
        assertEquals(
            "Subroutine foo redefined at /blah/X.pm line 65.",
            pline.getMessage());
        assertEquals(22, pline.getLineNumber());
        
        // test case for bug #1307071
        line = "syntax error at bug1307071.pl line 9, near \"if {\"";
        pline = new PerlValidatorBase.ParsedErrorLine(line, new Log());
        assertEquals("syntax error", pline.getMessage()); // could be better...
        assertEquals(9, pline.getLineNumber());
    }
}
