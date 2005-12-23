package org.epic.perleditor.editors;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.jface.text.*;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Provides text hovers for known Perl keywords and marker annotations.
 * A text hover is displayed when the mouse pointer stays over a selected
 * piece of text for which hover information is available. 
 */
public class PerlTextHover implements ITextHover
{
    private static int MAX_INFO_LENGTH = 80;

    private TextEditor fTextEditor;

    public PerlTextHover(TextEditor editor)
    {
        super();
        fTextEditor = editor;
    }

    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion)
    {
        String text = getTextForHover(textViewer, hoverRegion);
        if (text == null)
        {
            if (hoverRegion.getLength() == 0 &&
                "true".equals(System.getProperty("org.epic.perleditor.hoverPartitionType")))
            {
                return getPartitionHover(textViewer, hoverRegion);
            }            
            else return null;
        }

        try
        {
            ResourceBundle rb =
                ResourceBundle.getBundle("org.epic.perleditor.editors.quickreference");

            // Check if only a word (without spaces or tabs) has
            // been selected
            if (text.length() > 0 &&
                text.indexOf(" ") < 0 &&
                text.indexOf("\t") < 0)
            {
                try
                {
                    String value = rb.getString(text);
                    return splitMessage(value);
                }
                catch (MissingResourceException e)
                {
                    // Can happen if key does not exist
                    return null;
                }
            }
            else
            {
                try
                {
                    // If no keyword description was found,
                    // try to show marker info
                    IAnnotationHover markerAnnotation =
                        new PerlAnnotationHover(fTextEditor);
                    int line =
                        textViewer.getDocument().getLineOfOffset(hoverRegion.getOffset());
                    return
                        markerAnnotation.getHoverInfo((ISourceViewer) textViewer, line);
                }
                catch (BadLocationException e)
                {
                    // should never occur
                    return null; 
                }
            }
        }
        catch (MissingResourceException e)
        {
            // Properties file not available
            e.printStackTrace();
            return null;
        }
    }

    public IRegion getHoverRegion(ITextViewer textViewer, int offset)
    {
        Point selection = textViewer.getSelectedRange();
        if (selection.x <= offset && offset < selection.x + selection.y)
        {
            return new Region(selection.x, selection.y);
        }
        else
        {
            return new Region(offset, 0);
        }
    }
    
    private String getPartitionHover(ITextViewer textViewer, IRegion hoverRegion)
    {
        try
        {        
            IDocument doc = textViewer.getDocument();
            ITypedRegion partition = doc.getPartition(hoverRegion.getOffset());
            
            return
                "@" + hoverRegion.getOffset() + ": " +partition.getOffset() +
                ":" + partition.getLength() + ":" + partition.getType() + " {" +
                doc.get(partition.getOffset(), partition.getLength()) + "}"; 
        }
        catch (BadLocationException e) { return null; }
    }
    
    private String getTextForHover(ITextViewer textViewer, IRegion hoverRegion)
    {
        if (hoverRegion == null || hoverRegion.getLength() <= 0) return null;
        
        try
        {
            return textViewer.getDocument().get(
                hoverRegion.getOffset(),
                hoverRegion.getLength());
        }
        catch (BadLocationException x)
        {
            // should never occur
            return null;
        }
    }

    private String splitMessage(String message)
    {
        String result = "";

        if (message.length() <= MAX_INFO_LENGTH)
        {
            return message;
        }

        String tmpStr;

        // Index of \n
        int crIndex = message.indexOf("\n");

        if (crIndex != -1)
        {
            tmpStr = message.substring(0, crIndex);
        }
        else
        {
            tmpStr = new String(message);
        }

        while (tmpStr.length() > MAX_INFO_LENGTH)
        {

            int spacepos = tmpStr.indexOf(" ", MAX_INFO_LENGTH);

            if (spacepos != -1)
            {
                result += tmpStr.substring(0, spacepos) + "\n";
                tmpStr = tmpStr.substring(spacepos);
            }
            else
            {
                result += tmpStr.substring(0, MAX_INFO_LENGTH) + "\n";
                tmpStr = tmpStr.substring(MAX_INFO_LENGTH);
            }

        }

        result += tmpStr;

        if (crIndex != -1)
        {
            result += message.substring(crIndex);
        }

        return result;
    }
}
