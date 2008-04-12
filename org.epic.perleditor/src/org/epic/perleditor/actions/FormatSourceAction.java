package org.epic.perleditor.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.PerlEditorActionIds;
import org.epic.perleditor.editors.util.SourceFormatter;
import org.epic.perleditor.editors.util.SourceFormatterException;

/**
 * Reformats the edited document according to the style
 * specified by the Source Formatter preferences. 
 */
public class FormatSourceAction extends PerlEditorAction
{
    //~ Constructors

    public FormatSourceAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        IDocument doc = getEditor().getDocumentProvider().getDocument(
            getEditor().getEditorInput());

        if (doc.getLength() == 0) return;

        StringBuffer text = new StringBuffer(doc.get());
        String anchor = getAnchorString(text);

        ISourceViewer viewer = getEditor().getViewer();
        int anchorOffset = getAnchorOffset(viewer, doc);
        // insert an anchor comment at the end of the line with carret
        // we'll find it back to reposition the caret after reformatting
        text.insert(anchorOffset, anchor);

        String formattedText = runFormatter(text);

        if (formattedText == null ||            formattedText.equals(text.toString()) ||            formattedText.equals(anchor))        {
            return;
        }
        
        StringBuffer newText = new StringBuffer(formattedText);
        anchorOffset = newText.indexOf(anchor);
        if (anchorOffset > 0)
        {
            // remove the anchor comment and the preceeding whitespace
            // which might have been inserted by perltidy
            int len = anchor.length() + 1;
            anchorOffset--;
            while (anchorOffset >= 0 &&
                Character.isWhitespace(newText.charAt(anchorOffset)))
            {
                anchorOffset--;
                len++;
            }
            newText.delete(anchorOffset+1, anchorOffset+len);
        }
        else
            anchorOffset = 0;
        
        doc.set(newText.toString());
        viewer.setSelectedRange(anchorOffset, 0);
        viewer.revealRange(anchorOffset, 0);
    }

    protected String getPerlEditorActionId()
    {
        return PerlEditorActionIds.FORMAT_SOURCE;
    }
    
    private int getAnchorOffset(ISourceViewer viewer, IDocument doc)
    {
        try
        {
            Point sel = viewer.getSelectedRange();
            int docOffset = sel != null ? sel.x : 0;
            int line = doc.getLineOfOffset(docOffset);
            int i = doc.getLineOffset(line) + doc.getLineLength(line);
            i--;
            while (
                (doc.getChar(i) == '\n' || doc.getChar(i) == '\r') &&
                doc.getLineOfOffset(i) == line) i--;
            return i+1;
        }
        catch (BadLocationException e)
        {
            return 0;
        }
    }

    private String getAnchorString(StringBuffer docText)
    {
        String posAnchor = "φί§²";
        StringBuffer buf = new StringBuffer();
        buf.append('#');
        buf.append(posAnchor);        
        while (docText.indexOf(buf.toString()) >= 0)
            buf.append(posAnchor);
        return buf.toString();
    }
    
    private void handleCoreException(CoreException e)
    {
        log(e.getStatus());
        MessageDialog.openError(
            getEditor().getSite().getShell(),
            "Source formatter failed",
            e.getMessage());
    }
    
    private String runFormatter(StringBuffer text)
    {
        try
        {
            return SourceFormatter.format(text.toString(), getLog());
        }
        catch (SourceFormatterException e)
        {
            if (e.output == null)
            {
                handleCoreException(e);
                return null;
            }
            if (MessageDialog.openQuestion(
                getEditor().getSite().getShell(),
                "Source formatter failed",
                e.getMessage() +
                "\nUse formatter's output anyway?"))
            {
                return e.output;
            }
            return null;
        }
        catch (CoreException e)
        {
            handleCoreException(e);
            return null;
        }
    }
}
