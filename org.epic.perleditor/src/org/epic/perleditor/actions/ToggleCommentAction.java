package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.epic.perleditor.editors.*;


public class ToggleCommentAction extends PerlEditorAction
{
    //~ Constructors

    public ToggleCommentAction(PerlEditor editor)
    {
        super(editor);
    }

    //~ Methods

    protected void doRun()
    {
        PerlEditor editor = getEditor();
        Point selRange = editor.getViewer().getSelectedRange();
        IDocument myDoc = editor.getViewer().getDocument();

        // It would be nice to toggle comment in a rewrite session
        // to avoid wasteful and slow reparsing in PerlPartitioner
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=121320
        // for why it seems unpractical.

        // System.out.println("Range: x=" + selRange.x + "; y=" + selRange.y);
        try
        {
            int line2 = 0;
            if (selRange.y > 0)
            {
                line2 = myDoc.getLineOfOffset(selRange.x + selRange.y - 1);
            }
            else
            {
                line2 = myDoc.getLineOfOffset(selRange.x + selRange.y);
            }

            boolean noCommentFound = true;

            for (int i = myDoc.getLineOfOffset(selRange.x);
                 i <= line2 && noCommentFound;
                 i++)
            {
                /*System.out.println("Line: " + myDoc.getLineOffset(i)
                    + "; Länge=" + myDoc.getLineLength(i)
                    + "; 1. zwei Zeichen="
                    + myDoc.getChar(myDoc.getLineOffset(i))
                    + myDoc.getChar(myDoc.getLineOffset(i) + 1));*/
                if (myDoc.getChar(myDoc.getLineOffset(i)) != '#')
                {
                    noCommentFound = false;
                }
            }

            Action action;
            if (noCommentFound)
            {
                action = new TextOperationAction(
                    PerlEditorMessages.getResourceBundle(),
                    "Uncomment.",
                    editor,
                    ITextOperationTarget.STRIP_PREFIX);
            }
            else
            {
                action = new TextOperationAction(
                    PerlEditorMessages.getResourceBundle(),
                    "Comment.",
                    editor,
                    ITextOperationTarget.PREFIX);
            }

            action.run();
        }
        catch (BadLocationException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected String getPerlActionId()
    {
        return PerlEditorActionIds.TOGGLE_COMMENT;
    }
}
