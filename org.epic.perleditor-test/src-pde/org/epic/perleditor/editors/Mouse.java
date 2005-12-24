package org.epic.perleditor.editors;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * Supports emulation of moving the mouse pointer and clicking.
 */
public class Mouse
{
    public static void click(Point p, boolean rightClick)
    {
        // display.post doesn't work (under Linux?)
        
        Display display = Display.getDefault();
        
        try
        {
            int button =
                rightClick ? InputEvent.BUTTON3_MASK : InputEvent.BUTTON1_MASK;
            
            Robot robot = new Robot();
            robot.mouseMove(p.x, p.y);
            while (display.readAndDispatch());
            try { Thread.sleep(20); } catch (InterruptedException _) { }
            robot.mousePress(button);
            while (display.readAndDispatch());
            try { Thread.sleep(20); } catch (InterruptedException _) { }
            robot.mouseRelease(button);
            while (display.readAndDispatch());
            try { Thread.sleep(20); } catch (InterruptedException _) { }
        }
        catch (AWTException e) { throw new RuntimeException(e); }
    }
}
