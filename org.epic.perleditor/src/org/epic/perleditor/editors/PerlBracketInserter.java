package org.epic.perleditor.editors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Implements the "smart typing" functionality (automatically closing
 * quotes and brackets).
 * 
 * @author jploski
 */
class PerlBracketInserter implements VerifyKeyListener
{
    private final ILog log;
    private boolean closeAngularBrackets;
    private boolean closeBraces;
    private boolean closeBrackets;
    private boolean closeParens;
    private boolean closeDoubleQuotes;
    private boolean closeSingleQuotes;
    private ISourceViewer viewer;
    
    public PerlBracketInserter(ILog log)
    {
        this.log = log;
    }
    
    public boolean isEnabled()
    {
        return
            viewer != null && 
            (closeAngularBrackets ||
            closeBraces ||
            closeBrackets ||
            closeDoubleQuotes ||
            closeSingleQuotes ||
            closeParens);
    }
    
    public void setCloseAngularBracketsEnabled(boolean enabled)
    {
        closeAngularBrackets = enabled;
    }
    
    public void setCloseBracesEnabled(boolean enabled)
    {
        closeBraces = enabled;
    }

    public void setCloseBracketsEnabled(boolean enabled)
    {
        closeBrackets = enabled;
    }

    public void setCloseDoubleQuotesEnabled(boolean enabled)
    {
        closeDoubleQuotes = enabled;
    }

    public void setCloseSingleQuotesEnabled(boolean enabled)
    {
        closeSingleQuotes = enabled;
    }
    
    public void setCloseParensEnabled(boolean enabled)
    {
        closeParens = enabled;
    }
    
    public void setViewer(ISourceViewer viewer)
    {
        this.viewer = viewer;
    }

    public void verifyKey(VerifyEvent event)
    {
        if (!event.doit || !isEnabled()) return;

        char closingChar;
        switch (event.character)
        {
        case ')':
        case '(':
            if (!closeParens) return;
            closingChar = ')';
            break;
        case '>':
        case '<':
            if (!closeAngularBrackets) return;
            closingChar = '>';
            break;
        case '}':
        case '{':
            if (!closeBraces) return;
            closingChar = '}';
            break;
        case ']':
        case '[':
            if (!closeBrackets) return;
            closingChar = ']';
            break;
        case '\'':
            if (!closeSingleQuotes) return;
            closingChar = '\'';
            break;
        case '\"':
            if (!closeDoubleQuotes) return;
            closingChar = '"';
            break;
        default:
            return;
        }

        IDocument document = viewer.getDocument();

        final Point selection = viewer.getSelectedRange();
        final int offset = selection.x;
        final int length = selection.y;
        
        try
        {
            char nextChar =
                offset < document.getLength()
                ? document.getChar(offset)
                : (char) 0;

            if (event.character != closingChar ||
                ((event.character == '\'' || event.character == '"') &&
                nextChar != event.character))
            {
                document.replace(offset, length, String.valueOf(closingChar));
            }
            else if (nextChar == closingChar)
            {
                document.replace(offset, length+1, "");
            }
        }
        catch (BadLocationException e)
        {
            // this one should never occur
            log.log(
                new Status(Status.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    10005, // TODO: use some sort of constant
                    "Unexpected exception; report it as a bug " +
                    "in plug-in " + PerlEditorPlugin.getPluginId(),
                    e));
        }
    }
}