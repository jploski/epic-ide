package org.epic.perleditor.editors;

import java.util.List;

public class TestBracketMatching extends BasePDETestCase
{
    public void testAll() throws Exception
    {               
        PerlEditor editor = openEditor("EPICTest/Twig.pm");
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        testIface.setExactBracketMatching();
        
        try
        {
            String text = testIface.getText();
    
            List<String> expected = readLines("test.in/TestBracketMatching-expected.txt");
            long t1 = System.currentTimeMillis();
            int j = 0;
            for (int i = 0; i < text.length() && i < 10000; i++)
            {
                char c = text.charAt(i);
                if (c == '(' || c == '{' || c == '[' || c == '<' ||
                    c == ')' || c == '}' || c == ']' || c == '>')
                {
                    testIface.setCaretOffset(i+1);
                    int offset = testIface.getHighlightedBracketOffset();
                    String actual =
                        i + " " +
                        offset + " " +
                        (offset != -1 ? text.charAt(offset) : '-');
                    //System.out.println(actual);
                    assertTrue(j < expected.size());
                    assertEquals(expected.get(j), actual);
                    j++;
                }
            }
            long t2 = System.currentTimeMillis();
            //System.out.println("time: " + (t2-t1)); // 24661 -> 19000
        }
        finally
        {
            closeEditor(editor);
        }
    }
}
