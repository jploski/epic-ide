package org.epic.perleditor.editors;

import org.eclipse.jface.text.IDocument;

public class TestEditorAssociation extends BasePDETestCase
{
    public void testAll() throws Exception
    {   
        // This test checks whether opening files with a strang extension
        // which are associated with the PerlEditor works as expected.

        PerlEditor editor = openEditor("EPICTest/test_EditorAssociation.pre");

        try
        {
            IDocument doc = editor.getViewer().getDocument();
            assertTrue(doc.getDocumentPartitioner() instanceof PerlPartitioner);
            assertTrue(editor.getViewer().getAnnotationModel() instanceof PerlSourceAnnotationModel);
        }
        finally
        {
            closeEditor(editor);
        }
    }
}