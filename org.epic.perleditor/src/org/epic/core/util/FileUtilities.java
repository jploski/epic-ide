package org.epic.core.util;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author luelljoc
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FileUtilities
{
	public static FileEditorInput getFileEditorInput(IPath fPath)
    {
        IWorkspaceRoot root = PerlEditorPlugin.getWorkspace().getRoot();

        try
        {
            IFile[] files = root.findFilesForLocation(fPath);
    		if (files.length > 0) return new FileEditorInput(files[0]); // found

            // not found, let's create a link to its parent folder
            // and search again
            createFolderLink(fPath, getEpicLinksProject(root));
    
    		files = root.findFilesForLocation(fPath);    
            if (files.length > 0) return new FileEditorInput(files[0]); // found
            
            // we have the link and the file still can't be found??
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlEditorPlugin.getPluginId(),
                IStatus.OK,
                fPath.toOSString() + " could not be found through epic-links", 
                null));
        }
        catch (CoreException e)
        {
            IStatus[] status;
            IPath folderPath = fPath.removeLastSegments(1);
            
            if (root.getLocation().isPrefixOf(folderPath) ||
                folderPath.isPrefixOf(root.getLocation()))    
            {
                status = new IStatus[] {
                    e.getStatus(),
                    new Status(
                        IStatus.ERROR,
                        PerlEditorPlugin.getPluginId(),
                        IStatus.OK,
                        "EPIC cannot access files located in folders on the path " +
                        "to the workspace folder, nor within the workspace folder itself.",
                        null)
                    };
            }
            else
            {
                status = new IStatus[] { e.getStatus() };   
            }
            
            PerlEditorPlugin.getDefault().getLog().log(
                new MultiStatus(
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    status,
                    "An unexpected exception occurred while creating a link to " +
                    fPath.toString(),
                    e));
            
            // TODO: propagate this exception and/or update client code
            return null; 
        }
	}

    private static void createFolderLink(IPath fPath, IProject prj)
        throws CoreException
    {
        String name = Long.toString(System.currentTimeMillis());
		IFolder link = prj.getFolder(name);

		while (link.exists())
        {
			name = name + "_";
			link = prj.getFolder(name);
		}

        link.createLink(
            fPath.removeLastSegments(1),
			IResource.NONE,
			null);
    }

    private static IProject getEpicLinksProject(IWorkspaceRoot root)
        throws CoreException
    {
        IProject prj = root.getProject("epic_links");

		if (!prj.exists())
		{
            prj.create(null);
            prj.open(null);
            IProjectDescription description = prj.getDescription();
            String[] natures = new String[1];
            natures[0] = "org.epic.perleditor.perlinkexternalfilesnature";
            description.setNatureIds(natures);					      
            prj.setDescription(description, null);
		}
		else prj.open(null);

        return prj;
    }
}
