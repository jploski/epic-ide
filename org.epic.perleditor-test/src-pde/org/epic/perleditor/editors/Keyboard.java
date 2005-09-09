package org.epic.perleditor.editors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * Supports emulation of typing and individual keystrokes.
 */
public class Keyboard
{
    private static final Set uppercaseChars = new HashSet();
    
    static
    {
        // TODO: this probably depends on keyboard layout.. :-(
        // TODO: complete uppercaseChars

        char[] chars = { 
            '"', '{', '}', '!', '@', '#', '$', 
            '%', '^', '&', '*', '(', ')', '_',
            ':', '<', '>', '?', '|', '~', '+' };

        for (int i = 0; i < chars.length; i++)
            uppercaseChars.add(new Character(chars[i]));
    }
    
    /**
     * Simulates a backspace keystroke.
     */
    public static void backspace()
    {
        keystroke(SWT.BS, -1);
    }
    
    /**
     * Simulates a single keystroke.
     * 
     * @param c character that should be typed
     */
    public static void keystroke(char c)
    {
        keystroke(c, -1);
    }
    
    /**
     * Simulates a single keystroke.
     * 
     * @param keyCode code of key that should be typed (as defined in class SWT)
     */
    public static void keystroke(int keyCode)
    {
        keystroke(' ', keyCode);
    }
    
    /**
     * Simulates a left arrow keystroke.
     */
    public static void left()
    {
        keystroke(SWT.ARROW_LEFT);
    }
    
    /**
     * Simulates a right arrow keystroke.
     */
    public static void right()
    {
        keystroke(SWT.ARROW_RIGHT);
    }
    
    /**
     * Simulates typing of the specified string.
     */
    public static void typeString(String string)
    {
        Display display = Display.getDefault();
        
        for (int i = 0; i < string.length(); i++)
        {
            char c = string.charAt(i);
    
            boolean uppercase;        
            if (Character.isUpperCase(c))
            {
                c = Character.toLowerCase(c);
                uppercase = true;
            }
            else if (uppercaseChars.contains(new Character(c)))
            {
                uppercase = true;
            }
            else uppercase = false;
        
            Event e;
            
            if (uppercase)
            {
                e = new Event();
                e.type = SWT.KeyDown;
                e.keyCode = SWT.SHIFT;
                display.post(e);
            }
            
            e = new Event();
            e.type = SWT.KeyDown;
            e.character = c;
            if (uppercase) e.stateMask = SWT.SHIFT;
            display.post(e);
    
            e = new Event();
            e.type = SWT.KeyUp;
            e.character = c;
            if (uppercase) e.stateMask = SWT.SHIFT;
            display.post(e);
            
            if (uppercase)
            {
                e = new Event();
                e.type = SWT.KeyUp;
                e.keyCode = SWT.SHIFT;
                display.post(e);
            }
            
            while (display.readAndDispatch());
            try { Thread.sleep(2); } catch (InterruptedException _) { }
        }
    }

    private static void keystroke(char c, int keyCode)
    {
        Display display = Display.getDefault();
        Event e;

        e = new Event();
        e.type = SWT.KeyDown;
        if (keyCode == -1 ) e.character = c;
        else e.keyCode = keyCode;
        display.post(e);

        e = new Event();
        e.type = SWT.KeyUp;
        if (keyCode == -1 ) e.character = c;
        else e.keyCode = keyCode;
        display.post(e);
        
        while (display.readAndDispatch());
        try { Thread.sleep(20); } catch (InterruptedException _) { }
    }
}
