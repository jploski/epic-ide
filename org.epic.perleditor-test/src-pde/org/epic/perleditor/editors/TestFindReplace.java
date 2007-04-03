package org.epic.perleditor.editors;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IFileEditorInput;

public class TestFindReplace extends BasePDETestCase
{
    public void testTextBuffer() throws Exception
    {
        // This test checks whether the document obtained from the
        // editor is managed by TextFileBufferManager. Were it not
        // the case, then operations such as Search/Replace would
        // execute on the disk version of the file instead of the
        // working copy (see bug #1589280).
        
        PerlEditor editor = openEditor("EPICTest/test_FindReplace.pl");

        IPath fileLocation =
            ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation();
        
        ITextFileBufferManager bm = FileBuffers.getTextFileBufferManager();
        bm.connect(fileLocation, new NullProgressMonitor());
        
        try
        {
            IDocument doc = editor.getViewer().getDocument();
            doc.replace(0, 0, "abc");
            
            String bufText = bm.getTextFileBuffer(fileLocation).getDocument().get();           
            assertEquals(doc.get(), bufText);
        }
        finally
        {
            bm.disconnect(fileLocation, new NullProgressMonitor());
            closeEditor(editor);
        }
    }
    
    public void testFindReplace() throws Exception
    {
        PerlEditor editor = openEditor("EPICTest/test_FindReplace.pl");

        try
        {
            // In this test, we perform a global find-replace action
            // and check to see that the document partitioning is
            // correct after the change
            
            IFindReplaceTarget target = (IFindReplaceTarget) editor
                .getAdapter(IFindReplaceTarget.class);

            IDocument doc = editor.getViewer().getDocument();
            doFindReplace(target, "\\s*$", "", true);
            
            //IDocument doc = editor.getViewer().getDocument();            
            IDocumentPartitioner partitioner = (IDocumentPartitioner)
                PartitionTypes.getPerlPartitioner(doc);
            
            ITypedRegion[] regions =
                partitioner.computePartitioning(0, doc.getLength());
            
            assertEquals(3, regions.length);
            assertEquals(0, regions[0].getOffset());
            assertEquals(1, regions[0].getLength());
            assertEquals(1, regions[1].getOffset());
            assertEquals(5, regions[1].getLength());
            assertEquals(6, regions[2].getOffset());
            assertEquals(1, regions[2].getLength());
        }
        finally
        {
            closeEditor(editor);
        }
    }

    /**
     * This find-and-replace-all algorithm is adopted from the
     * {@link org.eclipse.ui.texteditor.FindReplaceDialog}.
     */
    private void doFindReplace(
        IFindReplaceTarget target,
        String findString,
        String replaceString,
        boolean regExSearch)
    {
        int replaceCount= 0;
        int findReplacePosition= 0;
        boolean forwardSearch = true;
        boolean caseSensitive = false;
        boolean wholeWord = false;
        
        if (target instanceof IFindReplaceTargetExtension)
            ((IFindReplaceTargetExtension) target).setReplaceAllMode(true);

        try
        {
            int index = 0;
            while (index != -1)
            {
                index = ((IFindReplaceTargetExtension3) target).findAndSelect(
                    findReplacePosition,
                    findString,
                    forwardSearch,
                    caseSensitive,
                    wholeWord,
                    regExSearch);

                if (index != -1)
                { // substring not contained from current position                    
                    ((IFindReplaceTargetExtension3) target).replaceSelection(
                        replaceString,
                        regExSearch);
                    Point selection = target.getSelection();
                    replaceCount++;

                    if (forwardSearch)
                        findReplacePosition = selection.x + selection.y;
                    else
                    {
                        findReplacePosition = selection.x - 1;
                        if (findReplacePosition == -1)
                            break;
                    }
                }
            }
        }
        finally
        {
            if (target instanceof IFindReplaceTargetExtension)
                ((IFindReplaceTargetExtension) target)
                    .setReplaceAllMode(false);
        }
    }
}