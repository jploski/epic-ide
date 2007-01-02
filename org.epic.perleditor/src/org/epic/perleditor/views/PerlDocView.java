package org.epic.perleditor.views;

import java.io.*;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.*;
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
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.epic.core.util.PerlExecutor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.popupmenus.PopupMessages;
import org.epic.perleditor.PerlPluginImages;

/**
 * @author luelljoc
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PerlDocView extends ViewPart {
	
	
	private Button searchPerldocButton;
	private Text searchPerldocText;
	private Display display;
	private TabFolder tabFolder;
    private FindReplaceAction findReplaceAction;
	
	
	private static int ITEM_COUNT = 4;
	private static String[] searchOptions = {"-t -f", "-t -q", "-t", "-m"};
	private static String[] tabItemsLabels = {"Builtin Function", "FAQ", "Module", "Module Source"};
	private boolean[] foundItems = {false, false, false, false};
	private SourceViewer[] sourceViewers = {null, null, null, null};
	private TabItem[] tabItems = {null, null, null, null};
	private final FindReplaceTarget findReplaceTarget = new FindReplaceTarget();

    private final IPropertyChangeListener fontPropertyChangeListener =
        new IPropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event)
        {
            if (event.getProperty().equals(getFontPropertyPreferenceKey()))
            {                
                Font textFont = JFaceResources.getFontRegistry().get(
                    getFontPropertyPreferenceKey());

                for (int i = 0; i < sourceViewers.length; i++)
                    sourceViewers[i].getTextWidget().setFont(textFont);
            }
        } };

	public PerlDocView() {
        JFaceResources.getFontRegistry().addListener(fontPropertyChangeListener);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
	    display = parent.getDisplay();

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
		searchPerldocButton.setImage(PerlPluginImages.get(PerlPluginImages.IMG_ICON_SEARCH));
		searchPerldocButton.setToolTipText("Search Perldoc");
		
		gridData = new GridData();
		gridData.horizontalIndent = 20;

        Font textFont = JFaceResources.getFontRegistry().get(
            getFontPropertyPreferenceKey());
        
        tabFolder = new TabFolder(parent, SWT.BORDER);
        // Inititalize SourceViewers
        for(int i = 0; i < sourceViewers.length; i++) {
            sourceViewers[i] = new SourceViewer(tabFolder, null, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            sourceViewers[i].setEditable(false);
            sourceViewers[i].getTextWidget().setFont(textFont);
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
	          if(item == searchPerldocButton) {
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
      	  		    break;
                }
      	    } };

        searchPerldocButton.addListener(SWT.Selection, listener);
        searchPerldocText.addKeyListener(keyListener);
      
        createActions();
	}
    
    public void dispose()
    {
        PerlEditorPlugin.getDefault().getPreferenceStore()
            .removePropertyChangeListener(fontPropertyChangeListener);
        super.dispose();
    }
    
    public Object getAdapter(Class required)
    {
        if (IFindReplaceTarget.class.equals(required))
        {
            return findReplaceTarget;
        }
        return null;
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
		
		// If nothing has been found, display info dialog
		if(itemsFound == 0) {
			MessageDialog.openInformation(
					display.getActiveShell(),
					PopupMessages.getString("NoDocumentation.title"),
					PopupMessages.getString("NoDocumentation.message"));
		}
        
        findReplaceAction.update();
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

	public void setFocus()
    {
	}
    
    private void createActions()
    {
        IActionBars actionBars = ((IViewSite) getSite()).getActionBars();
        ResourceBundle bundle = PerlDocViewMessages.getBundle();

        findReplaceAction =
            new FindReplaceAction(bundle, "find_replace_action_", this);
        actionBars.setGlobalActionHandler(
            ActionFactory.FIND.getId(),
            findReplaceAction);

        actionBars.updateActionBars();
    }
    
    private String getFontPropertyPreferenceKey()
    {
        return JFaceResources.TEXT_FONT;   
    }
    
    private class FindReplaceTarget implements IFindReplaceTarget
    {
        private IFindReplaceTarget getActiveTarget()
        {
            int selectedTab = tabFolder.getSelectionIndex();
            if (selectedTab < 0) return null;
            
            TabItem[] selection = tabFolder.getSelection();
            for (int i = 0; i < sourceViewers.length; i++)
            {
                if (selection[0].getControl() == sourceViewers[i].getControl())
                    return sourceViewers[i].getFindReplaceTarget();
            }
            return null;
        }

        public boolean canPerformFind()
        {
            if (getActiveTarget() == null) return false;
            return getActiveTarget().canPerformFind();
        }

        public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord)
        {
            return getActiveTarget().findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord);
        }

        public Point getSelection()
        {
            return getActiveTarget().getSelection();
        }

        public String getSelectionText()
        {
            return getActiveTarget().getSelectionText();
        }

        public boolean isEditable()
        {
            return false;
        }

        public void replaceSelection(String text)
        {
            getActiveTarget().replaceSelection(text);
        }
    }
}