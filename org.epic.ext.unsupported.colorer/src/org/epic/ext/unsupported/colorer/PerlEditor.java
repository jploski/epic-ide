/*
 * This file is a duplicate of org.epic.perleditor.editors.PerlEditor
 * to allow the use of the Colorer plugin
 * Changes are marked with todo tags!
 */

package org.epic.ext.unsupported.colorer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.util.IPropertyChangeListener;

import org.epic.perleditor.views.PerlOutlinePage;
import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.model.Module;
import org.epic.perleditor.views.model.Subroutine;
import org.epic.perleditor.PerlEditorPlugin;

import org.epic.perleditor.editors.*;
import net.sf.colorer.eclipse.editors.*;

/**
 * Perl specific text editor.
 */

public class PerlEditor
	extends ColorerEditor
	implements ISelectionChangedListener, IPropertyChangeListener {

	//implements ISelectionChangedListener {

	/** The outline page */
	private PerlContentOutlinePage fOutlinePage;
	protected PerlOutlinePage page;
	protected PerlSyntaxValidationThread fValidationThread = null;
	protected CompositeRuler ruler;
	protected LineNumberRulerColumn numberRuler;
	private boolean lineRulerActive = false;

	/**
	 * Default constructor();
	 */

	public PerlEditor() {
		super();
		
		// TODO Added to suppor Colorer Plugin
		setSourceViewerConfiguration(new PerlSourceViewerConfiguration(PerlEditorPlugin.getDefault().getPreferenceStore(), this));
		//setDocumentProvider(new ColoringDocumentProvider());
		
		PerlEditorPlugin
			.getDefault()
			.getPreferenceStore()
			.addPropertyChangeListener(
			this);
			

		this.setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());	
	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method extend the 
	 * actions to add those specific to the receiver
	 */

	protected void createActions() {
		super.createActions();
		
		IDocumentProvider provider = getDocumentProvider();
		IDocument document = provider.getDocument(getEditorInput());
		getSourceViewer().setDocument(document);
		if (fValidationThread == null) {
			fValidationThread =
				new PerlSyntaxValidationThread(this, getSourceViewer());
			//Thread defaults
			fValidationThread.start();
		}
		fValidationThread.setText(getSourceViewer().getTextWidget().getText());
		
	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * disposal actions required by the Perl editor.
	 */

	public void dispose() {
		try {
			IEditorInput input = this.getEditorInput();
			IResource resource =
				(IResource) ((IAdaptable) input).getAdapter(IResource.class);

			resource.deleteMarkers(IMarker.PROBLEM, true, 1);
			fValidationThread.dispose();
			super.dispose();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * revert behavior required by the Perl editor.
	 */

	public void doRevertToSaved() {

		super.doRevertToSaved();

	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * save behavior required by the Perl editor.
	 */

	public void doSave(IProgressMonitor monitor) {

		super.doSave(monitor);

		if (page != null) {
			page.update();
		}

		if (fValidationThread != null) {
			fValidationThread.setText(
				getSourceViewer().getTextWidget().getText());
		}

	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs any extra 
	 * save as behavior required by the Perl editor.
	 */

	public void doSaveAs() {

		super.doSaveAs();

		if (page != null) {
			page.update();
		}

		if (fValidationThread != null) {
			fValidationThread.setText(
				getSourceViewer().getTextWidget().getText());
		}

	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs sets the 
	 * input of the outline page after AbstractTextEditor has set input.
	 */

	public void doSetInput(IEditorInput input) throws CoreException {

		super.doSetInput(input);

	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method adds any 
	 * PerlEditor specific entries.
	 */

	public void editorContextMenuAboutToShow(MenuManager menu) {

		super.editorContextMenuAboutToShow(menu);

	}

	/** The <code>PerlEditor</code> implementation of this 
	 * <code>AbstractTextEditor</code> method performs gets
	 * the Perl content outline page if request is for a an 
	 * outline page.
	 */

	public Object getAdapter(Class required) {
		if (required.equals(IContentOutlinePage.class)) {
			IEditorInput input = getEditorInput();

			if (input instanceof IFileEditorInput) {
				page = new PerlOutlinePage(getSourceViewer());
				page.addSelectionChangedListener(this);
				return page;
			}

		}

		return super.getAdapter(required);
	}

	public void updateOutline() {
		if (page != null) {
			page.update();
		}

	}

	/* 
	 * Method declared on AbstractTextEditor
	 */
// TODO Changed to suppor Colorer Plugin
	protected void initializeEditor() {
		//super.initializeEditor();
		//TODO Changed to suppor Colorer Plugin
		//PerlEditorEnvironment.connect(this);
		setSourceViewerConfiguration(new PerlSourceViewerConfiguration(PerlEditorPlugin.getDefault().getPreferenceStore(), this));
		//setRulerContextMenuId("#TextRulerContext");
		
	}

	/*
		protected final ISourceViewer createSourceViewer(
			Composite parent,
			IVerticalRuler ruler,
			int styles) {
	
			ISourceViewer viewer = super.createSourceViewer(parent, ruler, styles);
	
			return viewer;
		}
		*/

	/*
		protected IVerticalRuler createVerticalRuler() {
			ruler = new CompositeRuler();
			ruler.addDecorator(0, new AnnotationRulerColumn(16));
	
			numberRuler = new LineNumberRulerColumn();
			numberRuler.setBackground(
				new Color(Display.getCurrent(), 230, 230, 230));
			numberRuler.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
	
			if (PerlEditorPlugin.getDefault().getShowLineNumbersPreference()) {
				ruler.addDecorator(1, numberRuler);
				lineRulerActive = true;
			}
	
			return ruler;
		}
		*/

	/**
	* @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	*/

	public void selectionChanged(SelectionChangedEvent event) {
		
		if (event != null) {
			if (event.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection sel =
					(IStructuredSelection) event.getSelection();
				if (sel != null) {
					if (sel.getFirstElement() instanceof Module
						|| sel.getFirstElement() instanceof Subroutine) {
						Model fe = (Model) sel.getFirstElement();
						if (fe != null) {
							selectAndReveal(fe.getStart(), fe.getLength());
						}
					}
				}
			}
		}
	}

	public void revalidateSyntax(boolean forceUpdate) {

		if (fValidationThread != null) {
			fValidationThread.setText(
				getSourceViewer().getTextWidget().getText(),
				forceUpdate);
		}

	}

	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		revalidateSyntax(false);
	
		if (page != null) {
			page.update();
		}
	}
	

	/*
		 * @see IPropertyChangeListener.propertyChange()
		 */
/* TODO Changed to suppor Colorer Plugin
	public void propertyChange(PropertyChangeEvent event) {
	
		if(event.getProperty().equals("PERL_EXECUTABLE")) {
			return;
		}
		
		
		//int topIndex =  getSourceViewer().getTextWidget().getTopIndex();
		//int carretOffset = getSourceViewer().getTextWidget().getCaretOffset();
		getSourceViewer().configure(new PerlSourceViewerConfiguration(PerlEditorPlugin.getDefault().getPreferenceStore(), this));

		IAnnotationModel model = getSourceViewer().getAnnotationModel();
		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		((SourceViewer)getSourceViewer()).refresh();
		getSourceViewer().setDocument(document, model);
		
		//getSourceViewer().getTextWidget().setTopIndex(topIndex);
		//getSourceViewer().getTextWidget().setCaretOffset(carretOffset);
	}
*/
	public ISourceViewer getViewer() {
		return getSourceViewer();
	}
}
