package org.epic.perleditor.editors;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
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
    private static final char NON_BRACKET = '\u0000';
    
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

        char closingChar = getClosingChar(event.character);
        if (closingChar == NON_BRACKET) return;
        
        processBracketKeyStroke(
            viewer.getDocument(),
            viewer.getSelectedRange(),
            event.character,
            closingChar);
    }
    
    /**
     * @return if <code>c</code> is one of the bracket characters for
     *         which bracket insertion is enabled, the correspnding
     *         closing bracket character; otherwise NON_BRACKET
     */
    private char getClosingChar(char c)
    {
        switch (c)
        {
        case ')':
        case '(':
            return closeParens ? ')' : NON_BRACKET;
        case '>':
        case '<':
            return closeAngularBrackets ? '>' : NON_BRACKET;
        case '}':
        case '{':
            return closeBraces ? '}' : NON_BRACKET;
        case ']':
        case '[':
            return closeBrackets ? ']' : NON_BRACKET;
        case '\'':
            return closeSingleQuotes ? '\'' : NON_BRACKET;
        case '\"':
            return closeDoubleQuotes ? '"' : NON_BRACKET;
        default:
            return NON_BRACKET;
        }
    }
    
    /**
     * Tells PerlPartitioner to ignore the next document change event.
     * It is safe to ignore because the real key stroke event will follow
     * and be processed normally. We don't want PerlPartitioner to waste
     * time processing the intermediate state occuring between the two events.
     */
    private void ignoreSmartTypingEvent(IDocument doc)
    {
        IDocumentPartitioner partitioner = doc.getDocumentPartitioner(); 
        if (partitioner instanceof PerlPartitioner)
            ((PerlPartitioner) partitioner).ignoreSmartTypingEvent();
    }
    
    /**
     * @return true if the given character inserted at the given offset
     *         in the document would act as a "closing" character;
     *         false otherwise
     */
    private boolean isClosingChar(IDocument doc, int offset, char c)
    {
        if (c == '}' || c == ']' || c == '>') return true; // easy
        else if (offset == 0) return false; // easy
        else
        {
            try
            {
                // A quote is a closing char when inserted to terminate a string literal,
                // otherwise it is an opening char:
                String partitionType = doc.getPartition(offset-1).getType();
                return PartitionTypes.LITERAL1.equals(partitionType) ||
                       PartitionTypes.LITERAL2.equals(partitionType);
            }
            catch (BadLocationException e)
            {
                logBadLocationException(e);
                return false;
            }
        }
    }
    
    /**
     * BadLocationExceptions should never occur in PerlBracketInserter.
     */
    private void logBadLocationException(BadLocationException e)
    {
        log.log(
            new Status(Status.ERROR,
                PerlEditorPlugin.getPluginId(),
                IStatus.OK,
                "Unexpected exception; report it as a bug " +
                "in plug-in " + PerlEditorPlugin.getPluginId(),
                e));
    }
    
    /**
     * @param doc           document to be modified in result of the key stroke
     * @param selection     selection in the document at the time of the key stroke
     *                      (x = offset, y = length) or caret position if there was
     *                      no selection (x, y = 0)
     * @param keystrokeChar character entered by the user
     * @param closingChar   the corresponding "closing" character
     */
    private void processBracketKeyStroke(
        IDocument doc,
        Point selection,
        char keystrokeChar,
        char closingChar)
    {
        final int offset = selection.x;
        final int length = selection.y;
        
        try
        {
            if (isClosingChar(doc, offset, keystrokeChar))
            {
                // The user has just typed a closing char
                
                if (offset + length < doc.getLength() &&
                    doc.getChar(offset + length) == closingChar)
                {
                    // There's already a closing char in front of us, so erase it
                    ignoreSmartTypingEvent(doc);
                    doc.replace(offset + length, 1, "");
                }
            }
            else
            {
                // The user has just typed an opening char
                
                if (offset + length < doc.getLength() &&
                    doc.getChar(offset + length) == keystrokeChar)
                {
                    // There's already an opening char in front of us, so erase it
                    ignoreSmartTypingEvent(doc);
                    doc.replace(offset + length, 1, "");
                }
                else
                {
                    // Auto-insert the closing char just after it
                    ignoreSmartTypingEvent(doc);
                    doc.replace(offset + length, 0, String.valueOf(closingChar));
                }
            }
        }
        catch (BadLocationException e) { logBadLocationException(e); }
    }
}