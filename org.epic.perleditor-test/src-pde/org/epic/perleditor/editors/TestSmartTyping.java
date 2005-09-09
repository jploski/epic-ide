package org.epic.perleditor.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

public class TestSmartTyping extends BasePDETestCase
{
    public void testAll() throws Exception
    {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        assertTrue(prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET1));
        assertTrue(prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET2));
        assertTrue(prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET3));
        assertTrue(prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET4));
        assertTrue(prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE1));
        assertTrue(prefs.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE2));
        
        PerlEditor editor = openEditor("EPICTest/empty.pl");
        
        try
        {   
            Keyboard.typeString("}");
            assertEquals("}", editor._getText());

            editor._clear();
            
            Keyboard.typeString("{");
            assertEquals("{}", editor._getText());
            Keyboard.typeString("{");
            assertEquals("{{}}", editor._getText());
            Keyboard.typeString("}");
            Keyboard.typeString("}");
            assertEquals("{{}}", editor._getText());

            editor._clear();
            
            Keyboard.typeString("my @x = (foo");
            assertEquals("my @x = (foo)", editor._getText());
            Keyboard.typeString(";");
            assertEquals("my @x = (foo;)", editor._getText());
            Keyboard.backspace();
            assertEquals("my @x = (foo)", editor._getText());
            Keyboard.typeString(")");
            assertEquals("my @x = (foo)", editor._getText());
            Keyboard.typeString(";");
            assertEquals("my @x = (foo);", editor._getText());
            
            editor._clear();
            
            Keyboard.typeString("print aaa");
            assertEquals("print aaa", editor._getText());
            Keyboard.left();
            Keyboard.left();
            Keyboard.left();
            Keyboard.typeString("'");
            assertEquals("print ''aaa", editor._getText());
            Keyboard.backspace();
            assertEquals("print 'aaa", editor._getText());
            Keyboard.right();
            Keyboard.right();
            Keyboard.right();
            Keyboard.right();
            Keyboard.typeString("'");
            assertEquals("print 'aaa''", editor._getText());
            Keyboard.right();
            Keyboard.backspace();
            Keyboard.left();
            Keyboard.typeString("'");
            Keyboard.typeString(";");
            assertEquals("print 'aaa';", editor._getText());

            /*
            Display display = Display.getDefault();
            long t1 = System.currentTimeMillis();
            while (t1 + 30000 > System.currentTimeMillis())
            {
                while (display.readAndDispatch());
                Thread.sleep(100);
            }
            */
        }
        finally
        {
            closeEditor(editor);
        }
    }
}
