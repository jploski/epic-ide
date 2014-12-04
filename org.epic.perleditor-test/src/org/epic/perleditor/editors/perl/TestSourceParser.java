package org.epic.perleditor.editors.perl;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perleditor.editors.perl.SourceParser;

public class TestSourceParser extends BaseTestCase
{
    public void testFoo()
    {
        String text1 = "my $x = F1oo_bar::Blah::";
        String text2 = "$y + Foo::";
        String text3 = "foo(Foo_bar::Blah->";
        String text4 = "$test?$abc:Foo->";
        
        Pattern pattern = Pattern.compile("([A-Za-z0-9_]+(::|->))+");
        
        assertEquals("F1oo_bar::Blah::", find(pattern, text1));
        assertEquals("Foo::", find(pattern, text2));
        assertEquals("Foo_bar::Blah->", find(pattern, text3));
        assertEquals("Foo->", find(pattern, text4));
    }
    
    public void testTwig() throws Exception
    {
        List<String> lines = readLines("workspace/EPICTest/Twig.pm");
        StringBuffer buf = new StringBuffer();
        for (Iterator<String> i = lines.iterator(); i.hasNext();)
        {
            buf.append(i.next());
            buf.append('\n');
        }
        String text = buf.toString();
        
        SourceParser.getElements(
            text,
            "sub \\S+?",
            "",
            "",
            SourceParser.DELETE_COMMENT | SourceParser.DELETE_POD);
    }
    
    public void testCommentPattern() throws Exception
    {
        String str =
            "some\r\n#nice\r\n=comment\r\nfirst\n  #second\r#  third\r\n=cut\r\ntext\r\nblah\n" +
            "=fiu\nanother =cut miut\n=cut huh?\r=cut\r\nokay";
        
        Matcher m = SourceParser.COMMENT_PATTERN.matcher(str);
        assertTrue(m.find());
        assertEquals(6, m.start());
        assertEquals(11, m.end());
        assertTrue(m.find());
        assertEquals(31, m.start());
        assertEquals(38, m.end());
        assertTrue(m.find());
        assertEquals(39, m.start());
        assertEquals(47, m.end());
        assertFalse(m.find());
    }
    
    public void testPODPattern() throws Exception
    {
        String str =
            "some\r\nnice\r\n=comment\r\nfirst\nsecond\rthird\r\n=cut\r\ntext\r\nblah\n" +
            "=fiu\nanother =cut miut\n=cut huh?\r=cut\r\nokay";
        
        Matcher m = SourceParser.POD_PATTERN.matcher(str);
        assertTrue(m.find());
        assertEquals(12, m.start());
        assertEquals(48, m.end());
        assertTrue(m.find());
        assertEquals(59, m.start());
        assertEquals(98, m.end());
        assertFalse(m.find());
    }
    
    private String find(Pattern pattern, String text)
    {
        Matcher m = pattern.matcher(text);        
        return m.find() ? m.group(0) : null;
    }
}
