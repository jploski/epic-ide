package cbg.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class ColoringDocumentProvider extends FileDocumentProvider {

	public ColoringDocumentProvider() {
		super();
	}

	protected IDocument createDocument(Object element) throws CoreException {
		IDocument doc = super.createDocument(element);
		String filename = null;
		if(element instanceof IStorageEditorInput) {
			filename = ((IStorageEditorInput)element).getStorage().getName();
		}
		initializeDocument(doc, filename);
		return doc;
	}

	private void initializeDocument(IDocument document, String filename) {
		if (document != null) {		
			IDocumentPartitioner partitioner = createPartitioner(filename);
			document.setDocumentPartitioner(partitioner);
			partitioner.connect(document);
		}
	}

	private IDocumentPartitioner createPartitioner(String filename) {
		ColoringPartitionScanner scanner = new ColoringPartitionScanner(filename);		
		IDocumentPartitioner partitioner = new ColoringPartitioner(scanner, scanner.getContentTypes());
		return partitioner;
	}

}
