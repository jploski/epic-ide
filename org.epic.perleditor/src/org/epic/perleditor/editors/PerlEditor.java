package org.epic.perleditor.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.ViewerActionBuilder;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PerlColorProvider;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.views.PerlOutlinePage;
import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.model.Module;
import org.epic.perleditor.views.model.Subroutine;

import cbg.editor.ColoringSourceViewerConfiguration;
import org.epic.core.util.FileUtilities;

/**
 * Perl specific text editor.
 */

public class PerlEditor extends TextEditor implements
		ISelectionChangedListener, IPropertyChangeListener
		{

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
	private int lastOutlineHashCode = 0;

	private int lastTextLength = 0;

	private int lastCursorPos = 0;

	private String lastCursorChar = "  ";

	private int markDocPos = -1;
  private int doubleQuoteHash= 0;
  private int singleQuoteHash= 0;
  private int commentHash=0;

	private StyleRange myLastStyleRange = new StyleRange();

	private StyleRange newStyleRange = new StyleRange();

	private final org.eclipse.swt.graphics.Color tempColorBack = new org.eclipse.swt.graphics.Color(
			null, 87, 207, 215);

	private final org.eclipse.swt.graphics.Color tempColorFore = new org.eclipse.swt.graphics.Color(
			null, 255, 255, 0);

	private final String matchBrakets = "{([<>])}";
	
	private ITextViewerExtension5 viewerText5;
	
	private ISourceViewer fSourceViewer;
	
	private IDocument document;

	private IdleTimer idleTimer;

	private final static String PERL_MODE = "perl";

	private ProjectionSupport projectionSupport;

	int iZ=0;
	/**
	 * Default constructor();
	 */

	public PerlEditor() {
		super();

		setDocumentProvider(new PerlDocumentProvider());

		PerlEditorPlugin.getDefault().getPreferenceStore()
				.addPropertyChangeListener(this);

		this.setPreferenceStore(PerlEditorPlugin.getDefault()
				.getPreferenceStore());
		setKeyBindingScopes(new String[] { "org.epic.perleditor.perlEditorScope" });
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

		IDocumentProvider provider = getDocumentProvider();
		document = provider.getDocument(getEditorInput());
		fSourceViewer = getSourceViewer();
		fSourceViewer.setDocument(document);

		if (fValidationThread == null && isPerlMode()) {
			fValidationThread = new PerlSyntaxValidationThread(this,
					getSourceViewer());
			// Set thread priority to minimal
			fValidationThread.setPriority(Thread.MIN_PRIORITY);
			//Thread defaults
			fValidationThread.start();
		}

		if (fValidationThread != null) {
			// Always check syntax when editor is opened
			fValidationThread.setText(document.get());
		}

		calculateIgnoreTypeHash();  // to get the current HashCodes of Strings we should ignore

		//set up the ToDoMarkerThread
		if ((fTodoMarkerThread == null) && isPerlMode()) {
			fTodoMarkerThread = new PerlToDoMarkerThread(this,
			    fSourceViewer);
			fTodoMarkerThread.start();
		}

		// set up the FoldingThread
		if ((fFoldingThread == null) && isPerlMode()) {
			fFoldingThread = new PerlFoldingThread(this, fSourceViewer);
			fFoldingThread.start();
		}

		setEditorForegroundColor();

		// Setup idle timer
		idleTimer = new IdleTimer(fSourceViewer, Display.getCurrent());
		idleTimer.start();

		// Register the validation thread if automatic checking is enabled		if(PerlEditorPlugin.getDefault().getSyntaxValidationPreference()) {		  this.registerIdleListener(fValidationThread);
		}				this.registerIdleListener(fTodoMarkerThread);
		this.registerIdleListener(fFoldingThread);
    lastTextLength = fSourceViewer.getTextWidget().getText().length();
    lastHashCode = fSourceViewer.getTextWidget().getText().hashCode();

		newStyleRange.background = tempColorBack;
		newStyleRange.foreground = tempColorFore;
		newStyleRange.length = 1;
		newStyleRange.start = 0;
		
	//	if (getSourceViewer() instanceof ITextViewerExtension5) {
		  //should be, otherwise a lot of stuff would not work (Bracket Matching)
		  viewerText5 = (ITextViewerExtension5) getSourceViewer();
	//	}
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

			// resource.deleteMarkers(IMarker.PROBLEM, true, 1);

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

		if (page != null) {
			// Update only if input has changed 
		  //check here before we retrieve the Outline-List
			int hashCode = getSourceViewer().getDocument().get().hashCode();
			if (hashCode == lastHashCode) {
				return;
			}
			lastOutlineHashCode = hashCode;
			page.update(page.getSubList(), page.getModList());
		}

		if (fValidationThread != null) {
			fValidationThread.setText(getSourceViewer().getDocument().get());
		}

	}

	/**
	 * The PerlEditor implementation of this AbstractTextEditor method performs
	 * any extra save behavior required by the Perl editor.
	 */

	public void doSave(IProgressMonitor monitor) {

		super.doSave(monitor);

		if (page != null) {
			// Update only if input has changed 
		  //check here before we retrieve the Outline-List
			int hashCode = getSourceViewer().getDocument().get().hashCode();
			if (hashCode == lastHashCode) {
				return;
			}
			lastOutlineHashCode = hashCode;
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
			// Update only if input has changed 
		  //check here before we retrieve the Outline-List
			int hashCode = getSourceViewer().getDocument().get().hashCode();
			if (hashCode == lastHashCode) {
				return;
			}
			lastOutlineHashCode = hashCode;
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
				page = new PerlOutlinePage(this, getSourceViewer());

				this.registerIdleListener(page);

				page.addSelectionChangedListener(this);
				return page;
			}

		}

		return super.getAdapter(adapter);
	}

	public void updateOutline() {
		if (page != null) {
			// Update only if input has changed 
		  //check here before we retrieve the Outline-List
			int hashCode = getSourceViewer().getDocument().get().hashCode();
			if (hashCode == lastHashCode) {
				return;
			}
			lastOutlineHashCode = hashCode;
			page.update(page.getSubList(), page.getModList());
		}

	}

	/*
	 * Method declared on AbstractTextEditor
	 */
	protected void initializeEditor() {
		setSourceViewerConfiguration(new PerlSourceViewerConfiguration(
				PerlEditorPlugin.getDefault().getPreferenceStore(), this));
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
			fValidationThread.setText(getSourceViewer().getDocument().get(),
					forceUpdate);
		}

	}
	
	
	/**
	 * 
	 * @param documentPosition
	 * @return true   => the current text has no assigned ColourPattern, i.e. normal Text
	 *         false  => the current text has an assigned ColourPattern (e.g. KEYWORD, LITERAL)
	 */
	public final boolean isNormalText(int documentPosition) {
    try {
      if (getSourceViewer().getDocument().getPartition(documentPosition).getType().
          toString().indexOf("__dftl_partition_content_type") < 0) {
        return false;
      }
    } catch (BadLocationException e) {
      // should not happen, since it is for FoldingThread and Outline only
    }
	  return true;
	  	}
	
	public void refreshTaskView() {
		if (fTodoMarkerThread != null) {
			fTodoMarkerThread.setText(getSourceViewer().getDocument().get());
		}
	}
	
	
	/**
	 * To provide an access to a changed position 
	 * Colouring of the Brackets will be adjusted.
	 */
	public final void newCurosorPos() {
	  handleCursorPositionChanged();
	}
	
	public final void foldingUpdate() {
	  fFoldingThread.updateFoldingAnnotations();
	}
	
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();

		StyledText myText = getSourceViewer().getTextWidget();
		IDocument myDocument=getSourceViewer().getDocument();
		
		int cursorPosition = myText.getCaretOffset();
		int currentTextLength = myText.getText().length();

		/*
     * The main complexity of the Bracket matching is the cursor momevent. The
     * most trickiest situation is the Alt+ArrowUp/ArrowDown which swaps the
     * texts. This swapping is sent in up to 3 different events, 1 for the Alt,
     * one for the text change and finally one for the cursor position change.
     * And only the final one has to be considered! and this only because the
     * Selection-Counter is set to 0! (Eclipse 3.0) blah, blah, blah...
     * HOPEfully I (LeO) handled all possible situations about the bracket
     * matching!
     * 
		 */

    //if we have marked something and CursorPosition changed => unmark it right
    // now
    if ((markDocPos >= 0) && (lastCursorPos != cursorPosition || 
                                myText.getText().hashCode() != lastHashCode)) {
      resetStyle(myDocument, myText, currentTextLength);
    }
		//we only make something (marking, adding, deleting), if NOTHING is
		// selected
		if (myText.getSelectionCount() == 0) {
			//Bracket matching makes only sense for Cursor > 0[there is nothing
			// before Positon 0]
			if (cursorPosition > 0) {
				char sourceChar = '\u0000';
        sourceChar = myText.getTextRange(cursorPosition - 1, 1).charAt(0);
        calculateIgnoreTypeHash();  // to get the current HashCodes of Strings we should ignore
        if (myText.getText().hashCode() != lastHashCode) {
          handleTextChange(myDocument, myText, cursorPosition, currentTextLength, sourceChar);
        }

        //either the cursorPosition has been changed (last Event of Alt+ArrowUp/Down)
        //or the Del-Key was pressed  => Text-Length has been changed!
        if (lastCursorPos != cursorPosition || 
            (lastTextLength - currentTextLength) == 1 ) {
          //we have really changed the postion => let's recacluate the whole
          // stuff
					int nextClip;
					//check if we should mark something like the brackets
					if (matchBrakets.indexOf(sourceChar) >= 0) { 
            nextClip = findNextOccurance(myDocument, sourceChar, cursorPosition);
						if (nextClip >= 0) {
							setStyleChar(nextClip, myText);
						}
					}
				}
      }
    }
    //compute the new values
    lastHashCode = myText.getText().hashCode();
    lastTextLength = myText.getText().length();
    lastCursorPos =myText.getCaretOffset();
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
          lastCursorChar = myText.getTextRange(lastCursorPos - 1, 1) + " ";
        }
      }
    }
  }

  /*
   * @see IPropertyChangeListener.propertyChange()
   */

  /**
   * Check if we should make special bracket resp. quote handling
   *  
   * @param myDoc
   * @param myText
   * @param cursorPosition
   * @param currentTextLength
   * @param sourceChar
   */
  private final void handleTextChange(final IDocument myDoc,
                                      StyledText myText, 
                                      int cursorPosition, 
                                      int currentTextLength, 
                                      char sourceChar) {
    boolean isTextChanged = false;
    if ((currentTextLength - lastTextLength) == -1) {
      //something was deleted
      //we could delete the pair, if we could get information, what was
      // deleted!
      char delChar = ' ';
      if (cursorPosition == lastCursorPos) {
        //delete on the right sight (via DEL-Key)
        delChar = lastCursorChar.charAt(1);
      } else {
        //delete on the left sight (via Backspace-Key)
        delChar = lastCursorChar.charAt(0);
      }
      if (delChar == '[') {
        if (myText.getTextRange(cursorPosition, 1).charAt(0) == ']'          && PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET1)) {          myText.replaceTextRange(cursorPosition, 1, "");
          isTextChanged = true;
        }
      }
      if (delChar == '{') {
        if (myText.getTextRange(cursorPosition, 1).charAt(0) == '}'          && PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET2)) {          myText.replaceTextRange(cursorPosition, 1, "");
          isTextChanged = true;
        }
      }
      if (delChar == '(') {
        if (myText.getTextRange(cursorPosition, 1).charAt(0) == ')'          && PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET3)) {          myText.replaceTextRange(cursorPosition, 1, "");
          isTextChanged = true;
        }
      }
      if (delChar == '<') {
        if (myText.getTextRange(cursorPosition, 1).charAt(0) == '>'          && PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET4)) {          myText.replaceTextRange(cursorPosition, 1, "");
          isTextChanged = true;
        }
      }
    } else if ((currentTextLength - lastTextLength) == 1)  {
      //something was added
      String addChar = "";
      switch (sourceChar) {
      case '[':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET1)) {        addChar = "]";
        }        break;
      case '{':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET2)) {        addChar = "}";
        }        break;
      case '(':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET3)) {        addChar = ")";
        }        break;
      case '<':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET4)) {        addChar = ">";
        }        break;
      case ']':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET1)) {        check2Insert(myText, cursorPosition - 1, ']');
        }        break;
      case '}':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET2)) {        check2Insert(myText, cursorPosition - 1, '}');
        }        break;
      case ')':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET3)) {        check2Insert(myText, cursorPosition - 1, ')');
        }        break;
      case '>':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET4)) {        check2Insert(myText, cursorPosition - 1, '>');
        }        break;
      case '\"':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE1)) {        check2Insert(myText, cursorPosition - 1, '\"');
        }        break;
      case '\'':
        if (PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE2)) {        check2Insert(myText, cursorPosition - 1, '\'');
        }        break;
      }
      if (addChar.length() == 1) {
        myText.insert(addChar);
        isTextChanged = true;
      }
    }
    //the Bracket matching insert at a postion when another bracket
    // was marked!
    if (isTextChanged && (markDocPos >= 0)) {
      resetStyle(myDoc, myText, currentTextLength); //reset the marker
    }
  }

	/**
	 * Sets the Text back to original style! (Only in case we found a changed
	 * Style)
	 * 
	 * @param myText
	 * @param currentTextLength
	 */
  private final void resetStyle(final IDocument myDoc, StyledText myText, int currentTextLength) {
		int posChange = viewerText5.modelOffset2WidgetOffset(markDocPos);
		if (posChange < 0) {
		  //not displayable Position
		  markDocPos = -1;
		  return;
		}
		try {
		  if (markDocPos >= myDoc.getLength()) {
		    //the last changed position is out of reach, i.e. something was
		    // deleted
		    posChange += currentTextLength - lastTextLength;
		  } else if (!newStyleRange.equals(myText.getStyleRangeAtOffset(posChange))) {
		    posChange += currentTextLength - lastTextLength;
		  }
		  if (posChange >= 0 && posChange <= currentTextLength) {
		    if (newStyleRange.equals(myText.getStyleRangeAtOffset(posChange))) {
		      myLastStyleRange.start = posChange;
		      myText.setStyleRange(myLastStyleRange);
		    }
		    markDocPos = -1;
		  }
		  
		} catch (Exception e) {
		  // in the rare case we (=LeO) have done something wrong
		  markDocPos = -1;
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
  private final void check2Insert(StyledText myText, int cursorPosition,
			char checkChar) {
		String checkText = myText.getText();
    
    if (cursorPosition + 1 < checkText.length()
        && checkText.charAt(cursorPosition + 1) == checkChar) {
      myText.replaceTextRange(cursorPosition, 1, "");
      myText.setCaretOffset(cursorPosition + 1);
      return;
    }
    
    if (checkChar == '\"' || checkChar == '\'') {
      if (cursorPosition == 0 || cursorPosition == checkText.length()) {
        myText.insert(String.valueOf(checkChar));
      } else if (checkText.charAt(cursorPosition - 1) == '\\') {
        return;
      } else {
        //only insert is done for the quotes, the rest is only for checking
        // purpose
        final IDocument myDocument = getSourceViewer().getDocument();
        try {
          int checkPos = myDocument.getPartition(cursorPosition).getType().hashCode();
          if ((checkChar == '\"' && checkPos == singleQuoteHash)
              || (checkChar == '\'' && checkPos == doubleQuoteHash)) {
            return;
          } else {
	          int pos = cursorPosition - 1;
		int muCounter = 0;
		while (pos >= 0 && checkText.charAt(pos) == checkChar) {
			muCounter += 1;
			pos -= 1;
		}
	          if (muCounter % 2 == 0) {
				myText.insert(String.valueOf(checkChar));
			}
	        }
        } catch (BadLocationException e) {
          // should not happen
        }
			}
		}
	}

	/**
	 * Sets the style for a given postion - only useful, when at least one char
	 * input exists <p>
	 * Maps the current position according to the TextWidget position 
	 * @param stylePosition = Postion of the Document
	 * @param myText = TextWidget
	 * @author LeO
	 * @since Sep. 2004
	 */
  private final void setStyleChar(int stylePosition, StyledText myText) {
    int myStylePosition= viewerText5.modelOffset2WidgetOffset(stylePosition);
    if (myStylePosition >= 0) {
      //we only handle viewable Positions
      if (myText.getStyleRangeAtOffset(myStylePosition) != null) {
        myLastStyleRange = myText.getStyleRangeAtOffset(myStylePosition);
        newStyleRange.start = myStylePosition;
        myText.setStyleRange(newStyleRange);
        markDocPos = stylePosition;
        if (myLastStyleRange.foreground == null) {
          // workaround for an Eclipse - bug with outcome in the resetStyle
          // if no foreground or background => the Style would be null instead of existing!
          myLastStyleRange.foreground = myText.getForeground(); 
        }
      } else {
        // Unmark for Debug-Information if the Bracket-Matching does not work
        //        Shell shell;
        //        shell = PerlEditorPlugin.getWorkbenchWindow().getShell();
        //        MessageDialog.openInformation(shell, "Null Error", "Catching Error!!!");
      }
    }
  }
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals("PERL_EXECUTABLE")) {
			return;
		}

		// Check if automatic syntax validation has to be enabled or disabled		if(event.getProperty().equals(PerlEditorPlugin.SYNTAX_VALIDATION_PREFERENCE)) {			if(PerlEditorPlugin.getDefault().getSyntaxValidationPreference()) {				if(!idleTimer.isRegistered(fValidationThread)) {					this.registerIdleListener(fValidationThread);				}			}			else {				if(idleTimer.isRegistered(fValidationThread)) {					idleTimer.removeListener(fValidationThread);				}			}		}						PerlSourceViewerConfiguration viewerConfiguration = (PerlSourceViewerConfiguration) this				.getSourceViewerConfiguration();		if (viewerConfiguration != null) {		viewerConfiguration.adaptToPreferenceChange(event);
		  
		  setEditorForegroundColor();
		}
	}

	public void createPartControl(Composite parent) {
	  //Workaround for Eclipse Bug 75440 (to fix it somehow) [LeO]
	  if (!Workbench.getInstance().isClosing()) {
		  super.createPartControl(parent);
			ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
			
			projectionSupport = new ProjectionSupport(viewer,
					getAnnotationAccess(), getSharedColors());
			projectionSupport
					.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
			projectionSupport
					.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
	
			projectionSupport.install();
	
			viewer.doOperation(ProjectionViewer.TOGGLE);
	  }
	}
	
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return PerlEditorPlugin.getDefault().getEditorTools()
				.affectsTextPresentation(event);
	}

	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		if (getSourceViewer() == null
				|| getSourceViewer().getTextWidget() == null)
			return;

		super.handlePreferenceStoreChanged(event);

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

  private final void setEditorForegroundColor() {
		// Set text editor forground colour
		RGB rgb = PreferenceConverter.getColor(PerlEditorPlugin.getDefault()
				.getPreferenceStore(), PreferenceConstants.EDITOR_STRING_COLOR);
		getSourceViewer().getTextWidget().setForeground(
				PerlColorProvider.getColor(rgb));
  }

  /**
   * to access the method via Command-Stroke
   * 
   * @param StartPosition from the widget
   * @return the position for the widget
   */
  public int findNextOccurance(){
		StyledText myText = getSourceViewer().getTextWidget();
		IDocument myDocument=getSourceViewer().getDocument();
		
		int cursorPosition = myText.getCaretOffset();
		char sourceChar = myText.getTextRange(cursorPosition - 1, 1).charAt(0);
    return viewerText5.modelOffset2WidgetOffset(findNextOccurance(myDocument,
                                           sourceChar, 
                                           cursorPosition
                                           ));
  }
  /**
   * Finds the next matching Bracket <p>
   * If the current Bracket is under quotes/comments => consider the next
   * matching bracket also under (same) quotes/comment. If the next found
   * Bracket is not under quotes/comment, this Bracket is ignored.
   * <p>
   * If the current Bracket is a 'normal' Bracket => search for the next
   * 'normal' Bracket and ignore all under quotes and comments
   *  
   * @author LeO
   * @param findNextChar  = The first Bracket to search for
   * @param StartPosition = The startpositon for the search in the getSourceViewer().getDocument()
   * @return absolute Positon in the Document
   * @since Sep. 2004
   */
  public int findNextOccurance(final IDocument myDocument, char findNextChar, int StartPosition) {
		char nextStringPair = ' ';
		int StackCounter = 0;
		int findFirst;
		int findPair;
		boolean searchForward = true;
    StartPosition = viewerText5.widgetOffset2ModelOffset(StartPosition);
    String text = myDocument.get();
    int maxLen = text.length();
    
    boolean isNextCharQuote = false;
    boolean isNextCharComment = false;
    boolean isNextCharLiteral2 = false;    int checkPos = 0;
    String textType="";        try {
      checkPos = myDocument.getPartition(StartPosition - 1).getType().hashCode();
      textType = myDocument.getPartition(StartPosition - 1).getType();    } catch (BadLocationException e) {
      //should not happen!
    }
    if (checkPos ==  doubleQuoteHash || checkPos == singleQuoteHash) {
      isNextCharQuote = true;
    } else if (checkPos == commentHash) {
      isNextCharComment = true;
    } else if (textType.indexOf("LITERAL2") >= 0) {      isNextCharLiteral2 = true;    }
    

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

		int calcResult=0;
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
					StartPosition = findPair + 1;
					calcResult = findSingleCharNextOccurance(myDocument, findPair, isNextCharQuote, isNextCharComment, isNextCharLiteral2);					if (calcResult >= 0) {
					  StackCounter -= calcResult;
					} else {
					  StackCounter = -3;
					}
        } else {
					StartPosition = findFirst - 1;
					calcResult = findSingleCharNextOccurance(myDocument, findFirst, isNextCharQuote, isNextCharComment, isNextCharLiteral2);					if (calcResult >= 0) {
					  StackCounter += calcResult;
					} else {
					  StackCounter = -4;
					}
				}
			} else if (findFirst < findPair) {
				if (searchForward) {
					StartPosition = findFirst + 1;
					calcResult = findSingleCharNextOccurance(myDocument, findFirst, isNextCharQuote, isNextCharComment, isNextCharLiteral2);					if (calcResult >= 0) {
					  StackCounter += calcResult;
					} else {
					  StackCounter = -5;
					}
        } else {
					StartPosition = findPair - 1;
					calcResult = findSingleCharNextOccurance(myDocument, findPair, isNextCharQuote, isNextCharComment, isNextCharLiteral2);					if (calcResult >= 0) {
					  StackCounter -= calcResult;
					} else {
					  StackCounter = -6;
					}
				}
			} else {
				if (findPair == 0
						&& (text.lastIndexOf(nextStringPair, StartPosition) == 0)) {
					//The very first character is the Bracket-matcher
					StartPosition = -1;
					calcResult = findSingleCharNextOccurance(myDocument, 0, isNextCharQuote, isNextCharComment, isNextCharLiteral2);					if (calcResult >= 0) {
					  StackCounter -= calcResult;
					} else {
					  StackCounter = -7;
					}
				} else {
					StackCounter = -2; //nothing found
				}
			}

		}
		
		int returnValue=0;
		if (StackCounter == -1) {
			if (searchForward) {
				if (StartPosition == 0) {
					returnValue= 0;
				} else {
					returnValue= StartPosition - 1;
				}
			} else {
				if (StartPosition == text.length()) {
					returnValue = StartPosition;
				} else {
					returnValue = StartPosition + 1;
				}
			}
		} else {
			return -1;
		}
	  return returnValue;
	}
  
  /*
   * To make the inline coding little bit more easier to understand
   * We check if the found position is a valid character or it should be ignored
   */
  private int findSingleCharNextOccurance(final IDocument myDocument, 
                                           final int findPos,
                                           final boolean isNextCharQuote,
                                           final boolean isNextCharComment,                                           final boolean isNextLiteral2                                           ) {
    int checkPos = 0;
    String checkType="";    try {
      checkPos = myDocument.getPartition(findPos).getType().hashCode();
      checkType = myDocument.getPartition(findPos).getType();    } catch (BadLocationException e1) {
      //should not happen!
    }
    if (isNextCharQuote) {
      if (checkPos == doubleQuoteHash || 
          checkPos == singleQuoteHash) {
        return 1; //The current pos is also under quotes
      } else {
        return -1; //End the search-procedure, cause we found a non-quote char
      }
    } else if (isNextCharComment) {
      if (checkPos == commentHash) {
        return 1; //The current pos is also a comment
      } else {
        return -1; //End the search-procedure, cause we found a non-comment char
      }
    } else if (isNextLiteral2) {      if (checkType.indexOf("LITERAL2") >= 0) {        return 1; //The current pos is also a comment      } else {        return -1; //End the search-procedure, cause we found a non-comment char    }
    } else {      if (!(checkPos == doubleQuoteHash) && 
          !(checkPos == singleQuoteHash) &&
          !(checkPos == commentHash) &&           !(checkType.indexOf("LITERAL2") >= 0)          ){
        return 1;
      } else {
        return 0;  //ignore the current pos., continue search
      }
    }
  }
  /**
   * calculate the TypeString for the Quotes (single and double) and Comments
   * 
   * @return
   */
  private void calculateIgnoreTypeHash() {
    
    if (doubleQuoteHash == 0 || singleQuoteHash == 0 || commentHash == 0) {
      final IDocument myDocument = getSourceViewer().getDocument();
      
      if (doubleQuoteHash == 0) { 
        doubleQuoteHash = calculateIgnoreTypeHash(myDocument, "\"", "LITERAL1", 0);
      }
      
      if (singleQuoteHash == 0) { 
        singleQuoteHash = calculateIgnoreTypeHash(myDocument,"\'", "LITERAL1", doubleQuoteHash);
      }

      if (singleQuoteHash == doubleQuoteHash && singleQuoteHash != 0) {
        doubleQuoteHash = calculateIgnoreTypeHash(myDocument, "\"", "LITERAL1", singleQuoteHash);
      }
      
      if (commentHash == 0) {
        commentHash = calculateIgnoreTypeHash(myDocument, "#", "COMMENT", 0);
      }
      return;
    }
  }

  /**
   * Initially used to calculate the Hash for the Quotes, but then extended, e.g. for Comments
   * 
   * @param myDocument
   * @param quoteChar
   * @param ignoreHash
   */
  private int calculateIgnoreTypeHash(final IDocument myDocument, 
                                 String quoteChar,
                                 String charType,
                                 int ignoreHash) {
    boolean quoteNotFound=true;
    String text=myDocument.get();
    int quotePos=text.indexOf(quoteChar);
    while (quoteNotFound && quotePos >= 0) {
      if (quotePos > 0)  {
        if (text.charAt(quotePos - 1) != '\\') {
          //check if it is a Quote (could also be Remark)
          try {
            if (myDocument.getPartition(quotePos+1).getType().toString().indexOf(charType) >= 0 &&
                ! (myDocument.getPartition(quotePos+1).getType().hashCode() == ignoreHash)) {
              quoteNotFound = false;
            }
          } catch (BadLocationException e) {
            //should not happen!
          }
        }
      } else if (quotePos == 0) {
        quoteNotFound = false;
      }
      if (quoteNotFound) {
        quotePos = text.indexOf(quoteChar,quotePos+1);
      }
    }
    if (quotePos >= 0) {
      try {
        return myDocument.getPartition(quotePos+1).getType().hashCode();
      } catch (BadLocationException e) {
        //should not happen!
      }
    }
    return 0;
  }
  
/**
 * Returns the Idle Timer associated with the editor
 * 
 * @return	Idle Timer
 */
public IdleTimer getIdleTimer() {
  		return idleTimer;
  }
  	
}