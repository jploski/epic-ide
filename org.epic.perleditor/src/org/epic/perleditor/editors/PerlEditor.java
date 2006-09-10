package org.epic.perleditor.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.ViewerActionBuilder;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.epic.core.model.*;
import org.epic.core.util.FileUtilities;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.actions.*;
import org.epic.perleditor.preferences.MarkOccurrencesPreferences;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.templates.perl.ModuleCompletionHelper;
import org.epic.perleditor.views.PerlOutlinePage;

/**
 * Perl specific text editor.
 */
public class PerlEditor extends TextEditor implements IPropertyChangeListener
{
    /**
     * Editor id, as declared in the plug-in manifest.
     */
    public static final String PERL_EDITOR_ID =
        "org.epic.perleditor.editors.PerlEditor";

    private PerlPairMatcher bracketMatcher;
    private PerlBracketInserter bracketInserter;
    private FoldReconciler foldReconciler;
    private TasksReconciler tasksReconciler;
    private PerlOutlinePage outlinePage;
    private PerlSyntaxValidationThread validationThread;
    private ISourceViewer sourceViewer;
    private IdleTimer idleTimer;
    private ProjectionSupport projectionSupport;
    private SourceFile source;

    private OccurrencesUpdater occurrencesUpdater;

    /**
     * Flag used to avoid relocating caret in response to an outline selection
     * change triggered by a previous caret move.
     */
    private boolean syncToOutline;

    /**
     * Flag used to avoid updating outline selection in response to a caret
     * move caused by a previous outline selection update.
     */
    private boolean syncFromOutline;

    public PerlEditor()
    {
        setDocumentProvider(PerlEditorPlugin.getDefault().getDocumentProvider());
        PerlEditorPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
        setKeyBindingScopes(new String[] { "org.epic.perleditor.perlEditorScope" });
    }

    public void createPartControl(Composite parent)
    {
        // Workaround for Eclipse Bug 75440 (to fix it somehow) [LeO]
        if (Workbench.getInstance().isClosing()) return;

        super.createPartControl(parent);

        installProjectionSupport();
        installBracketInserter();
        installCaretMoveListener();
        installModuleCompletionHelper();
        installIdleTimer();
        installSyntaxValidationThread();
        installFoldReconciler();
        installTasksReconciler();
        installAnnotationListener();

        source = new SourceFile(
            PerlEditorPlugin.getDefault().getLog(),
            getViewer().getDocument());

        reconcile();
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * any extra disposal actions required by the Perl editor.
     */
    public void dispose()
    {
        PerlEditorPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
        
        uninstallAnnotationListener();

        try
        {
            if (sourceViewer instanceof ITextViewerExtension &&
                bracketInserter != null)
            {
                ((ITextViewerExtension) sourceViewer).removeVerifyKeyListener(
                    bracketInserter);
            }
            if (validationThread != null) validationThread.dispose();
            if (idleTimer != null) idleTimer.dispose();

            String[] actionIds = PerlEditorActionIds.getEditorActions();
            for (int i = 0; i < actionIds.length; i++)
            {
                IAction action = getAction(actionIds[i]);
                if (action instanceof PerlEditorAction)
                    ((PerlEditorAction) action).dispose();
            }

            super.dispose();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(); // TODO log it
        }
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * any extra revert behavior required by the Perl editor.
     */
    public void doRevertToSaved()
    {
        super.doRevertToSaved();
        revalidateSyntax();
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * any extra save behavior required by the Perl editor.
     */
    public void doSave(IProgressMonitor monitor)
    {
        super.doSave(monitor);
        revalidateSyntax();
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * any extra save as behavior required by the Perl editor.
     */
    public void doSaveAs()
    {
        super.doSaveAs();
        revalidateSyntax();
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * sets the input of the outline page after AbstractTextEditor has set
     * input.
     */
    public void doSetInput(IEditorInput input)
        throws CoreException
    {
        // Map external files into workspace (epic-links)
        if (input instanceof ILocationProvider)
        {
            ILocationProvider l = (ILocationProvider)
                input.getAdapter(ILocationProvider.class);

            input = FileUtilities.getFileEditorInput(l.getPath(l).makeAbsolute());            
            if (input == null) throw new CoreException(new Status(
                IStatus.ERROR,
                PerlEditorPlugin.getPluginId(),
                IStatus.OK,
                "Could not open requested file " + l.getPath(l).makeAbsolute() +
                ". Inspect the Error Log for additional information.",
                null));
        }

        super.doSetInput(input);

        if (getViewer() != null)
        {
            // The editor is being reused (e.g. when the user clicks on matches
            // found through a search). Make sure we synchronize with the new content.

            source = new SourceFile(
                PerlEditorPlugin.getDefault().getLog(),
                getViewer().getDocument());

            reconcile();
        }
    }

    /**
     * Provided that the given document contains a bracket-like
     * character at the given offset, returns the offset of
     * the matching (pair) character (if found). Otherwise, returns -1.
     *
     * @param exact
     *        if false, search for the match within currently displayed text only;
     *        if true, search in the entire document
     */
    public int findMatchingBracket(
        final IDocument document,
        final int offset,
        boolean exact)
    {
        if (exact) bracketMatcher.setViewer(null);
        try
        {
            final int[] ret = new int[1];

            IRegion matchRegion = bracketMatcher.match(document, offset);

            if (matchRegion == null) ret[0] = -1;
            else ret[0] =
                matchRegion.getOffset() == offset - 1
                ? matchRegion.getOffset() + matchRegion.getLength() - 1
                : matchRegion.getOffset();

            return ret[0];
        }
        finally
        {
            if (exact) bracketMatcher.setViewer(sourceViewer);
        }
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * gets the Perl content outline page if request is for a an outline page.
     */
    public Object getAdapter(Class adapter)
    {
        if (ProjectionAnnotationModel.class.equals(adapter))
        {
            if (this.projectionSupport != null)
            {
                Object result = this.projectionSupport.getAdapter(
                    sourceViewer, adapter);
                if (result != null)
                {
                    return result;
                }
            }
        }

        if (adapter.equals(IContentOutlinePage.class))
        {
            IEditorInput input = getEditorInput();

            if (input instanceof IFileEditorInput)
            {
                outlinePage = new PerlOutlinePage(source);
                outlinePage.addSelectionChangedListener(new OutlineSelectionListener());
                return outlinePage;
            }
        }

        return super.getAdapter(adapter);
    }

    /**
     * Returns the Idle Timer associated with the editor
     *
     * @return Idle Timer
     */
    public IdleTimer getIdleTimer()
    {
        return idleTimer;
    }

    /**
     * @return the SourceFile edited in this editor
     */
    public SourceFile getSourceFile()
    {
        return source;
    }

    /**
     * @return test interface to this editor's internals;
     *         do not use outside of white-box test cases!
     */
    public TestInterface getTestInterface()
    {
        return new TestInterface();
    }

    /**
     * @return the source viewer used by this editor
     */
    public ISourceViewer getViewer()
    {
        return super.getSourceViewer();
    }

    /**
     * Provided that the caret's current position is after a bracket-like
     * character, jumps to its matching character (if found). Otherwise,
     * this method has no effect.
     *
     * @see PerlPairMatcher
     */
    public void jumpToMatchingBracket()
    {
        int caretOffset = sourceViewer.getSelectedRange().x;
        int matchOffset =
            findMatchingBracket(sourceViewer.getDocument(), caretOffset, true);

        if (matchOffset == -1) return;

        sourceViewer.revealRange(matchOffset + 1, 1);
        sourceViewer.setSelectedRange(matchOffset + 1, 0);
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals("PERL_EXECUTABLE"))
        {
            return;
        }

        // Check if automatic syntax validation has to be enabled or disabled
        if (event.getProperty().equals(
            PerlEditorPlugin.SYNTAX_VALIDATION_PREFERENCE))
        {
            if (PerlEditorPlugin.getDefault().getSyntaxValidationPreference())
            {
                if (!idleTimer.isRegistered(validationThread))
                {
                    this.registerIdleListener(validationThread);
                }
            }
            else
            {
                if (idleTimer.isRegistered(validationThread))
                {
                    idleTimer.removeListener(validationThread);
                }
            }
        }
    }

    /**
     * Updates the editor's dependent views and state after a document change.
     * This method is only intended for use by {@link PerlReconcilingStrategy}.
     */
    public void reconcile()
    {
        // the problem is, we might be called after the ISourceViewer
        // has been disposed; this occurs BEFORE dispose() is invoked
        // on the PerlEditor, so there seems to be no good way to
        // synchronise properly
        if (sourceViewer == null) return;
        StyledText widget = sourceViewer.getTextWidget();
        if (widget == null || widget.isDisposed()) return;
        Display display = widget.getDisplay();
        if (display == null) return;
        final IDocument doc = sourceViewer.getDocument();
        if (doc == null) return;

        // We reconcile on the main (Display) thread in order to avoid
        // race conditions due to user's modifications; this also means
        // that the reconciling has to be FAST in order to keep the GUI
        // responsive.
        //
        // An alternate (and better) solution would be to use a better
        // implementation (wrapper?) of IDocument, which supports multithreaded
        // access.
        //
        display.syncExec(new Runnable() {
            public void run()
            {
                source.parse();

                if (outlinePage != null) outlinePage.updateContent(source);
                if (foldReconciler != null) foldReconciler.reconcile();
                if (tasksReconciler != null) tasksReconciler.reconcile();
            } });
    }

    public void refreshTaskView()
    {
        tasksReconciler.reconcile();
    }

    public void registerIdleListener(IdleTimerListener obj)
    {
        idleTimer.addListener(obj);
    }

    /**
     * Immediately revalidates syntax of the edited file.
     * This method has no effect if the syntax validation thread is not installed.
     */
    public void revalidateSyntax()
    {
        if (validationThread != null) validationThread.revalidate();
    }

    protected boolean affectsTextPresentation(PropertyChangeEvent event)
    {
        // TODO examine event.getProperty() to tell whether presentation
        // is affected; see also reconfigureSourceViewer which might be
        // called too often on preference changes
        return true;
    }

    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support)
    {
        support.setCharacterPairMatcher(bracketMatcher);
        support.setMatchingCharacterPainterPreferenceKeys(
            PreferenceConstants.EDITOR_MATCHING_BRACKETS,
            PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR);

        super.configureSourceViewerDecorationSupport(support);
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method extend
     * the actions to add those specific to the receiver
     */
    protected void createActions()
    {
        super.createActions();

        wireAction(
            new ContentAssistAction(
                PerlEditorMessages.getResourceBundle(),
                "ContentAssistProposal.",
                this),
            PerlEditorCommandIds.CONTENT_ASSIST,
            PerlEditorActionIds.CONTENT_ASSIST);

        wireAction(new ToggleCommentAction(this), PerlEditorCommandIds.TOGGLE_COMMENT,
            PerlEditorActionIds.TOGGLE_COMMENT);
        wireAction(new CritiqueSourceAction(this), PerlEditorCommandIds.CRITIQUE_SOURCE,
            PerlEditorActionIds.CRITIQUE_SOURCE);
        wireAction(new FormatSourceAction(this), PerlEditorCommandIds.FORMAT_SOURCE,
            PerlEditorActionIds.FORMAT_SOURCE);
        wireAction(new Jump2BracketAction(this), PerlEditorCommandIds.MATCHING_BRACKET,
            PerlEditorActionIds.MATCHING_BRACKET);
        wireAction(new ExportHtmlSourceAction(this), PerlEditorCommandIds.HTML_EXPORT,
            PerlEditorActionIds.HTML_EXPORT);
        wireAction(new ValidateSourceAction(this), PerlEditorCommandIds.VALIDATE_SYNTAX,
            PerlEditorActionIds.VALIDATE_SYNTAX);
        wireAction(new OpenDeclarationAction(this), PerlEditorCommandIds.OPEN_SUB,
            PerlEditorActionIds.OPEN_SUB);
        wireAction(new PerlDocAction(this), PerlEditorCommandIds.PERL_DOC,
            PerlEditorActionIds.PERL_DOC);
        wireAction(new ExtractSubroutineAction(this), PerlEditorCommandIds.EXTRACT_SUBROUTINE,
            PerlEditorActionIds.EXTRACT_SUBROUTINE);
    }

    protected void createNavigationActions()
    {
        super.createNavigationActions();

        IAction action;
        StyledText textWidget = getSourceViewer().getTextWidget();

        action = new SmartLineStartAction(textWidget, false);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_START);
        setAction(ITextEditorActionDefinitionIds.LINE_START, action);

        action = new SmartLineStartAction(textWidget, true);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_LINE_START);
        setAction(ITextEditorActionDefinitionIds.SELECT_LINE_START, action);

        action = new NextWordAction(ST.WORD_NEXT, false);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_NEXT);
        setAction(ITextEditorActionDefinitionIds.WORD_NEXT, action);
        textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_RIGHT, SWT.NULL);

        action = new NextWordAction(ST.SELECT_WORD_NEXT, true);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT);
        setAction(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, action);
        textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_RIGHT, SWT.NULL);

        action = new PreviousWordAction(ST.WORD_PREVIOUS, false);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.WORD_PREVIOUS);
        setAction(ITextEditorActionDefinitionIds.WORD_PREVIOUS, action);
        textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_LEFT, SWT.NULL);

        action = new PreviousWordAction(ST.SELECT_WORD_PREVIOUS, true);
        action.setActionDefinitionId(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS);
        setAction(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, action);
        textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_LEFT, SWT.NULL);
    }

    /* Create SourceViewer so we can use the PerlSourceViewer class */
    protected final ISourceViewer createSourceViewer(
        Composite parent,
        IVerticalRuler ruler,
        int styles)
    {
        fAnnotationAccess = createAnnotationAccess();
        fOverviewRuler = createOverviewRuler(getSharedColors());
        sourceViewer = new PerlSourceViewer(
            parent,
            ruler,
            fOverviewRuler,
            isOverviewRulerVisible(),
            styles);

        // ensure source viewer decoration support has been created and
        // configured
        installBracketMatcher();
        getSourceViewerDecorationSupport(sourceViewer);

        return sourceViewer;
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
    protected void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);
        ViewerActionBuilder builder = new ViewerActionBuilder();
        builder.readViewerContributions("#PerlEditorContext",
            getSelectionProvider(), this);
        builder.contribute(menu, null, true);
    }

    protected void handlePreferenceStoreChanged(PropertyChangeEvent event)
    {
        if (sourceViewer == null || sourceViewer.getTextWidget() == null) return;

        try
        {
            if (event.getProperty().equals(MarkOccurrencesPreferences.MARK_OCCURRENCES))
            {
                boolean oldValue = event.getOldValue() != null
                    ? Boolean.valueOf(event.getOldValue().toString()).booleanValue()
                    : false;

                boolean newValue = event.getOldValue() != null
                    ? Boolean.valueOf(event.getNewValue().toString()).booleanValue()
                    : false;

                if (newValue != oldValue)
                {
                    if (newValue) installAnnotationListener();
                    else uninstallAnnotationListener();
                }
            }
            else
            {
                reconfigureBracketInserter();
            }
        }
        finally
        {
           super.handlePreferenceStoreChanged(event);
        }
    }

    protected void initializeEditor()
    {
        super.initializeEditor();

        // Make general workbench editor preferences (such as QuickDiff)
        // available through our preference store
        setPreferenceStore(new ChainedPreferenceStore(new IPreferenceStore[] {
            PerlEditorPlugin.getDefault().getPreferenceStore(),
            this.getPreferenceStore() }));

        setSourceViewerConfiguration(new PerlSourceViewerConfiguration(
            PerlEditorPlugin.getDefault().getPreferenceStore(), this));
    }

    private void caretMoved()
    {
        if (!getPreferenceStore().getBoolean(
            PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE)) return;
        if (syncFromOutline) { syncFromOutline = false; return; }
        if (outlinePage == null || source == null || source.getDocument() == null) return;

        try
        {
            syncToOutline = true;

            int caretOffset = sourceViewer.getTextWidget().getCaretOffset();
            int caretLine = source.getDocument().getLineOfOffset(caretOffset);

            outlinePage.updateSelection(caretLine);
        }
        catch (BadLocationException e)
        {
            e.printStackTrace(); // TODO log it
        }
        finally
        {
            syncToOutline = false;
        }
    }

    private void installBracketInserter()
    {
        bracketInserter =
            new PerlBracketInserter(PerlEditorPlugin.getDefault().getLog());

        reconfigureBracketInserter();

        if (sourceViewer instanceof ITextViewerExtension)
            ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(
                bracketInserter);

        bracketInserter.setViewer(sourceViewer);
    }

    private void installBracketMatcher()
    {
        bracketMatcher = new PerlPairMatcher(PerlEditorPlugin.getDefault().getLog());
        bracketMatcher.setViewer(sourceViewer);
    }

    private void installCaretMoveListener()
    {
        new CaretMoveListener().install(getSelectionProvider());
    }

    private void installAnnotationListener()
    {
        if (occurrencesUpdater == null)
        {
            occurrencesUpdater = new OccurrencesUpdater();
        }

        occurrencesUpdater.install(getSourceViewer());
    }

    private void uninstallAnnotationListener()
    {
        if (occurrencesUpdater == null) return;

        occurrencesUpdater.uninstall();
    }

    private void installFoldReconciler()
    {
        foldReconciler = new FoldReconciler(this);
    }

    private void installIdleTimer()
    {
        idleTimer = new IdleTimer(sourceViewer, Display.getCurrent());
        idleTimer.start();
    }

    private void installModuleCompletionHelper()
    {
        // load the module completion list in a low-priority background thread
    	Thread backgroundLoader = new Thread(new Runnable() {
			public void run() {
				try {
			        ModuleCompletionHelper completionHelper =
			            ModuleCompletionHelper.getInstance();						
					completionHelper.scanForModules(PerlEditor.this);
				}
				catch (CoreException e)
				{
					PerlEditorPlugin.getDefault().getLog().log(e.getStatus());
				}
			} },
            "EPIC:ModuleCompletionHelper");
    	backgroundLoader.setPriority(Thread.MIN_PRIORITY);
    	backgroundLoader.start();
    }

    private void installProjectionSupport()
    {
        ProjectionViewer viewer = (ProjectionViewer) sourceViewer;

        projectionSupport = new ProjectionSupport(
            viewer, getAnnotationAccess(), getSharedColors());
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
        projectionSupport.install();

        viewer.doOperation(ProjectionViewer.TOGGLE);
    }

    private void installSyntaxValidationThread()
    {
        // Always check syntax when editor is opened
        validationThread = new PerlSyntaxValidationThread();
        validationThread.setPriority(Thread.MIN_PRIORITY);
        validationThread.start();
        validationThread.setDocument(
            (IResource) ((IAdaptable) getEditorInput()).getAdapter(IResource.class),
            sourceViewer.getDocument());

        // Register the validation thread if automatic checking is enabled
        if (PerlEditorPlugin.getDefault().getSyntaxValidationPreference() &&
            idleTimer != null)
        {
            registerIdleListener(validationThread);
        }
    }

    private void installTasksReconciler()
    {
        tasksReconciler = new TasksReconciler(this);
    }

    private void reconfigureBracketInserter()
    {
        IPreferenceStore preferenceStore = getPreferenceStore();

        bracketInserter.setCloseBracketsEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET1));
        bracketInserter.setCloseBracesEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET2));
        bracketInserter.setCloseParensEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET3));
        bracketInserter.setCloseAngularBracketsEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET4));
        bracketInserter.setCloseDoubleQuotesEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE1));
        bracketInserter.setCloseSingleQuotesEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE2));
    }

    private void wireAction(IAction action, String commandId, String perlActionId)
    {
        action.setActionDefinitionId(commandId);
        setAction(perlActionId, action);
    }

    /**
     * Contains methods that provide access to internal workings of PerlEditor
     * intended to be available only to white-box test cases. Other clients
     * should not make use of this interface.
     */
    public class TestInterface
    {
        public void clear()
        {
            sourceViewer.getTextWidget().setText("");
        }

        public int getHighlightedBracketOffset()
        {
            int offset = sourceViewer.getTextWidget().getCaretOffset();

            PerlPairMatcher matcher =
                new PerlPairMatcher(PerlEditorPlugin.getDefault().getLog());

            matcher.setViewer(null);
            matcher.match(sourceViewer.getDocument(), offset);

            return
                offset - 1 == matcher.getStartPos()
                ? matcher.getEndPos()
                : matcher.getStartPos();
        }

        public String getText()
        {
            return sourceViewer.getTextWidget().getText();
        }

        public IVerticalRuler getVerticalRuler()
        {
            return ((PerlSourceViewer) sourceViewer)._getVerticalRuler();
        }

        public void setCaretOffset(final int offset)
        {
            Display display = sourceViewer.getTextWidget().getDisplay();
            sourceViewer.setSelectedRange(offset, 0);
            while (display.readAndDispatch());
        }

        public void setExactBracketMatching()
        {
            bracketMatcher.setViewer(null);
        }

        public void setTopIndex(int topIndex)
        {
            sourceViewer.setTopIndex(topIndex);
        }

        public void selectText(String text)
        {
            if (text.length() == 0)
            {
                getSelectionProvider().setSelection(TextSelection.emptySelection());
            }
            else
            {
                int i = sourceViewer.getDocument().get().indexOf(text);
                if (i == -1) throw new RuntimeException(
                    "text \"" + text + "\" not found in editor");
                getSelectionProvider().setSelection(new TextSelection(i, text.length()));
            }
        }
    }

    /**
     * Tracks caret movements in order to update selection in the outline page.
     * Implementation borrowed from org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.
     */
    private class CaretMoveListener implements ISelectionChangedListener
    {
        /**
         * Installs this selection changed listener with the given selection provider. If
         * the selection provider is a post selection provider, post selection changed
         * events are the preferred choice, otherwise normal selection changed events
         * are requested.
         *
         * @param selectionProvider
         */
        public void install(ISelectionProvider selectionProvider) {
            if (selectionProvider == null)
                return;

            if (selectionProvider instanceof IPostSelectionProvider)  {
                IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
                provider.addPostSelectionChangedListener(this);
            } else  {
                selectionProvider.addSelectionChangedListener(this);
            }
        }

        public void selectionChanged(SelectionChangedEvent event) {
            PerlEditor.this.caretMoved();
        }

        /**
         * Removes this selection changed listener from the given selection provider.
         *
         * @param selectionProvider the selection provider
         */
        public void uninstall(ISelectionProvider selectionProvider) {
            if (selectionProvider == null)
                return;

            if (selectionProvider instanceof IPostSelectionProvider)  {
                IPostSelectionProvider provider= (IPostSelectionProvider) selectionProvider;
                provider.removePostSelectionChangedListener(this);
            } else  {
                selectionProvider.removeSelectionChangedListener(this);
            }
        }
    }

    /**
     * Tracks selection in the outline page in order to highlight subroutine
     * declarations in source code.
     */
    private class OutlineSelectionListener implements ISelectionChangedListener
    {
        public void selectionChanged(SelectionChangedEvent event)
        {
            if (syncToOutline) return;
            if (event == null) return;
            if (!(event.getSelection() instanceof IStructuredSelection)) return;

            IStructuredSelection sel = (IStructuredSelection) event.getSelection();
            if (!(sel.getFirstElement() instanceof ISourceElement)) return;

            ISourceElement elem = (ISourceElement) sel.getFirstElement();
            syncFromOutline = true;
            selectAndReveal(elem.getOffset(), elem.getName().length());
        }
    }

    /**
     * This action implements smart home.
     *
     * Instead of going to the start of a line it does the following:
     *
     * - if smart home/end is enabled and the caret is after the line's first
     *   non-whitespace then the caret is moved directly before it; beginning of
     *   a comment ('#') counts as whitespace
     * - if the caret is before the line's first non-whitespace, the caret is
     *   moved to the beginning of the line
     * - if the caret is at the beginning of the line, see first case.
     */
    private class SmartLineStartAction extends LineStartAction
    {
        /**
         * @param textWidget the editor's styled text widget
         * @param doSelect a boolean flag which tells if the text up to the beginning
         *                 of the line should be selected
         */
        public SmartLineStartAction(StyledText textWidget, boolean doSelect)
        {
            super(textWidget, doSelect);
        }

        protected int getLineStartPosition(
            final IDocument document,
            final String line,
            final int length,
            final int offset)
        {
            int index = super.getLineStartPosition(document, line, length, offset);

            if (index < length - 1 && line.charAt(index) == '#')
            {
                index++;
                while (index < length && Character.isWhitespace(line.charAt(index)))
                    index++;
            }
            return index;
        }
    }

    /**
     * Base class for actions that navigate to or select text up to the next
     * word boundary.
     */
    protected abstract class WordNavigationAction extends TextNavigationAction
    {
        private boolean select;

        protected WordNavigationAction(int code, boolean select)
        {
            super(getSourceViewer().getTextWidget(), code);
            this.select = select;
        }

        public final void run()
        {
            final IPreferenceStore store = getPreferenceStore();
            if (!store.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION))
            {
                super.run();
                return;
            }

            ISourceViewer viewer = getSourceViewer();
            IDocument document = viewer.getDocument();
            int position = widgetOffset2ModelOffset(
                viewer, viewer.getTextWidget().getCaretOffset());

            if (position == -1) return;

            PerlPartitioner partitioner =
                (PerlPartitioner) document.getDocumentPartitioner();
            int docLength = document.getLength();

            run(viewer, document, position, partitioner, docLength);
        }

        protected abstract void run(
            ISourceViewer viewer,
            IDocument document,
            int position,
            PerlPartitioner partitioner,
            int docLength);

        protected final void setCaretPosition(final int position)
        {
            if (select)
            {
                final ISourceViewer viewer = getSourceViewer();
                final StyledText text = viewer.getTextWidget();

                if (text != null && !text.isDisposed())
                {
                    final Point selection = text.getSelection();
                    final int caret = text.getCaretOffset();
                    final int offset = modelOffset2WidgetOffset(viewer, position);

                    if (caret == selection.x)
                        text.setSelectionRange(selection.y, offset - selection.y);
                    else
                        text.setSelectionRange(selection.x, offset - selection.x);
                }
            }
            else
            {
                getSourceViewer().getTextWidget().setCaretOffset(
                    modelOffset2WidgetOffset(getSourceViewer(), position));
            }
        }
    }

    /**
     * Navigates or selects text up to the next word boundary.
     */
    private final class NextWordAction extends WordNavigationAction
    {
        /**
         * @param code
         *        Action code for the default operation.
         *        Must be an action code from {@link org.eclipse.swt.custom.ST}.
         */
        protected NextWordAction(int code, boolean select)
        {
            super(code, select);
        }

        protected void run(
            ISourceViewer viewer,
            IDocument document,
            int position,
            PerlPartitioner partitioner,
            int length)
        {
            try
            {
                int line = document.getLineOfOffset(position);
                if (position < length &&
                    (document.getChar(position) == '\n' ||
                     document.getChar(position) == '\r'))
                {
                    // Make "next word" action at the end of a line go to the begining of
                    // the next line
                    if (line < document.getNumberOfLines())
                        position = document.getLineOffset(line+1);
                    else return;
                }
                else
                {
                    ITypedRegion partition =
                        partitioner.getPartition(position, true);

                    int partitionEnd = partition.getOffset() + partition.getLength();

                    if (position == partitionEnd && position < length)
                    {
                        partition = partitioner.getPartition(position+1, true);
                        partitionEnd = partition.getOffset() + partition.getLength();
                    }

                    while (position < partitionEnd &&
                           !Character.isWhitespace(document.getChar(position))) position++;

                    while (position < length &&
                           Character.isWhitespace(document.getChar(position)) &&
                           document.getChar(position) != '\n' &&
                           document.getChar(position) != '\r') position++;
                }

                setCaretPosition(position);
                getTextWidget().showSelection();
                fireSelectionChanged();
            }
            catch (BadLocationException e) // should never occur
            {
                PerlEditorPlugin.getDefault().getLog().log(
                    new Status(
                        IStatus.ERROR,
                        PerlEditorPlugin.getPluginId(),
                        IStatus.OK,
                        "An unexpected exception occurred in NextWordAction",
                        e));

                super.run(); // fall back on default behavior
            }
        }
    }

    /**
     * Navigates or selects text up to the previous word boundary.
     */
    private final class PreviousWordAction extends WordNavigationAction
    {
        /**
         * @param code
         *        Action code for the default operation.
         *        Must be an action code from {@link org.eclipse.swt.custom.ST}.
         */
        protected PreviousWordAction(int code, boolean select)
        {
            super(code, select);
        }

        protected void run(
            ISourceViewer viewer,
            IDocument document,
            int position,
            PerlPartitioner partitioner,
            int length)
        {
            try
            {
                int line = document.getLineOfOffset(position);
                if (document.getLineOffset(line) == position)
                {
                    // Make "previous word" action at the beginning of a line go to the end of
                    // the previous line
                    if (line > 0)
                    {
                        int prevLineOffset = document.getLineOffset(line-1);
                        position = prevLineOffset + document.getLineLength(line-1);
                        while (
                            position > prevLineOffset &&
                            (document.getChar(position-1) == '\n' ||
                            document.getChar(position-1) == '\r')) position--;
                    }
                    else return;
                }
                else
                {
                    ITypedRegion partition =
                        partitioner.getPartition(position, false);

                    int partitionStart = partition.getOffset();

                    if (position == partitionStart && position > 0)
                    {
                        partition = partitioner.getPartition(position-1, false);
                        partitionStart = partition.getOffset();
                    }

                    while (position > partitionStart &&
                        Character.isWhitespace(document.getChar(position-1)) &&
                        document.getChar(position-1) != '\n' &&
                        document.getChar(position-1) != '\r') position--;

                    while (position > partitionStart &&
                           !Character.isWhitespace(document.getChar(position-1))) position--;
                }

                setCaretPosition(position);
                getTextWidget().showSelection();
                fireSelectionChanged();
            }
            catch (BadLocationException e) // should never occur
            {
                PerlEditorPlugin.getDefault().getLog().log(
                    new Status(
                        IStatus.ERROR,
                        PerlEditorPlugin.getPluginId(),
                        IStatus.OK,
                        "An unexpected exception occurred in PreviousWordAction",
                        e));

                super.run(); // fall back on default behavior
            }
        }
    }
}