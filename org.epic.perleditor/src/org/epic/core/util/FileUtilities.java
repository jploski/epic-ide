/*
 * Created on Feb 21, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.core.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author luelljoc
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FileUtilities {
	static FileEditorInput getFileEditorInput(IPath fPath) {
		IFile iFile = null;
		IFile[] files =
			PerlEditorPlugin.getWorkspace().getRoot().findFilesForLocation(
				fPath);

		if (files.length == 0) {
			IProject prj =
				PerlEditorPlugin.getWorkspace().getRoot().getProject(
					"EPIC_LinkedPerlFiles");
			if (!prj.exists()) {
				try {
					prj.create(null);
				} catch (Exception e) {
					System.out.println(e);
				}
			}

			try {
				prj.open(null);
			} catch (Exception e) {
				System.out.println(e);
			}

			long time = System.currentTimeMillis();
			String name;
			name = Long.toString(time);

			IFolder link = prj.getFolder(name);
			while (link.exists()) {
				name = name + "_";
				link = prj.getFolder(name);
			}

			try {
				link.createLink(
					fPath.removeLastSegments(1),
					IResource.NONE,
					null);
			} catch (Exception e) {
				System.out.println(e);
			}

			files =
				PerlEditorPlugin.getWorkspace().getRoot().findFilesForLocation(
					fPath);
			if (files.length > 0)
				iFile = files[0];
		} else
			iFile = files[0];

		return (new FileEditorInput(iFile));
	}
}
