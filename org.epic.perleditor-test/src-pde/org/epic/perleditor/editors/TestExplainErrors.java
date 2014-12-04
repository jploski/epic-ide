package org.epic.perleditor.editors;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.epic.perleditor.views.ExplainErrorsView;

public class TestExplainErrors extends BasePDETestCase
{
    private static final String VIEW_ID =
        "org.epic.perleditor.views.ExplainErrorsView";

    private static final String ACTION_ID =
        "org.epic.perleditor.popupmenus.ExplainErrorsRulerActionDelegate";

    public void testAll() throws Exception
    {
        PerlEditor editor = openEditor("EPICTest/test_ExplainErrors.pl");
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        ExplainErrorsView view = null;
        
        try
        {
            view = (ExplainErrorsView) findView(VIEW_ID);
            if (view != null) closeView(view);
            
            IVerticalRuler ruler = testIface.getVerticalRuler();
            IAnnotationModel model = ruler.getModel();
            
            IMarker marker = getErrorMarker(model);
            assertNotNull(marker);

            editor.addRulerContextMenuListener(new PopupActionRunner(ACTION_ID));
            
            Mouse.click(getMarkerLocation(editor, marker), true);

            view = (ExplainErrorsView) findView(VIEW_ID);
            assertNotNull(view);
            assertTrue(view.getText().startsWith("Bareword"));
        }
        finally
        {
            if (view != null) closeView(view);
            closeEditor(editor);
        }
    }
    
    private IMarker getErrorMarker(IAnnotationModel model)
    {
        for (@SuppressWarnings("unchecked")
		Iterator<Annotation> i = model.getAnnotationIterator(); i.hasNext();)
        {
            Object obj = i.next();
            if (obj instanceof MarkerAnnotation)
                return ((MarkerAnnotation) obj).getMarker();
        }
        return null;
    }
    
    private Point getMarkerLocation(PerlEditor editor, IMarker marker)
        throws CoreException
    {
        int lineNo =
            ((Integer) marker.getAttribute(IMarker.LINE_NUMBER)).intValue();
        
        PerlEditor.TestInterface testIface = editor.getTestInterface();
        IVerticalRuler ruler = testIface.getVerticalRuler();
        
        testIface.setTopIndex(lineNo-1);
        
        int y1 = 0, y2;
        while (ruler.toDocumentLineNumber(y1) < lineNo-1) y1++;
        y2 = y1;
        while (ruler.toDocumentLineNumber(y2) < lineNo) y2++;
        
        return ruler.getControl().toDisplay(ruler.getWidth()/3, y1+(y2-y1)/2);
    }
}
