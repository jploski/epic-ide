package org.epic.perleditor.editors;

import java.util.*;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

public class TestFolding extends BasePDETestCase
{
    public void testAll() throws Exception
    {
        _testCase("EPICTest/Twig.pm", "test.in/TestFolding-expected1.txt");
        _testCase("EPICTest/test_Folding.pl", "test.in/TestFolding-expected2.txt");
    }

    private void _testCase(String inFile, String outFile) throws Exception
    {
        String expected = readFile(outFile);
        PerlEditor editor = openEditor(inFile);

        try
        {
            IAnnotationModel model = (IAnnotationModel)
                editor.getAdapter(ProjectionAnnotationModel.class);

            String actual = "";
            long timeout = System.currentTimeMillis() + 3000;

            while (System.currentTimeMillis() < timeout)
            {
                // Pump UI event loop so async background reconciler updates can execute
                org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getDefault();
                while (display.readAndDispatch()) { }

                List<String> lines = new ArrayList<String>();
                for (Iterator<Annotation> i = model.getAnnotationIterator(); i.hasNext();)
                {
                    Annotation a = i.next();
                    Position p = model.getPosition(a);
                    if (p != null)
                    {
                        StringBuilder buf = new StringBuilder();
                        buf.append(p.getOffset());
                        buf.append(':');
                        buf.append(p.getLength());
                        buf.append(':');
                        buf.append(a.getType());
                        lines.add(buf.toString());
                    }
                }
                Collections.sort(lines);

                StringBuilder buf = new StringBuilder();
                for (Iterator<String> i = lines.iterator(); i.hasNext();)
                {
                    buf.append(i.next());
                    buf.append('\n');
                }

                actual = buf.toString();
                if (expected.equals(actual))
                {
                    break;
                }

                Thread.sleep(50);
            }

            assertEquals(expected, actual);
        }
        finally
        {
            closeEditor(editor);
        }
    }
}
