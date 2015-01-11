package org.epic.debug.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.*;

/**
 * Implementation of the org.eclipse.ui.console.consolePatternMatchListeners
 * extension point. Provides support for hyperlinked "die" error messages
 * and compilation errors.
 */
public class PerlConsoleTracker implements IPatternMatchListenerDelegate
{
    private final Pattern FILE_LINE_PATTERN =
        Pattern.compile(".*? at ((\\S+) line (\\d+))\\.?");
    
    private TextConsole console;
    
    public void connect(TextConsole console)
    {
        this.console = console;
    }

    public void disconnect()
    {
        this.console = null;
    }

    public void matchFound(PatternMatchEvent event)
    {
        try
        {
            int offset = event.getOffset();
            int length = event.getLength();
            
            String matchText = console.getDocument().get(offset, length);
            Matcher m = FILE_LINE_PATTERN.matcher(matchText);
            if (m.matches())
            {
                String path = m.group(2);
                int line;
                
                try { line = Integer.parseInt(m.group(3)); }
                catch (NumberFormatException e) { line = 1; }
                
                IHyperlink link = new PerlConsoleHyperlink(path, line);
                console.addHyperlink(
                    link,
                    offset + m.start(1),
                    m.end(1) - m.start(1));
            }             
        }
        catch (BadLocationException e)
        {
        }
    }
}
