package org.epic.core.model;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perl.editor.test.Log;
import org.epic.perleditor.editors.PartitionTypes;
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
        Document doc = new Document(readFile(inFile));
        new PerlPartitioner(new Log(), doc);

        String expected = readFile(outFile);

        SourceFile src = new SourceFile(new Log(), doc);
        src.parse();
        
        StringBuffer buf = new StringBuffer();
        for (Iterator<Package> i = src.getPackages().iterator(); i.hasNext();)
        {
            Package pkg = i.next();
            buf.append(pkg.getName());
            buf.append(':');
            buf.append(pkg.getStartLine());
            buf.append(':');
            buf.append(pkg.getEndLine());
            buf.append(':');
            buf.append(pkg.getSubs().size());
            buf.append(':');
            buf.append('{');
            for (Iterator<Subroutine> j = pkg.getSubs().iterator(); j.hasNext();)
            {
                Subroutine sub = j.next();
                buf.append(sub.getName());
                buf.append(':');
                buf.append(sub.getBlockLevel());
                buf.append(String.format("%n"));
            }
            buf.append('}');
            buf.append(String.format("%n"));
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