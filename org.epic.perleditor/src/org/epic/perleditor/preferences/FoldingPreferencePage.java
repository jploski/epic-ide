package org.epic.perleditor.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.epic.core.preferences.LabelFieldEditor;
import org.epic.core.preferences.SpacerFieldEditor;

import org.epic.perleditor.PerlEditorPlugin;


/**
 *Folding preferences page
 */
public class FoldingPreferencePage extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{
    //~ Constructors

    public FoldingPreferencePage()
    {
        super(GRID);
        setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());
    }

    //~ Methods

    /*
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench)
    {
        // empty impl
    }

    /*
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors()
    {
        addField(new BooleanFieldEditor(PreferenceConstants.SOURCE_FOLDING,
                PreferencesMessages.sourceFolding, getFieldEditorParent()));

        addField(new SpacerFieldEditor(getFieldEditorParent()));

        addField(new LabelFieldEditor(PreferencesMessages.initiallyFold, getFieldEditorParent()));

        addField(new BooleanFieldEditor(PreferenceConstants.PERLDOC_FOLDING,
                PreferencesMessages.perldocFolding, getFieldEditorParent()));

        addField(new BooleanFieldEditor(PreferenceConstants.SUBROUTINE_FOLDING,
                PreferencesMessages.subroutineFolding, getFieldEditorParent()));
    }
}
