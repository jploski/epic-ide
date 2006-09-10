package org.epic.perleditor.views;

import java.io.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.core.util.PerlExecutor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.popupmenus.PopupMessages;
import org.epic.perleditor.editors.PerlImages;

/**
 * @author luelljoc
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PerlDocView extends ViewPart {
	
	
	private Button highlightButton;
	private Button searchPerldocButton;
	private Text highlightText;
	private Text searchPerldocText;
	private Display display;
	private Color highlightColor;
	private Color highlightBackgroundColor;
	private TabFolder tabFolder;
	
	
	private static int ITEM_COUNT = 4;
	private static String[] searchOptions = {"-t -f", "-t -q", "-t", "-m"};
	private static String[] tabItemsLabels = {"Builtin Function", "FAQ", "Module", "Module Source"};
	private boolean[] foundItems = {false, false, false, false};
	private SourceViewer[] sourceViewers = {null, null, null, null};
	private TabItem[] tabItems = {null, null, null, null};
	//private IDocumentPartitioner partitioner;
	

	public PerlDocView() {
	
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		 display = parent.getDisplay();
		 highlightColor = new Color(display, 255, 127, 0);
		 highlightBackgroundColor = new Color(display, 255, 255, 255);
		 
		
		GridLayout gridLayout = new GridLayout(); 
		gridLayout.numColumns = 6;
		parent.setLayout(gridLayout);
		
		GridData gridData;
		
		new Label(parent, SWT.NULL).setText("Search:");
		
		searchPerldocText = new Text(parent, SWT.BORDER);        
		gridData = new GridData();
		gridData.widthHint = 100;		
		searchPerldocText.setLayoutData(gridData);
		
		searchPerldocButton = new Button(parent, SWT.PUSH | SWT.FLAT);
		searchPerldocButton.setImage(PerlImages.ICON_SEARCH.createImage());
		searchPerldocButton.setToolTipText("Search Perldoc");
		
		gridData = new GridData();
		gridData.horizontalIndent = 20;
		Label highlightLabel = new Label(parent, SWT.NULL);
		highlightLabel.setText("Highlight:");
		highlightLabel.setLayoutData(gridData);
		
		highlightText = new Text(parent, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 100;		
		highlightText.setLayoutData(gridData);
		
		highlightButton = new Button(parent, SWT.PUSH | SWT.FLAT);
		highlightButton.setImage(PerlImages.ICON_MARK_OCCURRENCES.createImage());
		highlightButton.setToolTipText("Highlight Text");
        
        tabFolder = new TabFolder(parent, SWT.BORDER);
        // Inititalize SourceViewers
        for(int i = 0; i < sourceViewers.length; i++) {
            sourceViewers[i] = new SourceViewer(tabFolder, null, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            sourceViewers[i].setEditable(false);
        }
		
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 6;
		
		tabFolder.setLayoutData(gridData);
		
		parent.pack();
		
		Listener listener = new Listener() {
	        public void handleEvent(Event event) {
	          Widget item = event.widget;
	          if (item == highlightButton) {
	            highlightText();
	          }
	          else if(item == searchPerldocButton) {
	          	search();
	          }
	          
	        }
	      };
	      
	      KeyAdapter keyListener = new KeyAdapter() {
	      	  public void keyPressed(KeyEvent event) {
	      	  	Widget item = event.widget;
	      	  	
	      	  	switch(event.keyCode) {
	      	  		case 13:
	      	  				if(item == searchPerldocText) {
	      	  					search();
	      	  				}
	      	  				else if(item == highlightText) {
	      	  			    highlightText();
	      	  				}
	      	  				break;
	      	  	}
	      	  }
	      	
	      };
	      
	      searchPerldocButton.addListener(SWT.Selection, listener);
	      searchPerldocText.addKeyListener(keyListener);
	      highlightButton.addListener(SWT.Selection, listener);
	      highlightText.addKeyListener(keyListener);
	}
    
    /**
     * For test purposes only.
     */
    public String getDisplayedText(int tabNo)
    {
        return sourceViewers[tabNo].getTextWidget().getText();
    }
	
	public void search() {
        try { search(searchPerldocText.getText()); }
        catch (CoreException e)
        {
            PerlEditorPlugin.getDefault().getLog().log(e.getStatus());
        }
	}
	
	public void search(String searchText) throws CoreException {
		search(searchText, null);
	}
	
	public void search(String searchText, ITextEditor textEditor) throws CoreException {
		
		if(searchText.trim().length() == 0) {
			return;
		}
		
		if(!searchPerldocText.getText().equals(searchText)) {
			searchPerldocText.setText(searchText);
		}
		
		// Set search string as highlight string if highlight input
		// field is empty
		if(highlightText.getText().length() == 0) {
			highlightText.setText(searchText);
		}
		
		// Search PerlDoc
		int itemsFound = 0;
		for(int i = 0; i < ITEM_COUNT; i++) {
			String result = getPerlDoc(searchOptions[i], searchText, textEditor);
			sourceViewers[i].setDocument(new Document(result));
			foundItems[i] = result.length() > 0 ? true : false;
			itemsFound += result.length() > 0 ? 1 : 0;
		}
	
		
		// Delete all tab items
		for(int i = 0; i < ITEM_COUNT; i++) {
			if(tabItems[i] != null) {
				tabItems[i].dispose();
				tabItems[i] = null;
			}
		}
		
		// Show tab items
		for(int i = 0; i < ITEM_COUNT; i++) {
			if(foundItems[i]) {
				// Create new tab item
				tabItems[i] = new TabItem(tabFolder, SWT.NULL);
				tabItems[i].setText(tabItemsLabels[i]);
				tabItems[i].setControl(sourceViewers[i].getControl());
			}
			
		}
		
		// Set focus on first tab
		for(int i = 0; i < ITEM_COUNT; i++) {
			if(foundItems[i]) {
				tabItems[i].getControl().setFocus();
				break;
			}
		}
		
		// Highlight text
		highlightText();
		
		// If nothing has been found, display info dialog
		if(itemsFound == 0) {
			MessageDialog.openInformation(
					display.getActiveShell(),
					PopupMessages.getString("NoDocumentation.title"),
					PopupMessages.getString("NoDocumentation.message"));
		}
	}
	
	private String getPerlDoc(String option, String searchText, ITextEditor textEditor) throws CoreException {

		String perlCode =
			"use Env qw(@PERL5LIB);\n\n"
				+ "splice(@PERL5LIB, 0, 0, @INC);\n"
				+ "exec('perldoc "
				+ option
				+ " \""
				+ searchText
				+ "\"');";
        
        PerlExecutor executor = new PerlExecutor();
        try
        {
        	// If the PerlDoc search method is not called from an editor the textEditor object is null.
        	// In this case the execute method is called with the current directory as argument.
        	if(textEditor != null) {
        		return executor.execute(textEditor, null, perlCode).stdout;
        	}
        	else {
        		try {
					return executor.execute(new File(new File(".").getCanonicalPath()), null, perlCode).stdout;
				} catch (Exception e) {
					e.printStackTrace();
					return "";
				}
        	}
        }
        finally { executor.dispose(); }
	}
	
	/**
	 * Highlights text in all SourceViewers
	 */
	private void highlightText() {
		for(int i = 0; i < ITEM_COUNT; i++) {
			if(foundItems[i]) {
				highlightText(sourceViewers[i]);
			}
		}
	}
	
	private void highlightText(SourceViewer sourceViewer) {
		StyleRange styleRange;
		
		IDocument document = sourceViewer.getDocument();
		
		// Reset style
		styleRange = new StyleRange(0, document.getLength(), null, null, SWT.NORMAL);            
		sourceViewer.getTextWidget().setStyleRange(styleRange);
		
		String searchText = highlightText.getText();
		if(searchText.trim().length() > 0) {
			FindReplaceDocumentAdapter findAdapter = new FindReplaceDocumentAdapter(document);
			try {
				IRegion findResult;
				int offset = 0;
				
				while ((findResult = findAdapter.find(offset, searchText, true, false, false, false))!= null) {
					int startPos = findResult.getOffset();
					int endPos = startPos + findResult.getLength();
					
					styleRange = new StyleRange();
			        styleRange.start = startPos;
			        styleRange.length = findResult.getLength();
			        styleRange.foreground = highlightBackgroundColor;
			        //styleRange.fontStyle = SWT.BOLD;
			        styleRange.background = highlightColor;
			        sourceViewer.getTextWidget().setStyleRange(styleRange);

			        offset = endPos + 1;
					
				}
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		// TODO Auto-generated method stub
	}

}