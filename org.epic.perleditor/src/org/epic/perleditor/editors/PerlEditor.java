package org.epic.perleditor.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
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

import org.epic.core.util.FileUtilities;

/**
 * Perl specific text editor.
 */

public class PerlEditor extends TextEditor implements
		ISelectionChangedListener, IPropertyChangeListener {

	//implements ISelectionChangedListener {

	/** The outline page */
	private PerlContentOutlinePage fOutlinePage;

	protected PerlOutlinePage page;

	protected PerlSyntaxValidationThread fValidationThread = null;

	protected PerlToDoMarkerThread fTodoMarkerThread = null;

	protected PerlFoldingThread fFoldingThread = null;

	protected CompositeRuler ruler;

	protected LineNumberRulerColumn numberRuler;

	private int lastHashCode = 0;

	private int lastTextLength = 0;

	private int lastCursorPos = 0;

	private String lastCursorChar = "  ";

	private boolean isStyleRangeChanged = false;

	private StyleRange myLastStyleRange = new StyleRange();

	private StyleRange newStyleRange = new StyleRange();

	private final org.eclipse.swt.graphics.Color tempColorBack = new org.eclipse.swt.graphics.Color(
			null, 87, 207, 215);

	private final org.eclipse.swt.graphics.Color tempColorFore = new org.eclipse.swt.graphics.Color(
			null, 255, 255, 0);

	private final String matchBrakets = "{([<>])}";

	private SourceViewer fSourceViewer;

	private IDocumentProvider fDocumentProvider;

	private IdleTimer idleTimer;

	private final static String PERL_MODE = "perl";

	private ProjectionSupport projectionSupport;

	/**
	 * Default constructor();
	 */

	public PerlEditor() {
		super();
		//setDocumentProvider(new ColoringDocumentProvider());
		setDocumentProvider(new PerlDocumentProvider());

		PerlEditorPlugin.getDefault().getPreferenceStore()
				.addPropertyChangeListener(this);

		this.setPreferenceStore(PerlEditorPlugin.getDefault()
				.getPreferenceStore());
		setKeyBindingScopes(new String[] { "org.epic.perleditor.perlEditorScope" });

		//		setRulerContextMenuId("#PerlRulerContext");
		//		setEditorContextMenuId("#PerlDocEditorContext");
	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method extend
	 * the actions to add those specific to the receiver
	 */

	protected void createActions() {
		super.createActions();

		Action action;
		// Create content assist action
		action = new ContentAssistAction(
				PerlEditorMessages.getResourceBundle(),
				"ContentAssistProposal.", this);
		action
				.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("org.epic.perleditor.ContentAssist", action);

		// Only enable actions if in Perl mode
		if (isPerlMode()) {
			// Add comment action
			action = new TextOperationAction(PerlEditorMessages
					.getResourceBundle(), "Comment.", this,
					ITextOperationTarget.PREFIX);
			action
					.setActionDefinitionId(IPerlEditorActionDefinitionIds.COMMENT);
			setAction("org.epic.perleditor.Comment", action);

			// Add uncomment action
			action = new TextOperationAction(PerlEditorMessages
					.getResourceBundle(), "Uncomment.", this,
					ITextOperationTarget.STRIP_PREFIX);
			action
					.setActionDefinitionId(IPerlEditorActionDefinitionIds.UNCOMMENT);
			setAction("org.epic.perleditor.Uncomment", action);
		}

		IDocumentProvider provider = getDocumentProvider();
		IDocument document = provider.getDocument(getEditorInput());
		getSourceViewer().setDocument(document);

		fDocumentProvider = provider;
		fSourceViewer = (SourceViewer) getSourceViewer();

		if (fValidationThread == null && isPerlMode()) {
			fValidationThread = new PerlSyntaxValidationThread(this,
					getSourceViewer());
			// Set thread priority to minimal
			fValidationThread.setPriority(Thread.MIN_PRIORITY);
			//Thread defaults
			fValidationThread.start();

		}

		if (fValidationThread != null) {
			try {
				// Give the validation thread time for initialization
				// TODO Find better solution
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Always check syntax when editor is opened
			//fValidationThread.setText(getSourceViewer().getTextWidget().getText());
			fValidationThread.setText(getSourceViewer().getDocument().get());
		}

		//set up the ToDoMarkerThread
		if ((fTodoMarkerThread == null) && isPerlMode()) {
			fTodoMarkerThread = new PerlToDoMarkerThread(this,
					getSourceViewer());
			fTodoMarkerThread.start();
		}

		// set up the FoldingThread
		if ((fFoldingThread == null) && isPerlMode()) {
			fFoldingThread = new PerlFoldingThread(this, getSourceViewer());
			fFoldingThread.start();
		}

		setEditorForegroundColor();

		// Setup idle timer
		idleTimer = new IdleTimer(this.getSourceViewer(), Display.getCurrent());
		idleTimer.start();

		// Register the validation thread
		this.registerIdleListener(fValidationThread);
		this.registerIdleListener(fTodoMarkerThread);
		this.registerIdleListener(fFoldingThread);

		newStyleRange.background = tempColorBack;
		newStyleRange.foreground = tempColorFore;
		newStyleRange.length = 1;
		newStyleRange.start = 0;

	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method performs
	 * any extra disposal actions required by the Perl editor.
	 */

	public void dispose() {
		try {
			IEditorInput input = this.getEditorInput();
			IResource resource = (IResource) ((IAdaptable) input)
					.getAdapter(IResource.class);

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

			if (fTodoMarkerThread != null) {
				fTodoMarkerThread.dispose();
			}

			if (fFoldingThread != null) {
				fFoldingThread.dispose();
			}

			super.dispose();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method performs
	 * any extra revert behavior required by the Perl editor.
	 */

	public void doRevertToSaved() {

		super.doRevertToSaved();

	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method performs
	 * any extra save behavior required by the Perl editor.
	 */

	public void doSave(IProgressMonitor monitor) {

		super.doSave(monitor);

		if (page != null) {
			page.update(page.getSubList(), page.getModList());
		}

		if (fValidationThread != null) {
			//fValidationThread.setText(getSourceViewer().getTextWidget().getText());
			fValidationThread.setText(getSourceViewer().getDocument().get());
		}

	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method performs
	 * any extra save as behavior required by the Perl editor.
	 */

	public void doSaveAs() {

		super.doSaveAs();

		if (page != null) {
			page.update(page.getSubList(), page.getModList());
		}

		if (fValidationThread != null) {
			//fValidationThread.setText(getSourceViewer().getTextWidget().getText());
			fValidationThread.setText(getSourceViewer().getDocument().get());
		}

	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method performs
	 * sets the input of the outline page after AbstractTextEditor has set
	 * input.
	 */

	public void doSetInput(IEditorInput input) throws CoreException {

		/* Map external files into workspace (epic-links) */
		if (input instanceof ILocationProvider) {
			ILocationProvider l = (ILocationProvider) input
					.getAdapter(ILocationProvider.class);
			if (l != null)
				input = FileUtilities.getFileEditorInput(l.getPath(l)
						.makeAbsolute());
		}

		super.doSetInput(input);

		// Set coloring editor mode
		if (input instanceof IStorageEditorInput) {
			String filename = ((IStorageEditorInput) input).getStorage()
					.getName();
			((ColoringSourceViewerConfiguration) getSourceViewerConfiguration())
					.setFilename(filename);
		}
	}

	public void rulerContextMenuAboutToShow(IMenuManager menu) {
		System.out.println(menu.getId());
		super.rulerContextMenuAboutToShow(menu);
		ViewerActionBuilder builder = new ViewerActionBuilder();
		builder.readViewerContributions("#PerlRulerContext",
				getSelectionProvider(), this);
		builder.contribute(menu, null, true);
	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method adds any
	 * PerlEditor specific entries.
	 */

	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		ViewerActionBuilder builder = new ViewerActionBuilder();
		builder.readViewerContributions("#PerlEditorContext",
				getSelectionProvider(), this);
		builder.contribute(menu, null, true);
	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method performs
	 * gets the Perl content outline page if request is for a an outline page.
	 */

	public Object getAdapter(Class adapter) {

		if (ProjectionAnnotationModel.class.equals(adapter)) {
			if (this.projectionSupport != null) {
				Object result = this.projectionSupport.getAdapter(
						getSourceViewer(), adapter);
				if (result != null) {
					return result;
				}
			}
		}

		if (adapter.equals(IContentOutlinePage.class)) {
			IEditorInput input = getEditorInput();

			if (input instanceof IFileEditorInput) {
				page = new PerlOutlinePage(getSourceViewer());

				this.registerIdleListener(page);

				page.addSelectionChangedListener(this);
				return page;
			}

		}

		return super.getAdapter(adapter);
	}

	public void updateOutline() {
		if (page != null) {
			page.update(page.getSubList(), page.getModList());
		}

	}

	/*
	 * Method declared on AbstractTextEditor
	 */
	protected void initializeEditor() {
		//PerlEditorEnvironment.connect(this);
		setSourceViewerConfiguration(new PerlSourceViewerConfiguration(
				PerlEditorPlugin.getDefault().getPreferenceStore(), this));
		//setRulerContextMenuId("#TextRulerContext");
		super.initializeEditor();
	}

	/* Create SourceViewer so we can use the PerlSourceViewer class */
	protected final ISourceViewer createSourceViewer(Composite parent,
			IVerticalRuler ruler, int styles) {

		fAnnotationAccess = createAnnotationAccess();
		fOverviewRuler = createOverviewRuler(getSharedColors());

		ISourceViewer sourceViewer = new PerlSourceViewer(parent, ruler,
				fOverviewRuler, isOverviewRulerVisible(), styles);

		// ensure source viewer decoration support has been created and
		// configured
		getSourceViewerDecorationSupport(sourceViewer);

		return sourceViewer;
	}

	/**
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */

	public void selectionChanged(SelectionChangedEvent event) {
		if (event != null) {
			if (event.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection sel = (IStructuredSelection) event
						.getSelection();
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
			//fValidationThread.setText(getSourceViewer().getTextWidget().getText(),
			// forceUpdate);
			fValidationThread.setText(getSourceViewer().getDocument().get(),
					forceUpdate);
		}

	}

	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();

		StyledText myText = getSourceViewer().getTextWidget();
		int cursorPosition = myText.getCaretOffset();
		System.out.println("LeO Cursor at " + cursorPosition);
		System.out.println("Hashcode=" + myText.getText().hashCode());
		System.out.println("lastCurPos 1=" + lastCursorPos);

		int currentTextLength = myText.getText().length();

		/*
		 * The main complexity of the Bracket matching is the cursor momevent.
		 * The most trickiest situation is the Alt+ArrowUp/ArrowDown which swaps
		 * the texts This swapping is sent in up to 3 different events, 1 for
		 * the Alt, one for the text change and finally one for the cursor
		 * position change. And only the final one has to be considered! and
		 * this only because the Selection-Counter is set to 0! (Eclipse 3.0)
		 * blah, blah, blah... HOPEfully I (LeO) handled all possible situations
		 * about the bracket matching!
		 */

		//we only make something (marking, adding, deleting), if NOTHING is
		// selected
		if (myText.getSelectionCount() == 0) {
			//if we have marked something => unmark it right now
			if (isStyleRangeChanged) {
				resetStyle(myText, currentTextLength);
			}

			//Bracket matching makes only sense for Cursor > 0[there is nothing
			// before Positon 0]
			if (cursorPosition > 0) {
				char sourceChar = '\u0000';
				//we cannot make one statement, cause the initial cursor=0 and
				// could
				// change!!!
				if (lastCursorPos != cursorPosition) {
					//we have really changed the postion => let's recacluate
					// the whole stuff
					if (myText.getStyleRangeAtOffset(cursorPosition - 1) == null) {
						System.out.println("NULLLL at " + (cursorPosition - 1));
					}
					int nextClip;
					sourceChar = myText.getTextRange(cursorPosition - 1, 1)
							.charAt(0);
					//check if we should mark something like the brackets
					//if (cursorPosition > 0) {
					if (matchBrakets.indexOf(sourceChar) >= 0) {
						nextClip = findNextOccurance(myText.getText(),
								sourceChar, cursorPosition);
						if (nextClip >= 0) {
							setStyleChar(nextClip, myText);
						}
					}
				}

				if (myText.getText().hashCode() != lastHashCode) {
					if (currentTextLength < lastTextLength) {
						//something was deleted
						//we could delete the pair, if we could get
						// information, what was
						// deleted!
						char delChar = ' ';
						if (cursorPosition == lastCursorPos) {
							//delete on the right sight (via DEL-Key)
							delChar = lastCursorChar.charAt(1);
						} else {
							//delete on the left sight (via Backspace-Key)
							delChar = lastCursorChar.charAt(0);
						}
						//            if (delChar == '[') {
						//              if (myText.getTextRange(cursorPosition, 1).charAt(0)
						// == ']') {
						//                myText.replaceTextRange(cursorPosition, 1, "");
						//              }
						//            }
					} else if (currentTextLength > lastTextLength) {
						//something was added
						String addChar = "";
						switch (sourceChar) {
						case '[':
							addChar = "]";
							break;
						case '{':
							addChar = "}";
							break;
						case '(':
							addChar = ")";
							break;
						case '<':
							addChar = ">";
							break;
						case '\"':
							check2Insert(myText, cursorPosition, '\"');
							break;
						case '\'':
							check2Insert(myText, cursorPosition, '\'');
							break;
						}
						if (addChar.length() == 1) {
							myText.insert(addChar);
							setStyleChar(cursorPosition, myText);
						}
					}
				}
			}
		}
		//compute the new values
		lastHashCode = myText.getText().hashCode();
		lastTextLength = myText.getText().length();
		lastCursorPos = myText.getCaretOffset();
		System.out.println("lastCurPos 2=" + lastCursorPos);
		if (lastTextLength == 0) {
			lastCursorChar = "  ";
		} else if (lastTextLength == 1) {
			lastCursorChar = myText.getText() + " ";
		} else {
			if (lastCursorPos == 0) {
				lastCursorChar = " " + myText.getTextRange(0, 1);
			} else {
				if ((lastTextLength - lastCursorPos + 1) > 1) {
					lastCursorChar = myText.getTextRange(lastCursorPos - 1, 2);
				} else {
					lastCursorChar = myText.getTextRange(lastCursorPos - 1, 1)
							+ " ";
				}
			}
		}
	}

	/*
	 * @see IPropertyChangeListener.propertyChange()
	 */

	/**
	 * Sets the Text back to original style! (Only in case we found a changed
	 * Style)
	 * 
	 * @param myText
	 * @param currentTextLength
	 */
	private void resetStyle(StyledText myText, int currentTextLength) {
		int posChange = myLastStyleRange.start;
		if (posChange >= myText.getCharCount()) {
			//the last changed position is out of reach, i.e. something was
			// deleted
			posChange += currentTextLength - lastTextLength;
		} else if (!newStyleRange.equals(myText
				.getStyleRangeAtOffset(posChange))) {
			posChange += currentTextLength - lastTextLength;
		}
		if (posChange >= 0 && posChange <= currentTextLength) {
			if (newStyleRange.equals(myText.getStyleRangeAtOffset(posChange))) {
				myLastStyleRange.start = posChange;
				myText.setStyleRange(myLastStyleRange);
			}
			isStyleRangeChanged = false;
		}
	}

	/**
	 * checks if checkChar should be inserted or not (input was done via
	 * console)
	 * 
	 * @param myText
	 * @param cursorPosition
	 * @param checkChar
	 */
	private void check2Insert(StyledText myText, int cursorPosition,
			char checkChar) {
		String checkText = myText.getText();
		int pos = cursorPosition - 2;
		int muCounter = 0;
		while (pos >= 0 && checkText.charAt(pos) == checkChar) {
			muCounter += 1;
			pos -= 1;
		}
		if (muCounter % 2 == 1) {
			if (checkText.charAt(cursorPosition - 2) != '\\') {
				myText.insert(String.valueOf(checkChar));
			}
		} else {
			if (checkText.charAt(cursorPosition + 1) == checkChar) {
				myText.setCaretOffset(cursorPosition + 1);
			} else if (checkText.charAt(cursorPosition - 2) != '\\') {
				myText.insert(String.valueOf(checkChar));
			}
		}
	}

	/**
	 * Sets the style for a given postion - only useful, when at least one char
	 * input exists
	 * 
	 * @param stylePosition
	 * @param myText
	 */
	private void setStyleChar(int stylePosition, StyledText myText) {
		if (stylePosition >= myText.getCharCount()) {
			stylePosition = myText.getCharCount() - 1;
		}
		myLastStyleRange = myText.getStyleRangeAtOffset(stylePosition);
		newStyleRange.start = stylePosition;
		myText.setStyleRange(newStyleRange);
		isStyleRangeChanged = true;
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals("PERL_EXECUTABLE")) {
			return;
		}
		setEditorForegroundColor();

	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
		projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
		projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
		
		projectionSupport.install();
		
		viewer.doOperation(ProjectionViewer.TOGGLE);
	}

	public ISourceViewer getViewer() {
		return getSourceViewer();
	}

	/**
	 * Checks if perlmode is used by the editor
	 * 
	 * @return true if in perl mode, otherwise false
	 */
	public boolean isPerlMode() {
		return getModeName().equalsIgnoreCase(PERL_MODE);
	}

	/**
	 * Returns the node name used by the editor
	 * 
	 * @return Mode name
	 */
	public String getModeName() {
		String modeName = ((PerlSourceViewerConfiguration) getSourceViewerConfiguration())
				.getMode().getDisplayName();
		return modeName;
	}

	public boolean registerIdleListener(Object obj) {
		return idleTimer.addListener(obj);
	}

	private void setEditorForegroundColor() {
		// Set text editor forground colour
		RGB rgb = PreferenceConverter.getColor(PerlEditorPlugin.getDefault()
				.getPreferenceStore(), PreferenceConstants.EDITOR_STRING_COLOR);
		getSourceViewer().getTextWidget().setForeground(
				PerlColorProvider.getColor(rgb));
	}

	private final int findNextOccurance(String text, char findNextChar,
			int StartPosition) {
		char nextStringPair = ' ';
		int StackCounter = 0;
		int findFirst;
		int findPair;
		int maxLen = text.length();
		boolean searchForward = true;

		switch (findNextChar) {
		case '[':
			nextStringPair = ']';
			break;
		case '{':
			nextStringPair = '}';
			break;
		case '(':
			nextStringPair = ')';
			break;
		case '<':
			nextStringPair = '>';
			break;
		case ']':
			nextStringPair = '[';
			searchForward = false;
			StartPosition -= 2;
			break;
		case '}':
			nextStringPair = '{';
			searchForward = false;
			StartPosition -= 2;
			break;
		case ')':
			nextStringPair = '(';
			searchForward = false;
			StartPosition -= 2;
			break;
		case '>':
			nextStringPair = '<';
			searchForward = false;
			StartPosition -= 2;
			break;
		}

		if (StartPosition < 0 || maxLen < StartPosition) {
			return -1;
		}

		while (StackCounter >= 0) {
			if (searchForward) {
				findFirst = text.indexOf(findNextChar, StartPosition);
			} else {
				findFirst = text.lastIndexOf(findNextChar, StartPosition);
			}
			if (findFirst == -1) {
				if (searchForward) {
					findFirst = maxLen;
				} else {
					findFirst = 0;
				}
			}

			if (searchForward) {
				findPair = text.indexOf(nextStringPair, StartPosition);
			} else {
				findPair = text.lastIndexOf(nextStringPair, StartPosition);
			}
			if (findPair == -1) {
				if (searchForward) {
					findPair = maxLen;
				} else {
					findPair = 0;
				}
			}
			if (findPair < findFirst) {
				if (searchForward) {
					StackCounter -= 1;
					StartPosition = findPair + 1;
				} else {
					StackCounter += 1;
					StartPosition = findFirst - 1;
				}
			} else if (findFirst < findPair) {
				if (searchForward) {
					StackCounter += 1;
					StartPosition = findFirst + 1;
				} else {
					StackCounter -= 1;
					StartPosition = findPair - 1;
				}
			} else {
				if (findPair == 0
						&& (text.lastIndexOf(nextStringPair, StartPosition) == 0)) {
					//The very first character is the Bracket-matcher
					StackCounter -= 1;
					StartPosition = -1;
				} else {
					StackCounter = -2; //nothing found
				}
			}

		}
		if (StackCounter == -1) {
			if (searchForward) {
				if (StartPosition == 0) {
					return 0;
				} else {
					return StartPosition - 1;
				}
			} else {
				if (StartPosition == text.length()) {
					return StartPosition;
				} else {
					return StartPosition + 1;
				}
			}
		} else {
			return -1;
		}
	}
}