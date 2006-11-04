package org.epic.perleditor.editors;

import org.eclipse.core.filebuffers.IAnnotationModelFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Implements the org.eclipse.core.filebuffers.annotationModelCreation
 * extension point to provide a PerlSourceAnnotationModel for documents
 * created by the ITextFileBufferManager.
 */
public class PerlAnnotationModelFactory implements IAnnotationModelFactory
{
    public IAnnotationModel createAnnotationModel(IPath location)
    {
        IWorkspaceRoot root = PerlEditorPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(location);
        return new PerlSourceAnnotationModel(resource);
    }
}
