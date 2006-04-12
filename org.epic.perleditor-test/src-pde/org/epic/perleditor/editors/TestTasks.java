package org.epic.perleditor.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IFileEditorInput;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.TaskTagPreferences;

public class TestTasks extends BasePDETestCase
{
    public void testAll() throws Exception
    {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        
        boolean p1 = prefs.getBoolean(TaskTagPreferences.ID_WHITESPACE);
    
        try { _testAll(); }
        finally
        {
            prefs.setValue(TaskTagPreferences.ID_WHITESPACE, p1);
        }
    }
    
    private void _testAll() throws Exception
    {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        
        prefs.setValue(TaskTagPreferences.ID_WHITESPACE, true);
        
        PerlEditor editor = openEditor("EPICTest/test_Tasks.pl");
        
        try
        {
            IResource res = ((IFileEditorInput) editor.getEditorInput()).getFile();
            IMarker[] markers = res.findMarkers(
                IMarker.TASK,
                true,
                IResource.DEPTH_ONE);
            
            assertEquals(2, markers.length);

            int line1 = ((Integer) markers[0].getAttribute(IMarker.LINE_NUMBER)).intValue();
            int line2 = ((Integer) markers[1].getAttribute(IMarker.LINE_NUMBER)).intValue();

            assertTrue(
                line1 == 3 && line2 == 4 ||
                line1 == 4 && line2 == 3
                );        
        }
        finally
        {
            closeEditor(editor);
        }
    }
}
