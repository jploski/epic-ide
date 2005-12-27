package org.epic.perleditor.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.epic.perleditor.PerlEditorPlugin;

public class PerlDocumentProvider extends FileDocumentProvider
{
    protected IDocument createDocument(Object element)
        throws CoreException
    {
        IDocument doc = super.createDocument(element);
        String filename = null;
        if (element instanceof IStorageEditorInput)
        {
            filename = ((IStorageEditorInput) element).getStorage().getName();
        }
        initializeDocument(doc, filename);
        return doc;
    }

    private void initializeDocument(IDocument document, String filename)
    {
        if (document != null)
        {
            IDocumentPartitioner partitioner = createPartitioner();
            document.setDocumentPartitioner(partitioner);
            partitioner.connect(document);
        }
    }

    private IDocumentPartitioner createPartitioner()
    {
        /*
        PerlPartitionScanner scanner = new PerlPartitionScanner();
        return new FastPartitioner(scanner, scanner.getContentTypes());
        */
        return new PerlPartitioner(PerlEditorPlugin.getDefault().getLog());
    }

    protected IAnnotationModel createAnnotationModel(Object element)
        throws CoreException
    {
        if (element instanceof IFileEditorInput)
        {
            IFileEditorInput input = (IFileEditorInput) element;
            return new PerlSourceAnnotationModel(input);
        }
        else
        {
            return super.createAnnotationModel(element);
        }
    }
}
