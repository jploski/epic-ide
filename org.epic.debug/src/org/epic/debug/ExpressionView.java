/*
 * Created on Jan 31, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

/**
 * @author luelljoc
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ExpressionView extends ViewPart {

	SashForm sashForm;
	Text expressionInput, expressionOutput;
	Action evaluateAction;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		makeActions();
		contributeToActionBars();

		sashForm = new SashForm(parent, SWT.VERTICAL);
		expressionInput =
			new Text(sashForm, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		expressionInput.setEditable(true);

		expressionOutput =
			new Text(sashForm, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		expressionOutput.setEditable(false);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		expressionInput.setFocus();
	}
	
	private void contributeToActionBars() {
			IActionBars bars = getViewSite().getActionBars();
			fillLocalToolBar(bars.getToolBarManager());
		}

		private void fillLocalToolBar(IToolBarManager manager) {
			manager.add(evaluateAction);
		}

	private void makeActions() {
		//		Validation action
		evaluateAction = new Action() {
			public void run() {
				evaluateExpression();
			}
		};
		evaluateAction.setText("Evaluate Expression");
		evaluateAction.setToolTipText("Evaluate Expression");
		evaluateAction.setImageDescriptor(
			PerlDebugImages.ACTION_EXPRESSION_EVALUATE);
	}
	
	/**
	 * Action called, when Evaluate Expressin button is pressed
	 */
	private void evaluateExpression() {
		MessageDialog.openInformation(
						this.getViewSite().getShell(),
						"Message title",
						"Expression to evaluate:\n" + this.expressionInput.getText());
	}
	
	private void setExpressionOutput(String text) {
		this.expressionOutput.setText(text);
	}

}
