package org.epic.perleditor.editors;

import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.jface.text.DocumentCommand;

import org.epic.perleditor.editors.util.PreferenceUtil;

public class PerlSourceViewer extends SourceViewer {

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
			command.text = PreferenceUtil.getTab();
		}
		super.customizeDocumentCommand(command);
	}
}