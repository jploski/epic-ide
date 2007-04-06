package org.epic.debug.varparser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import org.eclipse.debug.core.model.IVariable;
import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perl.editor.test.Log;

public class TestTokenVarParser extends BaseTestCase
{
    public void testAll() throws Exception
    {
        assertOk("test.in/TestTokenVarParser1.txt");
        assertOk("test.in/TestTokenVarParser2.txt");
        assertOk("test.in/TestTokenVarParser3.txt");
    }
    
    private void dumpVar(PerlDebugVar var, int indent, PrintWriter pw)
        throws Exception
    {
        for (int i = 0; i < indent; i++) pw.print(' ');
        pw.println(var.toString());
        
        if (var.getPdValue().hasVariables())
        {
            IVariable[] vars = var.getPdValue().getVariables();
            for (int i = 0; i < vars.length; i++)
                dumpVar((PerlDebugVar) vars[i], indent+1, pw);
        }
    }
    
    private void assertOk(String inputFile) throws Exception
    {
        DebugTargetStub targetStub = new DebugTargetStub();
        TokenVarParser p = new TokenVarParser(targetStub, new Log());
        
        List vars = p.parseVars(
            readFile(inputFile), PerlDebugVar.LOCAL_SCOPE);
        
        StringWriter actual = new StringWriter();
        PrintWriter pw = new PrintWriter(actual);
        
        for (Iterator i = vars.iterator(); i.hasNext();)
        {
            dumpVar((PerlDebugVar) i.next(), 0, pw);
        }
        pw.close();
        
        //System.err.println(actual);
        
        String expected =
            readFile(inputFile.substring(0, inputFile.length()-4)
                + "-expected.txt");
        
        assertEquals(expected, actual.toString());
    }
}
