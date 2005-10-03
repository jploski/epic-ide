package org.epic.perleditor.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.internal.ViewerActionBuilder;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.epic.core.util.FileUtilities;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.actions.*;
import org.epic.perleditor.editors.util.PerlColorProvider;
import org.epic.perleditor.popupmenus.OpenDeclaration;
import org.epic.perleditor.popupmenus.PerlDocAction;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.templates.perl.ModuleCompletionHelper;
import org.epic.perleditor.views.PerlOutlinePage;
import org.epic.perleditor.views.model.*;

import cbg.editor.ColoringSourceViewerConfiguration;

/**
 * Perl specific text editor.
 */

public class PerlEditor extends TextEditor
    implements ISelectionChangedListener, IPropertyChangeListener
{
    /**
     * Editor id, as declared in the plug-in manifest.
     */
    public static final String PERL_EDITOR_ID =
        "org.epic.perleditor.editors.PerlEditor";

    /** The outline page */
    private PerlContentOutlinePage fOutlinePage;

    protected PerlOutlinePage page;

    protected PerlSyntaxValidationThread fValidationThread = null;

    protected PerlToDoMarkerThread fTodoMarkerThread = null;

    protected PerlFoldingThread fFoldingThread = null;

    protected CompositeRuler ruler;

    protected LineNumberRulerColumn numberRuler;

    private StyleRange newStyleRange = new StyleRange();

    private final org.eclipse.swt.graphics.Color tempColorBack = new org.eclipse.swt.graphics.Color(
        null, 87, 207, 215);

    private final org.eclipse.swt.graphics.Color tempColorFore = new org.eclipse.swt.graphics.Color(
        null, 255, 255, 0);

    private ISourceViewer fSourceViewer;

    private IDocument document;

    private IdleTimer idleTimer;

    private final static String PERL_MODE = "perl";

    private ProjectionSupport projectionSupport;

    private int lastOutlineHashCode;

    int iZ = 0;
    
    private final PerlPairMatcher fBracketMatcher =
        new PerlPairMatcher(PerlEditorPlugin.getDefault().getLog());
    
    private final PerlBracketInserter fBracketInserter =
        new PerlBracketInserter(PerlEditorPlugin.getDefault().getLog());

    /**
     * Default constructor();
     */

    public PerlEditor()
    {
        super();

        setDocumentProvider(new PerlDocumentProvider());

        PerlEditorPlugin.getDefault().getPreferenceStore()
            .addPropertyChangeListener(this);

        this.setPreferenceStore(PerlEditorPlugin.getDefault()
            .getPreferenceStore());
        setKeyBindingScopes(new String[]
        { "org.epic.perleditor.perlEditorScope" });
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method extend
     * the actions to add those specific to the receiver
     */

    protected void createActions()
    {
        super.createActions();

        Action action;

        action = new ContentAssistAction(
            PerlEditorMessages.getResourceBundle(),
            "ContentAssistProposal.",
            this);
        action.setActionDefinitionId(PerlEditorCommandIds.CONTENT_ASSIST);
        setAction(PerlEditorActionIds.CONTENT_ASSIST, action);
        
        action = new Jump2BracketAction(this);
        action.setActionDefinitionId(PerlEditorCommandIds.MATCHING_BRACKET);
        setAction(action.getId(), action);
        
        action = new FormatSourceAction(this);
        action.setActionDefinitionId(PerlEditorCommandIds.FORMAT_SOURCE);
        setAction(action.getId(), action);
        
        action = new ExportHtmlSourceAction(this);
        action.setActionDefinitionId(PerlEditorCommandIds.HTML_EXPORT);
        setAction(action.getId(), action);
        
        action = new ValidateSourceAction(this);
        action.setActionDefinitionId(PerlEditorCommandIds.VALIDATE_SYNTAX);
        setAction(action.getId(), action);
        
        action = new OpenDeclaration(this);
        action.setActionDefinitionId(PerlEditorCommandIds.OPEN_SUB);
        setAction(action.getId(), action);
        
        action = new ToggleCommentAction(this);
        action.setActionDefinitionId(PerlEditorCommandIds.TOGGLE_COMMENT);
        setAction(action.getId(), action);
        
        action = new PerlDocAction(this);
        action.setActionDefinitionId(PerlEditorCommandIds.PERL_DOC);
        setAction(action.getId(), action);
        
        // TODO: the following initialization steps have nothing to do with
        // the purpose of createActions and should be moved to a more sensible
        // location

        IDocumentProvider provider = getDocumentProvider();
        document = provider.getDocument(getEditorInput());
        fSourceViewer = getSourceViewer();
        fSourceViewer.setDocument(document);
        
        fBracketMatcher.setViewer(fSourceViewer);
        fBracketInserter.setViewer(fSourceViewer);
        
        ModuleCompletionHelper completionHelper = 
        	ModuleCompletionHelper.getInstance();
        completionHelper.scanForModules(this);
        
        if (fValidationThread == null && isPerlMode())
        {
            fValidationThread = new PerlSyntaxValidationThread();
            // Set thread priority to minimal
            fValidationThread.setPriority(Thread.MIN_PRIORITY);
            // Thread defaults
            fValidationThread.start();
        }

        if (fValidationThread != null)
        {
            // Always check syntax when editor is opened
            fValidationThread.setDocument(
                (IResource) ((IAdaptable) getEditorInput()).getAdapter(IResource.class),
                document);
        }

        // set up the ToDoMarkerThread
        if ((fTodoMarkerThread == null) && isPerlMode())
        {
            fTodoMarkerThread = new PerlToDoMarkerThread(this, fSourceViewer);
            fTodoMarkerThread.start();
        }

        // set up the FoldingThread
        if ((fFoldingThread == null) && isPerlMode())
        {
            fFoldingThread = new PerlFoldingThread(this, fSourceViewer);
            fFoldingThread.start();
        }

        setEditorForegroundColor();

        // Setup idle timer
        idleTimer = new IdleTimer(fSourceViewer, Display.getCurrent());
        idleTimer.start();

        // Register the validation thread if automatic checking is enabled
        if (PerlEditorPlugin.getDefault().getSyntaxValidationPreference())
        {
            this.registerIdleListener(fValidationThread);
        }

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

    public void dispose()
    {
        try
        {
            // IEditorInput input = this.getEditorInput();
            // IResource resource = (IResource) ((IAdaptable) input)
            // .getAdapter(IResource.class);

            // resource.deleteMarkers(IMarker.PROBLEM, true, 1);
            
            if (fSourceViewer instanceof ITextViewerExtension)
                ((ITextViewerExtension) fSourceViewer).removeVerifyKeyListener(fBracketInserter);

            if (fValidationThread != null)
            {
                fValidationThread.dispose();
            }

            if (idleTimer != null)
            {
                idleTimer.dispose();
            }

            if (fOutlinePage != null)
            {
                fOutlinePage.dispose();
            }

            if (fTodoMarkerThread != null)
            {
                fTodoMarkerThread.dispose();
            }

            if (fFoldingThread != null)
            {
                fFoldingThread.dispose();
            }

            super.dispose();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * any extra revert behavior required by the Perl editor.
     */

    public void doRevertToSaved()
    {

        super.doRevertToSaved();

        if (page != null)
        {
            // Update only if input has changed
            // check here before we retrieve the Outline-List
            int hashCode = getSourceViewer().getDocument().get().hashCode();
            if (hashCode == lastOutlineHashCode)
            {
                return;
            }
            lastOutlineHashCode = hashCode;
            page.update(page.getSubList(), page.getModList());
        }

        if (fValidationThread != null)
        {
            fValidationThread.revalidate();
        }

    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * any extra save behavior required by the Perl editor.
     */

    public void doSave(IProgressMonitor monitor)
    {

        super.doSave(monitor);

        if (page != null)
        {
            // Update only if input has changed
            // check here before we retrieve the Outline-List
            int hashCode = getSourceViewer().getDocument().get().hashCode();
            if (hashCode == lastOutlineHashCode)
            {
                return;
            }
            lastOutlineHashCode = hashCode;
            page.update(page.getSubList(), page.getModList());
        }

        if (fValidationThread != null)
        {
            fValidationThread.revalidate();
        }

    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * any extra save as behavior required by the Perl editor.
     */

    public void doSaveAs()
    {

        super.doSaveAs();

        if (page != null)
        {
            // Update only if input has changed
            // check here before we retrieve the Outline-List
            int hashCode = getSourceViewer().getDocument().get().hashCode();
            if (hashCode == lastOutlineHashCode)
            {
                return;
            }
            lastOutlineHashCode = hashCode;
            page.update(page.getSubList(), page.getModList());
        }

        if (fValidationThread != null)
        {
            fValidationThread.revalidate();
        }

    }

    /**
     * The PerlEditor implementation of this AbstractTextEditor method performs
     * sets the input of the outline page after AbstractTextEditor has set
     * input.
     */

    public void doSetInput(IEditorInput input)
        throws CoreException
    {

        /* Map external files into workspace (epic-links) */
        if (input instanceof ILocationProvider)
        {
            ILocationProvider l = (ILocationProvider) input
                .getAdapter(ILocationProvider.class);
            if (l != null)
                input = FileUtilities.getFileEditorInput(l.getPath(l)
                    .makeAbsolute());
        }

        super.doSetInput(input);
        // Set coloring editor mode
        if (input instanceof IStorageEditorInput)
        {
            String filename = ((IStorageEditorInput) input).getStorage()
                .getName();
            ((ColoringSourceViewerConfiguration) getSourceViewerConfiguration())
                .setFilename(filename);
        }
    }

    public void rulerContextMenuAboutToShow(IMenuManager menu)
    {
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

    public void editorContextMenuAboutToShow(IMenuManager menu)
    {
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

    public Object getAdapter(Class adapter)
    {

        if (ProjectionAnnotationModel.class.equals(adapter))
        {
            if (this.projectionSupport != null)
            {
                Object result = this.projectionSupport.getAdapter(
                    getSourceViewer(), adapter);
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
                page = new PerlOutlinePage(this, getSourceViewer());

                this.registerIdleListener(page);

                page.addSelectionChangedListener(this);
                return page;
            }

        }

        return super.getAdapter(adapter);
    }

    public void updateOutline()
    {
        if (page != null)
        {
            // Update only if input has changed
            // check here before we retrieve the Outline-List
            int hashCode = getSourceViewer().getDocument().get().hashCode();
            if (hashCode == lastOutlineHashCode)
            {
                return;
            }
            lastOutlineHashCode = hashCode;
            page.update(page.getSubList(), page.getModList());
        }

    }

    /*
     * Method declared on AbstractTextEditor
     */
    protected void initializeEditor()
    {
        setSourceViewerConfiguration(new PerlSourceViewerConfiguration(
            PerlEditorPlugin.getDefault().getPreferenceStore(), this));
        super.initializeEditor();
    }

    /* Create SourceViewer so we can use the PerlSourceViewer class */
    protected final ISourceViewer createSourceViewer(Composite parent,
        IVerticalRuler ruler, int styles)
    {

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

    public void selectionChanged(SelectionChangedEvent event)
    {
        if (event != null)
        {
            if (event.getSelection() instanceof IStructuredSelection)
            {
                IStructuredSelection sel = (IStructuredSelection) event
                    .getSelection();
                if (sel != null)
                {
                    if (sel.getFirstElement() instanceof Module
                        || sel.getFirstElement() instanceof Subroutine)
                    {
                        Model fe = (Model) sel.getFirstElement();
                        if (fe != null)
                        {
                            selectAndReveal(fe.getStart(), fe.getLength());
                        }
                    }
                }
            }
        }
    }

    public void revalidateSyntax(boolean forceUpdate)
    {
        if (fValidationThread != null)
        {
            fValidationThread.revalidate();
        }
    }

    /* Disabled for the time being. Will be re-enabled if necessary */
    // public void setFocus(){
    // super.setFocus();
    // revalidateSyntax(true);
    // }
    /**
     * 
     * @param documentPosition
     * @return true => the current text has no assigned ColourPattern, i.e.
     *         normal Text <br>
     *         false => the current text has an assigned ColourPattern (e.g.
     *         KEYWORD, LITERAL)
     */
    public final boolean isNormalText(int documentPosition)
    {
        try
        {
            if (getSourceViewer().getDocument().getPartition(documentPosition)
                .getType().toString().indexOf("__dftl_partition_content_type") < 0)
            {
                return false;
            }
        }
        catch (BadLocationException e)
        {
            // should not happen, since it is for FoldingThread and Outline only
        }
        return true;
    }

    public void refreshTaskView()
    {
        if (fTodoMarkerThread != null)
        {
            fTodoMarkerThread.setText(getSourceViewer().getDocument().get());
        }
    }

    /**
     * To provide an access to a changed position Colouring of the Brackets will
     * be adjusted.
     */
    public final void newCurosorPos()
    {
        handleCursorPositionChanged();
    }

    public final void foldingUpdate()
    {
        fFoldingThread.updateFoldingAnnotations();
    }
    
    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support)
    {
        support.setCharacterPairMatcher(fBracketMatcher);
        support.setMatchingCharacterPainterPreferenceKeys(
            PreferenceConstants.EDITOR_MATCHING_BRACKETS,
            PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR);

        super.configureSourceViewerDecorationSupport(support);
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
                if (!idleTimer.isRegistered(fValidationThread))
                {
                    this.registerIdleListener(fValidationThread);
                }
            }
            else
            {
                if (idleTimer.isRegistered(fValidationThread))
                {
                    idleTimer.removeListener(fValidationThread);
                }
            }
        }

        PerlSourceViewerConfiguration viewerConfiguration = (PerlSourceViewerConfiguration) this
            .getSourceViewerConfiguration();
        if (viewerConfiguration != null)
        {
            viewerConfiguration.adaptToPreferenceChange(event);

            setEditorForegroundColor();
        }
    }

    public void createPartControl(Composite parent)
    {
        // Workaround for Eclipse Bug 75440 (to fix it somehow) [LeO]
        if (!Workbench.getInstance().isClosing())
        {
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
            
            reconfigureBracketInserter();
            
            ISourceViewer sourceViewer = getSourceViewer();
            if (sourceViewer instanceof ITextViewerExtension)
                ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(
                    fBracketInserter);
        }
    }

    protected boolean affectsTextPresentation(PropertyChangeEvent event)
    {
        return PerlEditorPlugin.getDefault().getEditorTools()
            .affectsTextPresentation(event);
    }

    protected void handlePreferenceStoreChanged(PropertyChangeEvent event)
    {
        if (getSourceViewer() == null
            || getSourceViewer().getTextWidget() == null)
            return;
        
        reconfigureBracketInserter();
        super.handlePreferenceStoreChanged(event);
    }
    
    private void reconfigureBracketInserter()
    {
        IPreferenceStore preferenceStore = getPreferenceStore();

        fBracketInserter.setCloseBracketsEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET1));
        fBracketInserter.setCloseBracesEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET2));
        fBracketInserter.setCloseParensEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET3));
        fBracketInserter.setCloseAngularBracketsEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_BRACKET4));
        fBracketInserter.setCloseDoubleQuotesEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE1));
        fBracketInserter.setCloseSingleQuotesEnabled(
            preferenceStore.getBoolean(PreferenceConstants.AUTO_COMPLETION_QUOTE2));
    }

    public ISourceViewer getViewer()
    {
        return getSourceViewer();
    }

    /**
     * Checks if perlmode is used by the editor
     * 
     * @return true if in perl mode, otherwise false
     */
    public boolean isPerlMode()
    {
        return getModeName().equalsIgnoreCase(PERL_MODE);
    }

    /**
     * Returns the node name used by the editor
     * 
     * @return Mode name
     */
    public String getModeName()
    {
        String modeName = ((PerlSourceViewerConfiguration) getSourceViewerConfiguration())
            .getMode().getDisplayName();
        return modeName;
    }

    public void registerIdleListener(IdleTimerListener obj)
    {
        idleTimer.addListener(obj);
    }

    private final void setEditorForegroundColor()
    {
        // Set text editor forground colour
        RGB rgb = PreferenceConverter.getColor(PerlEditorPlugin.getDefault()
            .getPreferenceStore(), PreferenceConstants.EDITOR_STRING_COLOR);
        getSourceViewer().getTextWidget().setForeground(
            PerlColorProvider.getColor(rgb));
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
        fBracketMatcher.setViewer(null);
        
        try
        {
            int caretOffset = getSourceViewer().getSelectedRange().x;
            int matchOffset =
                findMatchingBracket(getSourceViewer().getDocument(), caretOffset);
            
            if (matchOffset == -1) return;
            
            getSourceViewer().revealRange(matchOffset + 1, 1);
            getSourceViewer().setSelectedRange(matchOffset + 1, 0);
        }
        finally
        {
            fBracketMatcher.setViewer(getSourceViewer());
        }
    }
    
    /**
     * Provided that the given document contains a bracket-like
     * character at the given offset, returns the offset of
     * the matching (pair) character (if found). Otherwise, returns -1.
     */
    public int findMatchingBracket(final IDocument document, final int offset)
    {
        final int[] ret = new int[1];
        getSourceViewer().getTextWidget().getDisplay().syncExec(new Runnable() {
            public void run() {        
                IRegion matchRegion = fBracketMatcher.match(document, offset);
                
                if (matchRegion == null) ret[0] = -1;
                else ret[0] =
                    matchRegion.getOffset() == offset - 1
                    ? matchRegion.getOffset() + matchRegion.getLength() - 1
                    : matchRegion.getOffset();
            } });

        return ret[0];
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
     * For test purposes only.
     */
    public void _clear()
    {
        getSourceViewer().getTextWidget().setText("");
    }
    
    /**
     * For test purposes only.
     */
    public int _getHighlightedBracketOffset()
    {
        int offset = getSourceViewer().getTextWidget().getCaretOffset();

        PerlPairMatcher matcher =
            new PerlPairMatcher(PerlEditorPlugin.getDefault().getLog());
        
        matcher.setViewer(null);
        matcher.match(getSourceViewer().getDocument(), offset);
        
        return
            offset - 1 == matcher.getStartPos()
            ? matcher.getEndPos()
            : matcher.getStartPos();
    }
    
    /**
     * For test purposes only.
     */
    public String _getText()
    {
        return getSourceViewer().getTextWidget().getText();
    }
    
    /**
     * For test purposes only.
     */
    public void _setCaretOffset(final int offset)
    {
        Display display = getSourceViewer().getTextWidget().getDisplay();
        getSourceViewer().setSelectedRange(offset, 0);
        while (display.readAndDispatch());
    }
    
    /**
     * For test purposes only.
     */
    public void _setExactBracketMatching()
    {
        fBracketMatcher.setViewer(null);
    }

    /**
     * For test purposes only.
     */
    public void _selectText(String text)
    {
        if (text.length() == 0)
        {
            getSelectionProvider().setSelection(TextSelection.emptySelection());
        }
        else
        {        
            int i = getSourceViewer().getDocument().get().indexOf(text);
            if (i == -1) throw new RuntimeException(
                "text \"" + text + "\" not found in editor");
            getSelectionProvider().setSelection(new TextSelection(i, text.length()));
        }
    }
}