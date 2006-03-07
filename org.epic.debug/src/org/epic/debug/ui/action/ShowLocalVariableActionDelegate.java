package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.epic.debug.PerlDB;

/**
 *
 */
public class ShowLocalVariableActionDelegate implements IViewActionDelegate, org.eclipse.ui.IActionDelegate2{

	static private IAction fAction;

	public ShowLocalVariableActionDelegate() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view) {
		}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
	 */
	public void init(IAction action) {
		fAction = action;
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction, org.eclipse.swt.widgets.Event)
	 */
	public void runWithEvent(IAction action, Event event) {
		PerlDB.updateVariableView();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * Returns the value of this filters preference (on/off) for the given
	 * view.
	 *
	 * @param part
	 * @return boolean
	 */
	static public  boolean getPreferenceValue() {
		if( fAction == null) return (true);
		return( fAction.isChecked() );
	}

	
		public void update() {
		}
		

}