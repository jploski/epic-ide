package org.epic.perleditor.editors;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * Tests that open-declaration navigation works for arrow-qualified
 * symbols, i.e. 'TestOpenArrow->MAX_SIZE' and 'TestOpenArrow->compute_value()'.
 */
public class TestOpenArrowDefinition extends BasePDETestCase
{
    public void testOpenArrowConstant() throws Exception
    {
        PerlEditor editor = openEditor("EPICTest/test_OpenArrow.pl");
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        PerlEditor moduleEditor = null;

        try
        {
            testIface.selectText("MAX_SIZE");

            IAction openDeclAction = editor.getAction(PerlEditorActionIds.OPEN_DECLARATION);
            openDeclAction.run();

            moduleEditor = findEditor("EPICTest/lib/TestOpenArrow.pm");
            assertNotNull(moduleEditor);
            ISelectionProvider provider = moduleEditor.getSelectionProvider();
            ITextSelection selection = (ITextSelection) provider.getSelection();
            assertEquals("MAX_SIZE", selection.getText());
            assertEquals(
                "use constant MAX_SIZE",
                moduleEditor.getTestInterface().getText().substring(
                    selection.getOffset() - "use constant ".length(),
                    selection.getOffset() + selection.getLength()));
        }
        finally
        {
            closeEditor(editor);
            if (moduleEditor != null) closeEditor(moduleEditor);
        }
    }

    public void testOpenArrowMethod() throws Exception
    {
        PerlEditor editor = openEditor("EPICTest/test_OpenArrow.pl");
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        PerlEditor moduleEditor = null;

        try
        {
            testIface.selectText("compute_value");

            IAction openDeclAction = editor.getAction(PerlEditorActionIds.OPEN_DECLARATION);
            openDeclAction.run();

            moduleEditor = findEditor("EPICTest/lib/TestOpenArrow.pm");
            assertNotNull(moduleEditor);
            ISelectionProvider provider = moduleEditor.getSelectionProvider();
            ITextSelection selection = (ITextSelection) provider.getSelection();
            assertEquals("compute_value", selection.getText());
            assertEquals(
                "sub compute_value #ok",
                moduleEditor.getTestInterface().getText().substring(
                    selection.getOffset() - "sub ".length(),
                    selection.getOffset() - "sub ".length()
                        + "sub compute_value #ok".length()));
        }
        finally
        {
            closeEditor(editor);
            if (moduleEditor != null) closeEditor(moduleEditor);
        }
    }
}
