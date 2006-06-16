package org.epic.perleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.MarkOccurrencesPreferences;

/**
 * Enables/disables the "Mark Occurrences" toolbar button.
 */
public class ToggleMarkOccurrencesAction extends Action
    implements IPropertyChangeListener
{
    //~ Instance fields

    private IPreferenceStore fStore;

    //~ Constructors

    public ToggleMarkOccurrencesAction()
    {
        fStore = PerlEditorPlugin.getDefault().getPreferenceStore();
        setChecked(fStore.getBoolean(MarkOccurrencesPreferences.MARK_OCCURRENCES));
        fStore.addPropertyChangeListener(this);
    }

    //~ Methods
    
    public void dispose()
    {
        if (fStore != null)
        {
            fStore.removePropertyChangeListener(this);
            fStore = null;
        }
    }
    
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(MarkOccurrencesPreferences.MARK_OCCURRENCES))
        {
            setChecked(Boolean.valueOf(event.getNewValue().toString()).booleanValue());
        }
    }

    public void run()
    {
        fStore.setValue(MarkOccurrencesPreferences.MARK_OCCURRENCES, isChecked());
    }
}
