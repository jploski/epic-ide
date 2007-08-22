package org.epic.perleditor.editors;

import java.io.File;

import org.eclipse.jface.text.IDocument;

public class TestEditorAssociation extends BasePDETestCase
{
    public void testOpen() throws Exception
    {   
        // This test checks whether opening files with a strange extension
        // which are associated with the PerlEditor works as expected.

        assertOkay(openEditor("EPICTest/test_EditorAssociation.pre"));
    }
    
    public void testOpenExternal() throws Exception
    {
        // Same as above, this time with a file located outside
        // of the workspace
        
        final File tmpFile = File.createTempFile(
            "EPIC-TestEditorAssociation", ".pl");

        try
        {
            writeToFile(tmpFile, "#!/usr/bin/perl\nprint 'Hello, world!';");
            assertOkay(openEditor(tmpFile));
        }
        finally
        { 
            tmpFile.delete();
        }        
    }
    
    private void assertOkay(PerlEditor editor) throws Exception
    {
        try
        {
            IDocument doc = editor.getViewer().getDocument();
            assertTrue(PartitionTypes.getPerlPartitioner(doc) instanceof PerlPartitioner);            
            assertTrue(editor.getViewer().getAnnotationModel() instanceof PerlSourceAnnotationModel);
        }
        finally
        {
            closeEditor(editor);
        }
    }   
}