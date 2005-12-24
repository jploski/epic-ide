package org.epic.perleditor.editors.perl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.epic.perl.editor.test.BaseTestCase;

public class TestPerlCompletionProcessor extends BaseTestCase
{
    public void testModulePrefixRegexp()
    {
        String text1 = "my $x = F1oo_bar::Blah::";
        String text2 = "$y + Foo::";
        String text3 = "foo(Foo_bar::Blah->";
        String text4 = "$test?$abc:Foo->";
        
        Pattern pattern = PerlCompletionProcessor.MODULE_PREFIX_PATTERN;
        
        assertEquals("F1oo_bar::Blah::", find(pattern, text1));
        assertEquals("Foo::", find(pattern, text2));
        assertEquals("Foo_bar::Blah->", find(pattern, text3));
        assertEquals("Foo->", find(pattern, text4));
    }
    
    public void testVarPrefixRegexp()
    {
        String text1 = "my $abc = $y_1->";
        String text2 = "my $abc = $y_1->boo()";
        String text3 = "my $abc = $y_1::";
        String text4 = "my $abc = $y_1::boo()";
        
        Pattern pattern = PerlCompletionProcessor.VAR_PREFIX_PATTERN;
        
        assertTrue(pattern.matcher(text1).find());
        assertFalse(pattern.matcher(text2).find());
        assertTrue(pattern.matcher(text3).find());
        assertFalse(pattern.matcher(text4).find());
    }
    
    private String find(Pattern pattern, String text)
    {
        Matcher m = pattern.matcher(text);        
        return m.find() ? m.group(0) : null;
    }
}
