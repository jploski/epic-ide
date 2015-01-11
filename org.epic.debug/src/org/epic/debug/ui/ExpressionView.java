/*
 * Created on Jan 31, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug.ui;

import gnu.regexp.RE;
import gnu.regexp.RESyntax;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.epic.debug.*;
import org.epic.debug.db.*;

 
		

/**
 * @author luelljoc
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ExpressionView extends ViewPart {
	
	static RE mReIsWhitespace;
	private int mCommandCount;
	
	SashForm sashForm;
	Text expressionInput, expressionOutput;
	Action evaluateAction;
	
	
	public  ExpressionView()
	{
		try{
			 mReIsWhitespace = new RE("^\\s*$",0, RESyntax.RE_SYNTAX_PERL5);
			}catch(Exception e) {System.out.println(e);}
		mCommandCount = 1;
			
	}
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
	 * Action called, when Evaluate expression button is pressed
	 */
	private void evaluateExpression()
	{
		IAdaptable a = DebugUITools.getDebugContext();
		StackFrame stackFrame = a != null ? (StackFrame) a.getAdapter(StackFrame.class) : null;
		if (stackFrame != null)
		{
			try
            {
    			PerlDebugThread thread = stackFrame.getPerlThread();
                // TODO: this really should not run on the GUI thread
    			String res = thread.evaluateStatement(expressionInput.getText()); 
    			boolean isMatch = false;
    			if( res != null)
    			{
    				isMatch = mReIsWhitespace.isMatch(res) || 
    			   				mReIsWhitespace.getAllMatches(res).length > 0;
    			}
    						   				
    			if( res == null || res.length() == 0 || isMatch == true)			
    			{
    				res =  "\n<Command("+mCommandCount+") finished>\n";
    				
    			}
    			else
    			{
    				res = res + "\n<Command("+mCommandCount+") finished>\n";
    			}
    			  
    			setExpressionOutput(res);
    			mCommandCount++;
			}
            catch( Exception e)
            {
                PerlDebugPlugin.log(e);
            }
		}
		else
        {
			MessageDialog.openInformation(
                this.getViewSite().getShell(),
				"No stack frame selected",
				"Before evaluating a Perl expression, first select a stack frame\n" +
                "from a suspended debugger session in the Debug view to indicate\n" +
                "the evaluation context.\n\n");
        }
	}
	
	private void setExpressionOutput(String text) {
		this.expressionOutput.setText(text);
	}

}
