package org.epic.perleditor.editors;

import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
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

	protected void customizeDocumentCommand(DocumentCommand command) {
		if (command.text.equals("\t")) {
			int line            = getTextWidget().getLineAtOffset(command.offset);
			int lineStartOffset = getTextWidget().getOffsetAtLine(line);		
			int column          = command.offset - lineStartOffset;

			command.text = PreferenceUtil.getTab(column);
		}
		super.customizeDocumentCommand(command);
	}
}