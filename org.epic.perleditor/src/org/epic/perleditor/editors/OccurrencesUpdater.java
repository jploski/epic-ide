package org.epic.perleditor.editors;

import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.MarkOccurrencesPreferences;

/**
 * This class marks occurrences of a selection in the current document. The
 * class uses the document partitioner to mark the surrounding text. Which types
 * are provided, can be determined by the preference page "Mark Occurences".
 * 
 * @author Katrin Dust
 */
public class OccurrencesUpdater implements ISelectionChangedListener {

	/*
	 * Pattern used to match a char (a-z or A-Z) or a digit
	 */
	private static final Pattern LETTER_PATTERN = Pattern
			.compile("[a-zA-Z]||\\d");

	/*
	 * Annotation type used in extension point
	 */
	private static final String ANNOTATION_TYPE = "org.epic.perleditor.occurrence";

	/*
	 * List of current annotations (used for removing)
	 */
	private final LinkedList annotations = new LinkedList();

	/*
	 * stores the last marked test
	 */
	private String lastMarkedText = "";

	/**
	 * Constructor
	 */
	public OccurrencesUpdater() {
	}

	/**
	 * The new selected text and further occurrences will be marked, if the type
	 * of the text is selected in the preference page "Mark Occurrences". Old
	 * markers are removed. If the selected text contains no characters, the
	 * typedRegion of the document partitioner determines the marked text.
	 * Further occurrences of the text will be marked, if they've got the same
	 * contentType. Variables are also marked in strings.
	 * 
	 * @param event
	 *            selection changed event
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		// get model for highlighting
		ISourceViewer viewer = (ISourceViewer) event.getSource();
		IAnnotationModel _model = viewer.getAnnotationModel();        
        if (!(_model instanceof IAnnotationModelExtension)) return;        
        IAnnotationModelExtension model = (IAnnotationModelExtension) _model;
        
		if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
				MarkOccurrencesPreferences.MARK_OCCURRENCES)) {
			this.lastMarkedText = "";
			// remove old ones
			removeAnnotations(model);
			return;
		}
		// get document
		IDocument doc = viewer.getDocument();
		if (doc == null) {
			return;
		}
		try {
			ITextSelection textSelection = (ITextSelection) event
					.getSelection();
			String contentType = doc.getDocumentPartitioner().getPartition(
					textSelection.getOffset()).getType();
			if (!validSelection(contentType)) {
				if (!PerlEditorPlugin.getDefault().getPreferenceStore()
						.getBoolean(MarkOccurrencesPreferences.KEEP_MARKS)) {
					this.lastMarkedText = "";
					removeAnnotations(model);
				}
				return;
			}
			String text = defineText(doc, textSelection);
			// same text?
			if (text.equals(this.lastMarkedText)) {
				return;
			} else {
				this.lastMarkedText = text;
			}
			// remove old ones
			removeAnnotations(model);
			String type = doc.getContentType(textSelection.getOffset());
			markText(doc, type, text, model);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The method checks, if the contentType is selected in the preference page
	 * to mark occurrences of this type. It returns true, if the type of the
	 * textselection is a valid type and selected.
	 * 
	 * @param contentType
	 *            contentType
	 * @return true, if the contentType is valid and selected
	 */
	private boolean validSelection(String contentType) {
		if (contentType.equals(PartitionTypes.DEFAULT)) {
			return false;
		} else if (contentType.equals(PartitionTypes.VARIABLE)) {
			if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
					MarkOccurrencesPreferences.VARIABLE)) {
				return false;
			}
		} else if (contentType.equals(PartitionTypes.COMMENT)) {
			if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
					MarkOccurrencesPreferences.COMMENT)) {
				return false;
			}
		} else if (contentType.equals(PartitionTypes.KEYWORD1)
				|| contentType.equals(PartitionTypes.KEYWORD2)) {
			if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
					MarkOccurrencesPreferences.KEYWORD)) {
				return false;
			}
		} else if (contentType.equals(PartitionTypes.LITERAL1)
				|| contentType.equals(PartitionTypes.LITERAL2)) {
			if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
					MarkOccurrencesPreferences.LITERAL)) {
				return false;
			}
		} else if (contentType.equals(PartitionTypes.NUMBER)) {
			if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
					MarkOccurrencesPreferences.NUMBER)) {
				return false;
			}
		} else if (contentType.equals(PartitionTypes.OPERATOR)) {
			if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
					MarkOccurrencesPreferences.OPERATOR)) {
				return false;
			}
		}
		if (contentType.equals(PartitionTypes.POD)) {
			if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
					MarkOccurrencesPreferences.POD)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The method defines a text for a given textSelection and a document. If no
	 * character is selected the text of the typedRegion, determined by the
	 * document partitioner, is returned; otherwise the selected text.
	 * 
	 * @param doc
	 *            doc of the textselection
	 * @param textSelection
	 *            textselection within the document
	 * @return the text
	 * @throws BadLocationException,
	 *             thrown if the textSelection is not within the document
	 */
	private String defineText(IDocument doc, ITextSelection textSelection)
			throws BadLocationException {
		String text;
		if (textSelection.getLength() < 1) {
			ITypedRegion typedRegion = doc.getDocumentPartitioner()
					.getPartition(textSelection.getOffset());
			text = doc.get(typedRegion.getOffset(), typedRegion.getLength());
		} else {
			text = textSelection.getText();
		}
		return text;
	}

	/**
	 * The method removes all annotation from the given model, which are in the
	 * current annotation list
	 * 
	 * @param model
	 */
	private void removeAnnotations(IAnnotationModelExtension model) {        
        Annotation[] array = (Annotation[])
            annotations.toArray(new Annotation[annotations.size()]);

        model.replaceAnnotations(array, Collections.EMPTY_MAP);
        annotations.clear();
	}

	/**
	 * The method marks occurrences of the text in the document by adding
	 * annotations to the given model. Further occurrences of the text and the
	 * text itself will be marked, if they've got the same contentType.
	 * Variables are also marked in strings.
	 * 
	 * @param doc
	 *            the document, used to get a FindReplaceDocumentAdapter and the
	 *            contentTypes
	 * @param type
	 *            the contentType of the associated text
	 * @param text
	 *            the text, further occurrences will be marked
	 * @param model
	 *            the model, the annotation were added
	 * 
	 */
	private void markText(IDocument doc, String type, String text,
			IAnnotationModelExtension model) {
		if (text == null || (type == null)) {
			return;
		}
		int offset = 0;
		String docText = doc.get();
		int index = docText.indexOf(text, offset);
		try {
            Map newAnnotations = new HashMap();
			while (index != -1) {
				offset = index + text.length();
				String contentType = doc.getContentType(index);
				if (contentType.equals(type)
						|| contentType.equals(PartitionTypes.LITERAL1)
						&& type.equals(PartitionTypes.VARIABLE)
						|| (contentType.equals(PartitionTypes.VARIABLE) && type
								.equals(PartitionTypes.LITERAL1))) {
					String behind = "" + doc.getChar(offset + 1);
					if (!(LETTER_PATTERN.matcher(behind)).matches()) {
						addAnnotation(text, newAnnotations, index, text.length());
					}
				}
				index = docText.indexOf(text, offset);
			}
            model.replaceAnnotations(new Annotation[] {}, newAnnotations);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

	}

	/**
	 * The method adds a new Annotation to the model. The offset and the length
	 * determine the position of the annotation. The annotation is not
	 * persistent.
	 * 
	 * @param text
	 *            the associated text of the annotation
	 * @param model
	 *            the model, the annotation will be added to
	 * @param offset
	 *            the offset of the annotation position
	 * @param length
	 *            the length of the annotation position
	 */
	private void addAnnotation(String text, Map newAnnotations, int offset,
			int length) {
		Annotation annotation = new Annotation(ANNOTATION_TYPE, false, text);
		Position position = new Position(offset, length);
		annotations.add(annotation);       
        newAnnotations.put(annotation, position);
	}

	/**
	 * Installs this selection changed listener with the given selection
	 * provider. If the selection provider is a post selection provider, post
	 * selection changed events are the preferred choice, otherwise normal
	 * selection changed events are requested.
	 * 
	 * @param selectionProvider
	 */
	public void install(ISelectionProvider selectionProvider) {
		if (selectionProvider == null) {
			return;
		}
		if (selectionProvider instanceof IPostSelectionProvider) {
			IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
			provider.addPostSelectionChangedListener(this);
		} else {
			selectionProvider.addSelectionChangedListener(this);
		}
	}
}
