package org.epic.perleditor.views;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.jface.text.source.ISourceViewer;

import org.epic.perleditor.views.model.*;
import org.epic.perleditor.views.util.*;
import org.epic.perleditor.editors.perl.PerlPartitionScanner;

/**
 * DOCUMENT ME!
 * 
 * @author Addi To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates. To enable and disable the creation of type comments
 *         go to Window>Preferences>Java>Code Generation.
 */
public class PerlOutlinePage extends ContentOutlinePage {

	protected ISourceViewer input;
	protected int lastHashCode = 0;
	SourceElement subroutines;
	SourceElement modules;

	
	public PerlOutlinePage(ISourceViewer input) {
		super();
		this.input = input;
	}

	
	public void createControl(Composite parent) {
		super.createControl(parent);

		TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(new SourceElementContentProvider());
		viewer.setLabelProvider(new SourceElementLabelProvider());
		viewer.setInput(getInitalInput());
		viewer.setSorter(new NameSorter());
		
		// Tree is expanded by default
		viewer.expandAll();

	}

	

	public SourceElement getInitalInput() {
		SourceElement root = new SourceElement();
		modules = new SourceElement("Modules", SourceElement.MODULE_TYPE);
		subroutines =
			new SourceElement("Subroutines", SourceElement.SUBROUTINE_TYPE);
		SourceParser parser = new SourceParser();

		subroutines.addSubroutines(
			parser.getElements(
				input.getTextWidget().getText(),
				PerlPartitionScanner.TOKEN_SUBROUTINE,
				"{", "=;"));
		modules.addModules(
			parser.getElements(
				input.getTextWidget().getText(),
				PerlPartitionScanner.TOKEN_MODULE,
				";", "=>$"));

		root.add(modules);
		root.add(subroutines);

		return root;
	}

	
	public void update() {
		// Update only if input has changed
		int hashCode = input.getTextWidget().getText().hashCode();

		if (hashCode == lastHashCode) {
			return;
		}

		lastHashCode = hashCode;

		getControl().setRedraw(false);
		subroutines.removeChildren();
		modules.removeChildren();
		SourceParser parser = new SourceParser();
		subroutines.addSubroutines(
			parser.getElements(
				input.getTextWidget().getText(),
				PerlPartitionScanner.TOKEN_SUBROUTINE,
				"{", "=;"));
				
		modules.addModules(
			parser.getElements(
				input.getTextWidget().getText(),
				PerlPartitionScanner.TOKEN_MODULE,
				";", "=>$"));
		getTreeViewer().refresh(subroutines, false);
		getTreeViewer().refresh(modules, false);

        // Tree is expanded by default
		//getTreeViewer().expandAll();
		
		getControl().setRedraw(true);
	}

}


class NameSorter extends ViewerSorter {

}