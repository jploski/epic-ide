/*
 * Created on Jun 4, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.core.popupmenus;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
//import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
//import org.eclipse.ui.views.navigator.IResourceNavigator;
import org.epic.core.Constants;
import org.epic.core.util.NatureUtilities;

/**
 * @author luelljoc
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TogglePerlNatureActionDelegate implements IObjectActionDelegate {

	private IWorkbenchPart part;
	private IProject project = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
		//IResourceNavigator nav = (IResourceNavigator) part;
		
		ISelection selection = part.getSite()
				.getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel=(IStructuredSelection) selection;
			Object firstElement = sel.getFirstElement();
	
			if (firstElement instanceof IProject) {
				try {
					project = (IProject) firstElement;
					if (project.isAccessible()) {
						if (project.hasNature(Constants.PERL_NATURE_ID)) {
							action.setText("Remove Perl Nature");
						} else {
							action.setText("Add Perl Nature");
						}
					}
					action.setEnabled(project.isAccessible());
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {

		if (project != null) {
			try {
				if (project.isAccessible()) {
					if (project.hasNature(Constants.PERL_NATURE_ID)) {
						NatureUtilities.removeNature(project, Constants.PERL_NATURE_ID);
					} else {
						NatureUtilities.addNature(project, Constants.PERL_NATURE_ID);
					}
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {

	}

}