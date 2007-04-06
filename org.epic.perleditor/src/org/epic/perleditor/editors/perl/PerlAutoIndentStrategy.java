package org.epic.perleditor.editors.perl;

import org.eclipse.jface.text.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PreferenceUtil;
import org.epic.perleditor.editors.*;

/**
 * Auto indent strategy sensitive to brackets.
 */
public final class PerlAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy
{
    private final PerlPairMatcher bracketMatcher =
        new PerlPairMatcher(PerlEditorPlugin.getDefault().getLog());
    
    public PerlAutoIndentStrategy()
    {
    }

    public void customizeDocumentCommand(IDocument d, DocumentCommand c)
    {
        if (c.length == 0 &&
            c.text != null &&
            TextUtilities.endsWith(d.getLegalLineDelimiters(), c.text) != -1)
        {
            smartIndentAfterNewLine(d, c);
        }
        else if ("}".equals(c.text)) //$NON-NLS-1$
        {
            smartInsertAfterBracket(d, c);
        }
    }

    /**
     * Returns the String at line with the leading whitespace removed.
     * 
     * @returns the String at line with the leading whitespace removed.
     * @param document -
     *            the document being parsed
     * @param line -
     *            the line being searched
     */
    private String getIndentOfLine(IDocument document, int line)
        throws BadLocationException
    {
        if (line > -1)
        {
            int start = document.getLineOffset(line);
            int end = start + document.getLineLength(line) - 1;
            int whiteend = findEndOfWhiteSpace(document, start, end);
            return document.get(start, whiteend - start);
        }
        else
        {
            return ""; //$NON-NLS-1$
        }
    }
    
    private boolean handleNewLineAfterHashClosingBracket(
        IDocument document,
        int whiteend,
        StringBuffer textToInsert) throws BadLocationException
    {
        int docLength = document.getLength();
        if (whiteend < docLength && "}".equals(document.get(whiteend, 1)))
        {
            // If we got here, then a new line is inserted on a line
            // which starts with a } (after white space)
            // Check if this } closes a hash; if so, make the new line
            // indented just as the line which started the hash (with {)
            
            IRegion block = bracketMatcher.match(document, whiteend+1);
            if (block != null && isHash(document, block))
            {
                int hashStartLine = document.getLineOfOffset(block.getOffset());
                textToInsert.append(getIndentOfLine(document, hashStartLine));
                return true;
            }
        }
        return false;
    }
    
    private boolean handleNewLineAfterOpeningBracket(
        IDocument document,
        DocumentCommand command,
        int start,
        int line,
        StringBuffer textToInsert) throws BadLocationException
    {
        // Quick check first:
        String lineText = document.get(start, document.getLineLength(line));
        int openBracketIndex = lineText.indexOf('{'); 
        if (openBracketIndex == -1) return false;
        
        // If the { we're seeing is in a comment, don't bother
        if (PartitionTypes.COMMENT.equals(
            document.getPartition(start + openBracketIndex).getType())) 
            return false;
        
        // Now the accurate (slower) check:
        IRegion block = bracketMatcher.match(document, command.offset+1, '}');
        if (block == null) return false;
        
        if (document.getLineOfOffset(block.getOffset()) == line)
        {
            textToInsert.append(PreferenceUtil.getTab(0));
            return true;
        }
        return false;
    }
    
    private void handleNewLineWithinBlock(
        IDocument document,
        DocumentCommand command,
        StringBuffer textToInsert) throws BadLocationException
    {
        int docLength = document.getLength();
        int p = command.offset == docLength
            ? command.offset - 1
            : command.offset;
        int line = document.getLineOfOffset(p);
        int start = document.getLineOffset(line);
        int whiteend = findEndOfWhiteSpace(document, start, command.offset);
        
        if (handleNewLineAfterHashClosingBracket(
            document, whiteend, textToInsert)) return;
        
        textToInsert.append(document.get(start, whiteend - start));
        
        // Check if the line on which the new line is inserted contains
        // more open brackets than close brackets; if so, we will add
        // an extra indent after the newline.
        
        handleNewLineAfterOpeningBracket(
            document, command, start, line, textToInsert);
    }
    
    private void handleNewLineBeforeClosingBracket(
        IDocument document,
        DocumentCommand command,
        StringBuffer textToInsert) throws BadLocationException
    {
        IRegion block = bracketMatcher.match(document, command.offset+1);
        if (block == null) return;

        boolean bothBracketsWereOnSameLine = 
            document.getLineOfOffset(block.getOffset()) ==
                document.getLineOfOffset(command.offset);
        
        textToInsert.append(getIndentOfLine(
            document, document.getLineOfOffset(block.getOffset())));

        if (bothBracketsWereOnSameLine)
        {
            textToInsert.append(PreferenceUtil.getTab(0));
            command.shiftsCaret = false;
            command.caretOffset = command.offset + textToInsert.length();
            textToInsert.append(TextUtilities.getDefaultLineDelimiter(document));
            textToInsert.append(getIndentOfLine(
                document, document.getLineOfOffset(block.getOffset())));            
        }
        if (isHash(document, block))
        {
            textToInsert.append(PreferenceUtil.getTab(0));
        }
    }
    
    private boolean isHash(IDocument document, IRegion block)
        throws BadLocationException
    {
        int offset = block.getOffset() - 1;        
        while (offset >= 0)
        {
            ITypedRegion p = document.getPartition(offset);
            if (PartitionTypes.OPERATOR.equals(p.getType()))
            {
                String op = document.get(p.getOffset(), p.getLength());
                if ("=".equals(op) ||
                    "(".equals(op) ||
                    "=>".equals(op)) return true;
                else return false;
            }
            else
            {
                offset = p.getOffset()-1;
            }
        }
        return false;
    }
    
    private boolean newLineInsertedBeforeClosingBracket(
        IDocument document,
        DocumentCommand command) throws BadLocationException
    {
        return
            command.offset < document.getLength() &&
            document.getChar(command.offset) == '}';
    }

    /**
     * Set the indent of a new line based on the command provided
     * in the supplied document.
     * 
     * @param document -
     *            the document being parsed
     * @param command -
     *            the command being performed
     */
    private void smartIndentAfterNewLine(
        IDocument document, DocumentCommand command)
    {
        int docLength = document.getLength();
        if (command.offset == -1 || docLength == 0) return;

        try
        {
            StringBuffer buf = new StringBuffer(command.text);
            if (newLineInsertedBeforeClosingBracket(document, command))                
            {
                handleNewLineBeforeClosingBracket(document, command, buf);                
            }
            else
            {
                handleNewLineWithinBlock(document, command, buf);
            }
            command.text = buf.toString();

        }
        catch (BadLocationException excp)
        {
            System.out.println(PerlEditorMessages
                .getString("AutoIndent.error.bad_location_1")); //$NON-NLS-1$
        }
    }

    /**
     * Set the indent of a bracket based on the command provided in the supplied
     * document.
     * 
     * @param document -
     *            the document being parsed
     * @param command -
     *            the command being performed
     */
    private void smartInsertAfterBracket(
        IDocument document, DocumentCommand command)
    {
        if (command.offset == -1 || document.getLength() == 0) return;

        try
        {
            int p = (command.offset == document.getLength() ? command.offset - 1
                : command.offset);
            int line = document.getLineOfOffset(p);
            int start = document.getLineOffset(line);
            int whiteend = findEndOfWhiteSpace(document, start, command.offset);

            // shift only when line does not contain any text up to
            // the closing bracket
            if (whiteend != command.offset) return;
            
            IRegion block = bracketMatcher.match(document, command.offset, '}');
            if (block == null) return;

            int indLine = document.getLineOfOffset(block.getOffset());
            if (indLine == line) return;            

            // take the indent of the found line with an open bracket
            StringBuffer replaceText = new StringBuffer(
                getIndentOfLine(document, indLine));
            // add the rest of the current line including the just added
            // close bracket
            replaceText.append(
                document.get(whiteend, command.offset - whiteend));
            if (isHash(document, block))
            {
                // if the closing bracket belongs to a Perl hash rather
                // than a block, indent it to hang at the same column
                // as the hash's content
                replaceText.append(PreferenceUtil.getTab(0));
            }
            replaceText.append(command.text);
            // modify document command
            command.length = command.offset - start;
            command.offset = start;
            command.text = replaceText.toString();
        }
        catch (BadLocationException excp)
        {
            System.out.println(PerlEditorMessages
                .getString("AutoIndent.error.bad_location_2")); //$NON-NLS-1$
        }
    }
}
