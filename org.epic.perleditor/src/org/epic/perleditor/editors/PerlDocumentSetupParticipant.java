package org.epic.perleditor.editors;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Implements the org.eclipse.core.filebuffers.documentSetup extension
 * point to connect PerlPartitioner to documents created by
 * the ITextFileBufferManager.
 */
public class PerlDocumentSetupParticipant implements IDocumentSetupParticipant
{
    public void setup(IDocument document)
    {
        // TODO: it would be wiser to use a specific Perl partitioning
        // category instead of the default partitioning...
        
        IDocumentPartitioner partitioner =
            new PerlPartitioner(PerlEditorPlugin.getDefault().getLog());
        document.setDocumentPartitioner(partitioner);
        partitioner.connect(document);
    }
}