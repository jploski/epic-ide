package org.epic.core.model;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perl.editor.test.Log;
import org.epic.perleditor.editors.PerlPartitioner;

public class TestSourceFile extends BaseTestCase
{
    public void testOutline() throws Exception
    {
        // Regression test for subroutine-to-package mappings
        
        _testFile(
            "workspace/EPICTest/lib/TestPackage.pm",
            "test.in/TestSourceFile-expected1.txt");
        
        _testFile(
            "workspace/EPICTest/Twig.pm",
            "test.in/TestSourceFile-expected2.txt");
    }
    
    private void _testFile(String inFile, String outFile) throws IOException
    {
        IDocument doc = new Document(readFile(inFile));
        String expected = readFile(outFile);
        
        PerlPartitioner partitioner = new PerlPartitioner(new Log());
        doc.setDocumentPartitioner(partitioner);
        partitioner.connect(doc);
        
        SourceFile src = new SourceFile(new Log(), doc);
        src.parse();
        
        StringBuffer buf = new StringBuffer();
        for (Iterator i = src.getPackages().iterator(); i.hasNext();)
        {
            Package pkg = (Package) i.next();
            buf.append(pkg.getName());
            buf.append(':');
            buf.append(pkg.getStartLine());
            buf.append(':');
            buf.append(pkg.getEndLine());
            buf.append(':');
            buf.append(pkg.getSubs().size());
            buf.append(':');
            buf.append('{');
            for (Iterator j = pkg.getSubs().iterator(); j.hasNext();)
            {
                Subroutine sub = (Subroutine) j.next();
                buf.append(sub.getName());
                buf.append(':');
                buf.append(sub.getBlockLevel());
                buf.append('\n');
            }
            buf.append('}');
            buf.append('\n');
        }
        
        /*
        PrintWriter pw = new PrintWriter(
            new FileWriter("/tmp/outf"));
        
        pw.println(buf);
        pw.close();
        */
        
        assertEquals(expected, buf.toString());
    }
}