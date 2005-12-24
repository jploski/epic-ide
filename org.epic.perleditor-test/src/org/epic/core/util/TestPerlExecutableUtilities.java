package org.epic.core.util;

import org.epic.perl.editor.test.BaseTestCase;

public class TestPerlExecutableUtilities extends BaseTestCase
{
    public void testTranslatePathForCygwin() throws Exception
    {
        _test("C:\\Program Files\\foobar", "/cygdrive/c/program files/foobar");
        _test("\\Program Files\\foobar", "/program files/foobar");
        _test("foobar/BAZ", "foobar/baz");
        _test("x:\\foo", "/cygdrive/x/foo");
        _test("", "");
    }
    
    private void _test(String in, String out)
    {
        assertEquals(out, PerlExecutableUtilities.translatePathForCygwin(in));
    }
}
