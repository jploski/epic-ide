/*
 * Created on Jun 4, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.core.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

/**
 * @author luelljoc
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class NatureUtilities {
	public static void addNature(IProject project, String nature) {
		try {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = nature;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static void removeNature(IProject project, String nature) {
		try {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();

			int found = 0;
			for (int i = 0; i < prevNatures.length; i++) {
				if (prevNatures[i].equals(nature)) {
					found = i;
					break;
				}
			}

			if (found != -1) {
				String[] newNatures = new String[prevNatures.length - 1];
				System.arraycopy(prevNatures, 0, newNatures, 0, found);
				System.arraycopy(prevNatures, found+1, newNatures, found, newNatures.length-found);
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}