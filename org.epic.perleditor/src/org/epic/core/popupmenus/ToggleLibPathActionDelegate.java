package org.epic.core.popupmenus;

import java.io.IOException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.epic.core.util.XMLUtilities;

import com.sun.org.apache.xerces.internal.impl.dtd.models.CMLeaf;

public class ToggleLibPathActionDelegate implements IObjectActionDelegate {

	private IWorkbenchPart part;
	private IFolder folder = null;

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
		ISelection selection = part.getSite().getSelectionProvider()
				.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object firstElement = sel.getFirstElement();
			if (firstElement instanceof IFolder) {
				folder = (IFolder) firstElement;
				if (folder.isAccessible()) {
					if (isInIncPath(folder)) {
						action.setText("Remove From Perl Include Path");
					} else {
						action.setText("Add to Perl Include Path");
					}
				}
				action.setEnabled(folder.isAccessible());
			}
		}
	}

	public void run(IAction action) {
		if (folder != null) {
			if (folder.isAccessible()) {
				if (isInIncPath(folder)) {
					changeIncPath(folder, "REMOVE");
				} else {
					changeIncPath(folder, "ADD");
				}
			}
		}

	}

	private boolean changeIncPath(IFolder folder, String action) {
		XMLUtilities xmlUtil = new XMLUtilities();
		int currentIncPathsCount = xmlUtil.getIncludeEntries(folder
				.getProject()).length;
		String[] incPaths = null;
		if (action.contains("ADD")) {
			incPaths = new String[currentIncPathsCount + 1];
			System.arraycopy(xmlUtil.getIncludeEntries(folder.getProject()), 0,
					incPaths, 0, currentIncPathsCount);
			incPaths[currentIncPathsCount] = folder.getLocation().toString();
		} else if (action.contains("REMOVE")) {
			incPaths = new String[currentIncPathsCount - 1];
			int incPathIndex = 0;
			for (int index = 0; index < currentIncPathsCount; index++) {
				if (xmlUtil.getIncludeEntries(folder.getProject())[index]
						.matches(folder.getLocation().toString())) {
					// don't add this entry
				} else {
					incPaths[incPathIndex++] = xmlUtil.getIncludeEntries(folder
							.getProject())[index];
				}
			}
		} else if (action.contains("ISPRESENT")) {
			incPaths = xmlUtil.getIncludeEntries(folder.getProject());
			for (int index = 0; index < incPaths.length; index++) {
				if (incPaths[index].matches(folder.getLocation().toString())) {
					return true;
				}
			}
			return false;
		} else {
			// we don't support any other action yet
			return false;
		}
		try {
			xmlUtil.writeIncludeEntries(folder.getProject(), incPaths);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Sugar Function to check if folder is present in project INC path
	 * 
	 * @param folder
	 * @return
	 */
	private boolean isInIncPath(IFolder folder) {
		return changeIncPath(folder, "ISPRESENT");
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
