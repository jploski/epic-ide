package org.epic.perleditor.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.ViewerActionBuilder;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.actions.IPerlEditorActionDefinitionIds;
import org.epic.perleditor.editors.util.PerlColorProvider;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.views.PerlOutlinePage;
import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.model.Module;
import org.epic.perleditor.views.model.Subroutine;

import cbg.editor.ColoringSourceViewerConfiguration;
//import java.util.Map;
//import java.util.HashMap;
//import org.eclipse.swt.graphics.Color;

/* Eclipse 3.0 extension (uncomment when using Eclipse 3)
import org.eclipse.ui.editors.text.ILocationProvider;
import org.epic.core.util.FileUtilities;
*/


/**
 * Perl specific text editor.
 */

public class PerlEditor
	extends TextEditor
	implements ISelectionChangedListener, IPropertyChangeListener {

	//implements ISelectionChangedListener {

	/** The outline page */
	private PerlContentOutlinePage fOutlinePage;
	protected PerlOutlinePage page;
	protected PerlSyntaxValidationThread fValidationThread = null;
	protected CompositeRuler ruler;
	protected LineNumberRulerColumn numberRuler;
	private boolean lineRulerActive = false;
	private SourceViewer fSourceViewer;
	private IDocumentProvider fDocumentProvider;
	private IdleTimer idleTimer;

	private final static String PERL_MODE = "perl";

	/**
	 * Default constructor();
	 */

	public PerlEditor() {
		super();
		//setDocumentProvider(new ColoringDocumentProvider());
		setDocumentProvider(new PerlDocumentProvider());

		PerlEditorPlugin
			.getDefault()
			.getPreferenceStore()
			.addPropertyChangeListener(
			this);

		this.setPreferenceStore(
			PerlEditorPlugin.getDefault().getPreferenceStore());
		setKeyBindingScopes(
			new String[] { "org.epic.perleditor.perlEditorScope" });

//		setRulerContextMenuId("#PerlRulerContext");
//		setEditorContextMenuId("#PerlDocEditorContext");
	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method extend the 
	 * actions to add those specific to the receiver
	 */

	protected void createActions() {
		super.createActions();

		Action action;
		// Create content assist action
		action =
			new ContentAssistAction(
				PerlEditorMessages.getResourceBundle(),
				"ContentAssistProposal.",
				this);
		action.setActionDefinitionId(
			ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("org.epic.perleditor.ContentAssist", action);

		// Only enable actions if in Perl mode
		if (isPerlMode()) {
			// Add comment action
			action =
				new TextOperationAction(
					PerlEditorMessages.getResourceBundle(),
					"Comment.",
					this,
					ITextOperationTarget.PREFIX);
			action.setActionDefinitionId(
				IPerlEditorActionDefinitionIds.COMMENT);
			setAction("org.epic.perleditor.Comment", action);

			// Add uncomment action
			action =
				new TextOperationAction(
					PerlEditorMessages.getResourceBundle(),
					"Uncomment.",
					this,
					ITextOperationTarget.STRIP_PREFIX);
			action.setActionDefinitionId(
				IPerlEditorActionDefinitionIds.UNCOMMENT);
			setAction("org.epic.perleditor.Uncomment", action);
		}

		IDocumentProvider provider = getDocumentProvider();
		IDocument document = provider.getDocument(getEditorInput());
		getSourceViewer().setDocument(document);

		fDocumentProvider = provider;
		fSourceViewer = (SourceViewer) getSourceViewer();

		if (fValidationThread == null && isPerlMode()) {
			fValidationThread =
				new PerlSyntaxValidationThread(this, getSourceViewer());
			//Thread defaults
			fValidationThread.start();
			
			// Always check syntax when editor is opened
			fValidationThread.onEditorIdle(getSourceViewer());
		}

		if (fValidationThread != null) {
			fValidationThread.setText(
				getSourceViewer().getTextWidget().getText());
		}

		setEditorForegroundColor();

		// Setup idle timer
		idleTimer = new IdleTimer(this.getSourceViewer(), Display.getCurrent());
		idleTimer.start();

		// Register the validation thread
		this.registerIdleListener(fValidationThread);

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
	 * disposal actions required by the Perl editor.
	 */

	public void dispose() {
		try {
			IEditorInput input = this.getEditorInput();
			IResource resource =
				(IResource) ((IAdaptable) input).getAdapter(IResource.class);

			resource.deleteMarkers(IMarker.PROBLEM, true, 1);

			if (fValidationThread != null) {
				fValidationThread.dispose();
			}

			if (idleTimer != null) {
				idleTimer.dispose();
			}

			if (fOutlinePage != null) {
				fOutlinePage.dispose();
			}

			super.dispose();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
	 * revert behavior required by the Perl editor.
	 */

	public void doRevertToSaved() {

		super.doRevertToSaved();

	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
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

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs any extra 
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

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs sets the 
	 * input of the outline page after AbstractTextEditor has set input.
	 */

	public void doSetInput(IEditorInput input) throws CoreException {

		/* Eclipse 3.0 extension (uncomment when using Eclipse 3)
		if (input instanceof ILocationProvider) {
			ILocationProvider l =
				(ILocationProvider) input.getAdapter(ILocationProvider.class);
			if (l != null)
				input = getFileEditorInput(l.getPath(l).makeAbsolute());
		}
		*/

		super.doSetInput(input);

		// Set coloring editor mode
		if (input instanceof IStorageEditorInput) {
			String filename =
				((IStorageEditorInput) input).getStorage().getName();
			(
				(ColoringSourceViewerConfiguration) getSourceViewerConfiguration())
					.setFilename(
				filename);
		}
	}
	
	public void rulerContextMenuAboutToShow(IMenuManager menu)
	{
		System.out.println(menu.getId());
		super.rulerContextMenuAboutToShow(menu);
		ViewerActionBuilder builder = new ViewerActionBuilder();
		builder.readViewerContributions("#PerlRulerContext", getSelectionProvider(), this);
		builder.contribute(menu,null,true);
	}

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method adds any 
	 * PerlEditor specific entries.
	 */

	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		ViewerActionBuilder builder = new ViewerActionBuilder();
		builder.readViewerContributions("#PerlDocEditorContext", getSelectionProvider(), this);
		builder.contribute(menu,null,true);
	}
	
	

	/** The PerlEditor implementation of this 
	 * AbstractTextEditor method performs gets
	 * the Perl content outline page if request is for a an 
	 * outline page.
	 */

	public Object getAdapter(Class required) {
		if (required.equals(IContentOutlinePage.class)) {
			IEditorInput input = getEditorInput();

			if (input instanceof IFileEditorInput) {
				page = new PerlOutlinePage(getSourceViewer());

				this.registerIdleListener(page);

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
	protected void initializeEditor() {
		//PerlEditorEnvironment.connect(this);
		setSourceViewerConfiguration(
			new PerlSourceViewerConfiguration(
				PerlEditorPlugin.getDefault().getPreferenceStore(),
				this));
		//setRulerContextMenuId("#TextRulerContext");
		super.initializeEditor();
	}

	/* Create SourceViewer so we can use the PerlSourceViewer class */
	protected final ISourceViewer createSourceViewer(
		Composite parent,
		IVerticalRuler ruler,
		int styles) {
		
		fAnnotationAccess= createAnnotationAccess();
		fOverviewRuler= createOverviewRuler(getSharedColors());

		ISourceViewer sourceViewer =
			new PerlSourceViewer(
				parent,
				ruler,
				fOverviewRuler,
				isOverviewRulerVisible(),
				styles);

		// ensure source viewer decoration support has been created and configured
		getSourceViewerDecorationSupport(sourceViewer);    

		return sourceViewer;
	}
	

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
	}

	/*
		 * @see IPropertyChangeListener.propertyChange()
		 */

	public void propertyChange(PropertyChangeEvent event) {

		if (event.getProperty().equals("PERL_EXECUTABLE")) {
			return;
		}

		PerlSourceViewerConfiguration viewerConfiguration =
			(PerlSourceViewerConfiguration) this.getSourceViewerConfiguration();
		viewerConfiguration.adaptToPreferenceChange(event);

		IAnnotationModel model = fSourceViewer.getAnnotationModel();
		IDocument document = fDocumentProvider.getDocument(getEditorInput());
		fSourceViewer.refresh();
		fSourceViewer.setDocument(document, model);

		setEditorForegroundColor();

	}

	public ISourceViewer getViewer() {
		return getSourceViewer();
	}

	/**
	 * Checks if perlmode is used by the editor
	 * @return true if in perl mode, otherwise false
	 */
	public boolean isPerlMode() {
		return getModeName().equalsIgnoreCase(PERL_MODE);
	}

	/**
	 * Returns the node name used by the editor
	 * @return Mode name
	 */
	public String getModeName() {
		String modeName =
			((PerlSourceViewerConfiguration) getSourceViewerConfiguration())
				.getMode()
				.getDisplayName();
		return modeName;
	}

	public boolean registerIdleListener(Object obj) {
		return idleTimer.addListener(obj);
	}

	private void setEditorForegroundColor() {
		// Set text editor forground colour
		RGB rgb =
			PreferenceConverter.getColor(
				PerlEditorPlugin.getDefault().getPreferenceStore(),
				PreferenceConstants.EDITOR_STRING_COLOR);
		getSourceViewer().getTextWidget().setForeground(
			PerlColorProvider.getColor(rgb));
	}

}