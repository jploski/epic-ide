package cbg.editor;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;
import cbg.editor.jedit.Mode;
import cbg.editor.rules.ColorManager;
/**
 * Insert the type's description here.
 * @see EditorPart
 */
public class ColoringEditor extends StatusTextEditor {
	private TabConverter tabConverter;
	interface ITextConverter {
		void customizeDocumentCommand(IDocument document, DocumentCommand command);
	};
	class AdaptedSourceViewer extends SourceViewer {
		private List textConverters;
		private boolean ignoreTextConverters = false;
		public AdaptedSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
			super(parent, ruler, styles);
		}
		/*
		 * @see ITextOperationTarget#doOperation(int)
		 */
		public void doOperation(int operation) {
			if (getTextWidget() == null)
				return;
			switch (operation) {
				case UNDO :
					ignoreTextConverters = true;
					break;
				case REDO :
					ignoreTextConverters = true;
					break;
			}
			super.doOperation(operation);
		}
		public void addTextConverter(ITextConverter textConverter) {
			if (textConverters == null) {
				textConverters = new LinkedList();
				textConverters.add(textConverter);
			} else if (!textConverters.contains(textConverter))
				textConverters.add(textConverter);
		}
		public void removeTextConverter(ITextConverter textConverter) {
			if (textConverters != null) {
				textConverters.remove(textConverter);
				if (textConverters.size() == 0)
					textConverters = null;
			}
		}
		/*
		 * @see TextViewer#customizeDocumentCommand(DocumentCommand)
		 */
		protected void customizeDocumentCommand(DocumentCommand command) {
			super.customizeDocumentCommand(command);
			if (!ignoreTextConverters && textConverters != null) {
				for (Iterator e = textConverters.iterator(); e.hasNext();)
					 ((ITextConverter) e.next()).customizeDocumentCommand(getDocument(), command);
			}
			ignoreTextConverters = false;
		}
		public void updateIndentationPrefixes() {
			SourceViewerConfiguration configuration = getSourceViewerConfiguration();
			String[] types = configuration.getConfiguredContentTypes(this);
			for (int i = 0; i < types.length; i++) {
				String[] prefixes = configuration.getIndentPrefixes(this, types[i]);
				if (prefixes != null && prefixes.length > 0)
					setIndentPrefixes(prefixes, types[i]);
			}
		}
	};
	static class TabConverter implements ITextConverter {
		private int fTabRatio;
		private ILineTracker fLineTracker;
		public TabConverter() {
		}
		public void setNumberOfSpacesPerTab(int ratio) {
			fTabRatio = ratio;
		}
		public void setLineTracker(ILineTracker lineTracker) {
			fLineTracker = lineTracker;
		}
		private int insertTabString(StringBuffer buffer, int offsetInLine) {
			if (fTabRatio == 0)
				return 0;
			int remainder = offsetInLine % fTabRatio;
			remainder = fTabRatio - remainder;
			for (int i = 0; i < remainder; i++)
				buffer.append(' ');
			return remainder;
		}
		public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
			String text = command.text;
			if (text == null)
				return;
			int index = text.indexOf('\t');
			if (index > -1) {
				StringBuffer buffer = new StringBuffer();
				fLineTracker.set(command.text);
				int lines = fLineTracker.getNumberOfLines();
				try {
					for (int i = 0; i < lines; i++) {
						int offset = fLineTracker.getLineOffset(i);
						int endOffset = offset + fLineTracker.getLineLength(i);
						String line = text.substring(offset, endOffset);
						int position = 0;
						if (i == 0) {
							IRegion firstLine = document.getLineInformationOfOffset(command.offset);
							position = command.offset - firstLine.getOffset();
						}
						int length = line.length();
						for (int j = 0; j < length; j++) {
							char c = line.charAt(j);
							if (c == '\t') {
								position += insertTabString(buffer, position);
							} else {
								buffer.append(c);
								++position;
							}
						}
					}
					command.text = buffer.toString();
				} catch (BadLocationException x) {
				}
			}
		}
	};
	/**
	 * The constructor.
	 */
	public ColoringEditor() {
		super();
		setDocumentProvider(new ColoringDocumentProvider());
		ColoringEditorTools tools = EditorPlugin.getDefault().getEditorTools();
		ColorManager colorManager = EditorPlugin.getDefault().getColorManager();
		setSourceViewerConfiguration(new ColoringSourceViewerConfiguration(colorManager, tools));
		/* This is needed for the editor to respond to preference changes from
		 * the ColoringPreferencePage. When the Workbench's preferences change
		 * we want to update too, if they are font changes.
		 */
		IPreferenceStore store = EditorPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		WorkbenchChainedTextFontFieldEditor.startPropagate(store, PREFERENCE_FONT);
	}
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (input instanceof IStorageEditorInput) {
			String filename = ((IStorageEditorInput) input).getStorage().getName();
			((ColoringSourceViewerConfiguration) getSourceViewerConfiguration()).setFilename(filename);
			configureTabConverter();
		}
	}
	private void configureTabConverter() {
		if (tabConverter != null) {
			tabConverter.setLineTracker(new DefaultLineTracker());
		}
	}
	private void startTabConversion() {
		if (tabConverter == null) {
			tabConverter = new TabConverter();
			configureTabConverter();
			tabConverter.setNumberOfSpacesPerTab(getTabSize());
			AdaptedSourceViewer asv = (AdaptedSourceViewer) getSourceViewer();
			asv.addTextConverter(tabConverter);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			asv.updateIndentationPrefixes();
		}
	}
	private int getTabSize() {
		return EditorPlugin.getDefault().getPreferenceStore().getInt(ColoringSourceViewerConfiguration.PREFERENCE_TAB_WIDTH);
	}
	private void stopTabConversion() {
		if (tabConverter != null) {
			AdaptedSourceViewer asv = (AdaptedSourceViewer) getSourceViewer();
			asv.removeTextConverter(tabConverter);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			asv.updateIndentationPrefixes();
			tabConverter = null;
		}
	}
	protected void updateStatusField(String category) {
		if (category == null)
			return;
		if (category.equals(EditorPlugin.STATUS_CATEGORY_MODE)) {
			IStatusField field = getStatusField(category);
			if (field != null) {
				field.setImage(EditorPlugin.getImage("box"));
				Mode mode = ((ColoringSourceViewerConfiguration) getSourceViewerConfiguration()).getMode();
				String text = mode == null ? "No mode" : mode.getDisplayName();
				field.setText(text);
				return;
			}
		}
		super.updateStatusField(category);
	}
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return EditorPlugin.getDefault().getEditorTools().affectsTextPresentation(event);
	}
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		if (getSourceViewer() == null || getSourceViewer().getTextWidget() == null)
			return;
		try {
			AdaptedSourceViewer asv = (AdaptedSourceViewer) getSourceViewer();
			if (asv != null) {
				String p = event.getProperty();
				if (ColoringSourceViewerConfiguration.SPACES_FOR_TABS.equals(p)) {
					if (isTabConversionEnabled())
						startTabConversion();
					else
						stopTabConversion();
					return;
				}
				if (ColoringSourceViewerConfiguration.PREFERENCE_TAB_WIDTH.equals(p)) {
					asv.updateIndentationPrefixes();
					if (tabConverter != null)
						tabConverter.setNumberOfSpacesPerTab(getTabSize());
					Object value = event.getNewValue();
					if (value instanceof Integer) {
						asv.getTextWidget().setTabs(((Integer) value).intValue());
					} else if (value instanceof String) {
						asv.getTextWidget().setTabs(Integer.parseInt((String) value));
					}
					return;
				}
			}
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		if (isTabConversionEnabled())
			startTabConversion();
	}
	private boolean isTabConversionEnabled() {
		IPreferenceStore store = getPreferenceStore();
		return store.getBoolean(ColoringSourceViewerConfiguration.SPACES_FOR_TABS);
	}
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		return new AdaptedSourceViewer(parent, ruler, styles);
	}
}
