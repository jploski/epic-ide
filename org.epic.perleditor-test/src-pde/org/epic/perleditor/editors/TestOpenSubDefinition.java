package org.epic.perleditor.editors;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;

public class TestOpenSubDefinition extends BasePDETestCase
{
    public void testOpenSub1() throws Exception
    {
        testOpenSub("EPICTest/test_OpenSub.pl");
        testOpenSub("EPICTest/test_OpenSub2.pl");
    }

    public void testOpenSub2() throws Exception
    {
        PerlEditor editor = openEditor("EPICTest/test_OpenSub3.pl");
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        PerlEditor moduleEditor = null;

        try
        {
            testIface.selectText("main::some_sub");
            
            IAction openSubAction = editor.getAction(PerlEditorActionIds.OPEN_DECLARATION);
            openSubAction.run();

            // Check that the module editor opened with the right selection
            moduleEditor = findEditor("EPICTest/lib/TestOpenSub.pm");
            assertNotNull(moduleEditor);
            ISelectionProvider provider = moduleEditor.getSelectionProvider();
            ITextSelection selection = (ITextSelection) provider.getSelection();
            assertEquals("some_sub", selection.getText());
            assertEquals(
                "sub some_sub #ok",
                moduleEditor.getTestInterface().getText().substring(
                    selection.getOffset() - 4,
                    selection.getOffset() - 4 + "sub some_sub #ok".length()));
        }
        finally
        {
            closeEditor(editor);
            if (moduleEditor != null) closeEditor(moduleEditor);
        }
    }

    
    private void testOpenSub(String startFile) throws Exception
    {
        PerlEditor editor = openEditor(startFile);
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        PerlEditor moduleEditor = null;
        
        try
        {
            testIface.selectText("some_sub()");
            
            IAction openSubAction = editor.getAction(PerlEditorActionIds.OPEN_DECLARATION);
            openSubAction.run();
            
            // Check that the module editor opened with the right selection
            moduleEditor = findEditor("EPICTest/lib/TestOpenSub.pm");
            assertNotNull(moduleEditor);
            ISelectionProvider provider = moduleEditor.getSelectionProvider();
            ITextSelection selection = (ITextSelection) provider.getSelection();
            assertEquals("some_sub", selection.getText());
            assertEquals(
                "sub some_sub #ok",
                moduleEditor.getTestInterface().getText().substring(
                    selection.getOffset() - 4,
                    selection.getOffset() - 4 + "sub some_sub #ok".length()));
            
            IDocument doc1 = editor.getViewer().getDocument();
            appendText(doc1, "\n\nzzz();");
            
            IDocument doc2 = moduleEditor.getViewer().getDocument();
            appendText(doc2, "\n\nsub zzz { }");
            
            testIface.selectText("zzz");
            openSubAction.run();
            selection = (ITextSelection) provider.getSelection();
            assertEquals(
                "sub zzz",
                moduleEditor.getTestInterface().getText().substring(
                    selection.getOffset() - 4,
                    selection.getOffset() - 4 + "sub zzz".length()));
        }
        finally
        {
            closeEditor(editor);
            if (moduleEditor != null) closeEditor(moduleEditor);
        }
    }

    private void appendText(final IDocument doc, final String text)
    {            
        Display.getDefault().syncExec(
            new Runnable() {
                public void run()
                {
                    try { doc.replace(doc.getLength(), 0, text); }
                    catch (BadLocationException e)
                    {
                        throw new RuntimeException(e);
                    }
                } });
    }
}
