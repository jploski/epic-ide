package org.epic.perleditor.editors;

import junit.framework.Assert;

import org.eclipse.core.runtime.ILog;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.epic.core.model.SourceFile;

import org.epic.perl.editor.test.BaseTestCase;
import org.epic.perl.editor.test.Log;
import org.epic.perl.editor.test.MockAnnotationModel;


/**
 * Unit test for <code>FoldReconciler</code>
 */
public class TestFoldReconciler extends BaseTestCase
{
    //~ Instance fields

    private MockAnnotationModel mockModel;
    private SourceFile sourceFile;

    //~ Methods

    public void testReconcile() throws Exception
    {
        FoldReconciler uut = new MockedFoldReconciler();

        // original test file
        sourceFile = loadSourceFile("workspace/EPICTest/test_Folding.pl");
        sourceFile.parse();

        uut.reconcile();
        Assert.assertEquals(7, mockModel.size());

        sourceFile = loadSourceFile("workspace/EPICTest/folding/test_folding1.pl");
        sourceFile.parse();

        uut.reconcile();
        Assert.assertEquals(6, mockModel.size());

        sourceFile = loadSourceFile("workspace/EPICTest/folding/test_folding2.pl");
        sourceFile.parse();

        uut.reconcile();
        Assert.assertEquals(4, mockModel.size());

        // TODO: additional tests
    }

    protected void setUp() throws Exception
    {
        mockModel = new MockAnnotationModel();
    }

    private SourceFile loadSourceFile(String file) throws Exception
    {
        Document doc = new Document(readFile(file));
        new PerlPartitioner(new Log(), doc);

        return new SourceFile(getLoggerForTests(), doc);
    }

    //~ Inner Classes

    /**
     * Testable sub-class
     */
    private class MockedFoldReconciler extends FoldReconciler
    {

        MockedFoldReconciler()
        {
            // safe only b/c all methods referencing have been overridden
            super(null);
        }

        protected IAnnotationModel getAnnotations()
        {
            return mockModel;
        }

        protected ILog getLog()
        {
            return getLoggerForTests();
        }

        protected boolean getPreference(String name)
        {
            return true;
        }

        protected SourceFile getSourceFile()
        {
            return sourceFile;
        }
    }
}
