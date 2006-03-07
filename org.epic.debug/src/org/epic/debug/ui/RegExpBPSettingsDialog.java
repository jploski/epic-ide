/*
 * Created on 07.01.2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epic.debug.PerlRegExpBreakpoint;

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RegExpBPSettingsDialog extends Dialog {

	 PerlRegExpBreakpoint mBP;
	private Label fSourceLabel;
	private Text fSourceText;
	private Text fRegExpText;
	private Text fMatchText;
	private String mTitle;
	private Button fIgnoreCase;
	private Button fMultiLine;
	/**
	 * @param parentShell
	 */
	public RegExpBPSettingsDialog(Shell parentShell, PerlRegExpBreakpoint fBP) {
		super(parentShell);
		mBP = fBP;
		mTitle = "Regular Expression Breakpoint Properties";
		// TODO Auto-generated constructor stub
	}

	public RegExpBPSettingsDialog(Shell parentShell, PerlRegExpBreakpoint fBP, String fTitle) {
		super(parentShell);
		mBP = fBP;
		mTitle = fTitle;
		// TODO Auto-generated constructor stub
	}
	 protected Control createDialogArea(Composite parent) {
	    String val;  
	 	Composite comp = (Composite)super.createDialogArea(parent);
	    
			GridLayout topLayout = new GridLayout();
			comp.setLayout(topLayout);
			GridData gd;
			fSourceText = createLine(comp,"Source Line:",false,mBP.getSourceLine());	
			fRegExpText = createLine(comp,"Regular Expression:",true,mBP.getRegExp());
			fMatchText = createLine(comp,"Term to Match:",true,mBP.getMatchText());
			fMultiLine = createBoolen(comp,"Multi Line",true,mBP.getMultiLine()); 
			fIgnoreCase = createBoolen(comp,"Ignore Case",true,mBP.getIgnoreCase());
			return(comp);
			
			
			
	 }
	 
	   protected void configureShell(Shell newShell) {
	      super.configureShell(newShell);
	      newShell.setText(mTitle);
	     }

	   Button  createBoolen(Composite fComp,String fText, boolean fWrite, boolean fVal)
	   {
	   			Composite comp = new Composite(fComp, SWT.NONE);
	   			GridLayout sourceLayout = new GridLayout();
	   			sourceLayout.numColumns = 3;
	   			sourceLayout.marginHeight = 0;
	   			sourceLayout.marginWidth = 0;
	   			sourceLayout.makeColumnsEqualWidth = true;
	   			comp.setLayout(sourceLayout);
	   			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	   			comp.setLayoutData(gd);
	   	

	   			fSourceLabel = new Label(comp, SWT.NONE);
	   			fSourceLabel.setText(fText); //$NON-NLS-1$
	   			gd = new GridData();
	   			gd.horizontalSpan = 1;
	   			fSourceLabel.setLayoutData(gd);
	   			

	   			Button checkBox = new Button(fComp, SWT.CHECK | SWT.CENTER);
gd = new GridData(GridData.FILL_HORIZONTAL);
gd.horizontalSpan = 1;
checkBox.setLayoutData(gd);

checkBox.setSelection(fVal);
checkBox.setEnabled(fWrite);

	   	       return checkBox;
	   	   }
	   
	   
	   Text  createLine(Composite fComp,String fText, boolean fWrite, String fVal)
	   {
	   			Composite comp = new Composite(fComp, SWT.NONE);
	   			GridLayout sourceLayout = new GridLayout();
	   			sourceLayout.numColumns = 3;
	   			sourceLayout.marginHeight = 0;
	   			sourceLayout.marginWidth = 0;
	   			sourceLayout.makeColumnsEqualWidth = true;
	   			comp.setLayout(sourceLayout);
	   			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	   			comp.setLayoutData(gd);
	   	

	   			fSourceLabel = new Label(comp, SWT.NONE);
	   			fSourceLabel.setText(fText); //$NON-NLS-1$
	   			gd = new GridData();
	   			gd.horizontalSpan = 1;
	   			fSourceLabel.setLayoutData(gd);
	   			

	   			Text text = new Text(fComp, SWT.SINGLE | SWT.BORDER);
	   			gd = new GridData(GridData.FILL_HORIZONTAL);
	   			gd.horizontalSpan = 2;
	   			text.setLayoutData(gd);
	   			text.setEditable(fWrite);
	   			
	   			if( fVal != null )
	   				text.setText(fVal);
	   	       return text;
	   	   }
	   protected void okPressed()
	   {
	   	mBP.setRegExp(fRegExpText.getText());
	   	mBP.setMatchText(fMatchText.getText());
	   	mBP.setMultiLine(fMultiLine.getSelection());
	   	mBP.setIgnoreCase(fIgnoreCase.getSelection());
	   super.okPressed();
	   }
	   
}
