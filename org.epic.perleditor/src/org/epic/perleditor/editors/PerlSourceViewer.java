package org.epic.perleditor.editors;

import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;

import org.epic.perleditor.editors.util.PreferenceUtil;

public class PerlSourceViewer extends ProjectionViewer {

	public PerlSourceViewer(
		Composite parent,
		IVerticalRuler ruler,
		IOverviewRuler overviewRuler,
		boolean showAnnotationsOverview,
		int styles) {
		super(parent, ruler, overviewRuler, showAnnotationsOverview, styles);
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
}