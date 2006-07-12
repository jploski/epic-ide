package org.epic.core.parser;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import antlr.CharScanner;
import antlr.CharStreamException;

/**
 * A version of antlr.InputBuffer optimised for performance when reading
 * from {@link org.eclipse.jface.text.IDocument}. This class avoids the
 * circular character queue implementation of {@link antlr.InputBuffer}
 * (which we don't need). Furthermore, it avoids unnecessary copying of
 * document's character data (which would cause excess garbage collection).
 * <p>
 * Note that antlr.InputBuffer is not an official ANTLR interface,
 * so this rewrite may cause problems when ANTLR changes. In this case
 * we may wish to readjust or even fall back on the default (slow)
 * implementation.
 * 
 * @author jploski
 */
class DocumentInputBuffer extends antlr.InputBuffer
{
    private final IDocument doc;
    private final int len;
    
    /**
     * @param doc           document from which characters are to be read
     * @param startOffset   offset of the first character that should be read
     *                      from the document
     */
    public DocumentInputBuffer(IDocument doc, int startOffset)
    {
        this.doc = doc;
        this.len = doc.getLength();
            
        markerOffset = startOffset-1;
    }

    public void commit()
    {
        nMarkers--;
    }

    public void consume()
    {
        markerOffset++;
    }

    public String getLAChars()
    {
        throw new RuntimeException("unexpected method invocation");
    }

    public String getMarkedChars()
    {
        throw new RuntimeException("unexpected method invocation");
    }

    public boolean isMarked()
    {
        return nMarkers != 0;
    }

    public char LA(int i) throws CharStreamException
    {
        try
        {
            char c =
                markerOffset + i < len
                ? doc.getChar(markerOffset + i)
                : CharScanner.EOF_CHAR;
            
            if (c == 65533) throw new CharStreamException(
                "unrecognized character at document offset " + (markerOffset + i));
            else return c;
        }
        catch (BadLocationException e)
        {
            // this can only happen if the document changes while being read
            // ...which should never occur if we have no bugs.            
            throw new RuntimeException(e);
        }
    }

    public void fill(int amount) throws CharStreamException
    {
        // do nothing
    }

    public int mark()
    {
        nMarkers++;
        return markerOffset;
    }

    public void rewind(int mark)
    {
        syncConsume();
        markerOffset = mark;
        nMarkers--;
    }

    public void reset()
    {
        nMarkers = 0;
        markerOffset = -1;
    }
}