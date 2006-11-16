package org.epic.perleditor.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

public class TestSmartTyping extends BasePDETestCase
{
    public void testAll() throws Exception
    {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        
        boolean p1 = prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET1);
        boolean p2 = prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET2);
        boolean p3 = prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET3);
        boolean p4 = prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET4);
        boolean p5 = prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE1);
        boolean p6 = prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE2);
    
        try { _testAll(); }
        finally
        {
            prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET1, p1);
            prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET2, p2);
            prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET3, p3);
            prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET4, p4);
            prefs.setValue(PreferenceConstants.AUTO_COMPLETION_QUOTE1, p5);
            prefs.setValue(PreferenceConstants.AUTO_COMPLETION_QUOTE2, p6);
        }
    }
    
    private void _testAll() throws Exception
    {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        
        prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET1, true);
        prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET2, true);
        prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET3, true);
        prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET4, true);
        prefs.setValue(PreferenceConstants.AUTO_COMPLETION_QUOTE1, true);
        prefs.setValue(PreferenceConstants.AUTO_COMPLETION_QUOTE2, true);
        
        PerlEditor editor = openEditor("EPICTest/empty.pl");
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        
        try
        {   
            Keyboard.typeString("}");            
            assertEquals("}", testIface.getText());

            testIface.clear();
            
            Keyboard.typeString("{");
            assertEquals("{}", testIface.getText());
            Keyboard.typeString("{");
            assertEquals("{{}}", testIface.getText());
            Keyboard.typeString("}");
            Keyboard.typeString("}");
            assertEquals("{{}}", testIface.getText());

            testIface.clear();
            
            Keyboard.typeString("my @x = (foo");
            assertEquals("my @x = (foo)", testIface.getText());
            Keyboard.typeString(";");
            assertEquals("my @x = (foo;)", testIface.getText());
            Keyboard.backspace();
            assertEquals("my @x = (foo)", testIface.getText());
            Keyboard.typeString(")");
            assertEquals("my @x = (foo)", testIface.getText());
            Keyboard.typeString(";");
            assertEquals("my @x = (foo);", testIface.getText());
            
            testIface.clear();
            
            Keyboard.typeString("print aaa");
            assertEquals("print aaa", testIface.getText());
            Keyboard.left();
            Keyboard.left();
            Keyboard.left();
            Keyboard.typeString("'");
            assertEquals("print ''aaa", testIface.getText());
            Keyboard.backspace();
            assertEquals("print 'aaa", testIface.getText());
            Keyboard.right();
            Keyboard.right();
            Keyboard.right();
            Keyboard.right();
            Keyboard.typeString("'");
            assertEquals("print 'aaa'", testIface.getText());
            Keyboard.left();
            Keyboard.typeString("'");
            Keyboard.typeString(";");
            assertEquals("print 'aaa';", testIface.getText());
            
            testIface.clear();
            
            Keyboard.typeString("# ' ok {");
            assertEquals("# ' ok {}", testIface.getText());
            
            testIface.clear();
            
            Keyboard.typeString("=comment\nLet's go");
            assertEquals("=comment\nLet's go", testIface.getText());
            
            testIface.clear();
            prefs.setValue(PreferenceConstants.AUTO_COMPLETION_BRACKET2, false);

            Keyboard.typeString(" ");
            Keyboard.left();
            Keyboard.typeString("${()");

            assertEquals("${() ", testIface.getText());
        }
        finally
        {
            closeEditor(editor);
        }
    }
}
