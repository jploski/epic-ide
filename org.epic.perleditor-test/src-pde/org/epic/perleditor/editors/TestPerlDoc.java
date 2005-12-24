package org.epic.perleditor.editors;

import org.eclipse.jface.action.IAction;
import org.epic.perleditor.views.PerlDocView;

public class TestPerlDoc extends BasePDETestCase
{
    private static final String VIEW_ID =
        "org.epic.perleditor.views.PerlDocView";

    public void testAll() throws Exception
    {
        PerlEditor editor = openEditor("EPICTest/test_PerlDoc.pl");
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        PerlDocView view = null;
        
        try
        {
            view = (PerlDocView) findView(VIEW_ID);
            if (view != null) closeView(view);
            
            testIface.selectText("print");
            
            IAction perlDocAction = editor.getAction(PerlEditorActionIds.PERL_DOC);
            perlDocAction.run();
            
            view = (PerlDocView) findView(VIEW_ID);
            assertNotNull(view);
            assertTrue(
                view.getDisplayedText(0).indexOf("print FILEHANDLE LIST") != -1);
        }
        finally
        {
            if (view != null) closeView(view);
            closeEditor(editor);
        }
    }
}
