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
        StringBuilder buf = new StringBuilder();
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

    /**
     * Verifies standard POD blocks, '=cut' with trailing text, and offset stability.
     */
    public void testPerlCompliantPODBlanking()
    {
        String src =
            "my $a = 1;\r\n" +
            "=head1 NAME\r\n" +
            "Module - Description\r\n" +
            "=cut The documentation ends here\r\n" +
            "my $b = 2;\r\n" +
            "=pod\r\n" +
            "Another block\r\n" +
            "=cut\r\n" +
            "my $c = 3;";

        String masked = SourceParser.blankPODAndComments(src, true, false);

        // 1. Total length must be 100% identical
        assertEquals(src.length(), masked.length());

        // 2. Offsets of code statements are preserved identically
        assertTokenOffsetPreserved(src, masked, "my $a = 1;");
        assertTokenOffsetPreserved(src, masked, "my $b = 2;");
        assertTokenOffsetPreserved(src, masked, "my $c = 3;");

        // 3. POD content is blanked
        assertFalse(masked.contains("Module - Description"));
        assertFalse(masked.contains("Another block"));

        // 4. Verify '=head1' block (including '=cut with trailing text') is blanked
        int pod1Start = src.indexOf("=head1");
        int pod1End = src.indexOf("my $b = 2;");
        assertRangeBlanked(masked, pod1Start, pod1End);
    }

    /**
     * Verifies that Perl operators starting with '=' at column 0 (e.g. '= 5;')
     * are NOT mistaken for POD directives and remain intact.
     */
    public void testCodeStartingWithEqualsIsNotPOD()
    {
        String src =
            "my $total\r\n" +
            "= 500;\r\n" +
            "if ($x\r\n" +
            "== 10) { return; }";

        String masked = SourceParser.blankPODAndComments(src, true, false);

        // Code statements starting with '=' or '==' must not be blanked
        assertTokenOffsetPreserved(src, masked, "= 500;");
        assertTokenOffsetPreserved(src, masked, "== 10)");
    }

    /**
     * Verifies that words starting with '=cut' like '=cute' or '=cutoff'
     * do not falsely terminate POD.
     */
    public void testCutPrefixDoesNotTerminatePOD()
    {
        String src =
            "=pod\r\n" +
            "Inside pod\r\n" +
            "=cutoff is an unknown pod directive\r\n" +
            "Still inside pod\r\n" +
            "=cut\r\n" +
            "my $valid_code = 1;";

        String masked = SourceParser.blankPODAndComments(src, true, false);

        assertTokenOffsetPreserved(src, masked, "my $valid_code = 1;");
        assertFalse(masked.contains("Inside pod"));
        assertFalse(masked.contains("Still inside pod"));
    }

    /**
     * Verifies real-world CPAN modules (like Excel::Writer::XLSX) where POD
     * after __END__ has no closing '=cut', ensuring it does not hang and
     * blanks until EOF.
     */
    public void testPODWithoutCutAtEOF()
    {
        String src =
            "package MyModule;\r\n" +
            "1;\r\n" +
            "__END__\r\n" +
            "=head1 NAME\r\n" +
            "MyModule - Documentation\r\n" +
            "=head1 SYNOPSIS\r\n" +
            "Documentation continuing to end of file without cut\r\n";

        String masked = SourceParser.blankPODAndComments(src, true, false);

        // Pre-POD code intact
        assertTokenOffsetPreserved(src, masked, "package MyModule;");
        assertTokenOffsetPreserved(src, masked, "1;");
        assertTokenOffsetPreserved(src, masked, "__END__");

        // Trailing POD is completely blanked
        int podStart = src.indexOf("=head1");
        assertRangeBlanked(masked, podStart, src.length());
    }

    
    /**
     * Helper to verify that non-whitespace tokens in the original string
     * remain at the EXACT same character index in the masked string.
     */
    private void assertTokenOffsetPreserved(String original, String masked, String token)
    {
        int originalIdx = original.indexOf(token);
        assertTrue("Token must exist in original: " + token, originalIdx != -1);

        int maskedIdx = masked.indexOf(token);
        assertEquals("Token character offset must remain identical", originalIdx, maskedIdx);
    }

    /**
     * Helper to verify that a range in the masked string contains ONLY
     * whitespace (spaces and line breaks).
     */
    private void assertRangeBlanked(String masked, int start, int end)
    {
        for (int i = start; i < end; i++) {
            char c = masked.charAt(i);
            assertTrue("Char at " + i + " must be blanked (space or newline), but was: '" + c + "'",
                c == ' ' || c == '\r' || c == '\n');
        }
    }
    
    private String find(Pattern pattern, String text)
    {
        Matcher m = pattern.matcher(text);        
        return m.find() ? m.group(0) : null;
    }
}
