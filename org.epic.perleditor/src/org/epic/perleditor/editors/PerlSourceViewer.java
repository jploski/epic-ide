package org.epic.perleditor.editors;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PreferenceUtil;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PerlSourceViewer extends ProjectionViewer {

    private IVerticalRulerColumn foldColumn;
    
	public PerlSourceViewer(
		Composite parent,
		IVerticalRuler ruler,
		IOverviewRuler overviewRuler,
		boolean showAnnotationsOverview,
		int styles) {
		super(parent, ruler, overviewRuler, showAnnotationsOverview, styles);
	}
    
    public void addVerticalRulerColumn(IVerticalRulerColumn column)
    {
        super.addVerticalRulerColumn(column);
        if (foldColumn == null)
        {
            foldColumn = column;
            updateFoldColumnBackgroundColor(false);
        }
    }
    
    IVerticalRuler _getVerticalRuler()
    {
        return super.getVerticalRuler();
    }

	protected void customizeDocumentCommand(DocumentCommand command) {
		try {
			if (command.text.equals("\t")) {
				int line            = getDocument().getLineOfOffset(command.offset);
				int lineStartOffset = getDocument().getLineOffset(line);		
				int column          = command.offset - lineStartOffset;

				command.text = PreferenceUtil.getTab(column);
			}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.customizeDocumentCommand(command);
	}

    public void updateFoldColumnBackgroundColor(boolean force)
    {
        // We only mess with the background color of the fold column
        // if the user has changed the preference
        
        RGB rgb = PreferenceConverter.getColor(
            PerlEditorPlugin.getDefault().getPreferenceStore(),
            PreferenceConstants.EDITOR_FOLD_COLUMN_BG_COLOR);
        
        if (force || !rgb.equals(PreferenceConstants.EDITOR_FOLD_COLUMN_BG_COLOR_DEFAULT))
        {
            foldColumn.getControl().setBackground(
                PerlEditorPlugin.getDefault().getColor(
                    PreferenceConstants.EDITOR_FOLD_COLUMN_BG_COLOR));
        }
    }
}