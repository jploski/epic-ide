package org.epic.perleditor.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Responsible for providing an IDocument instance to the editor.
 * The actual document creation is handled by the TextFileBufferManager,
 * while the required post-initialization is performed by this class.  
 */
public class PerlDocumentProvider extends TextFileDocumentProvider
{
    public void connect(Object element) throws CoreException
    {
        super.connect(element);

        connectPerlPartitioner(element);         
    }
    
    protected IAnnotationModel createAnnotationModel(IFile file)
    {
        return new PerlSourceAnnotationModel(file);
    }
    
    private void connectPerlPartitioner(Object input)
    {
        IDocument doc = getDocument(input);
        IDocumentExtension3 _doc;

        if (!(doc instanceof IDocumentExtension3)) return; // should never occur
        else _doc = (IDocumentExtension3) doc; 

        if (PartitionTypes.getPerlPartitioner(doc) == null)
        {
            IDocumentPartitioner partitioner =
                new PerlPartitioner(PerlEditorPlugin.getDefault().getLog());
            _doc.setDocumentPartitioner(PartitionTypes.PERL_PARTITIONING, partitioner);
            partitioner.connect(doc);
        }
    }
}
