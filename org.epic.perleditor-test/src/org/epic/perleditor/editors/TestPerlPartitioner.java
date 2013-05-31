package org.epic.perleditor.editors;

import org.eclipse.jface.text.*;
import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perl.editor.test.Log;

public class TestPerlPartitioner extends BaseTestCase
{
    // this test expects unix newlines
    public void testSyntax() throws Exception
    {
        Document doc = new Document(readFile("workspace/EPICTest/syntax.pl").replace("\r",""));
        PerlPartitioner partitioner = new PerlPartitioner(new Log(), doc);

        ITypedRegion[] partitioning =
            partitioner.computePartitioning(0, doc.getLength());
        
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < partitioning.length; i++)
        {
            ITypedRegion r = partitioning[i];
            buf.append(r.getOffset());
            buf.append(':');
            buf.append(r.getLength());
            buf.append(':');
            buf.append(r.getType());
            buf.append(':');
            buf.append('{');
            buf.append(doc.get(r.getOffset(), r.getLength()));
            buf.append('}');
            buf.append('\n');
        }
        
        String expected = readFile("test.in/TestPerlPartitioner-expected.txt").replace("\r","");
        assertEquals(expected, buf.toString());
        //System.err.println(buf);
    }
}
