/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.epic.perleditor.preferences;


import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlPartitioner;
import org.epic.perleditor.editors.PerlSourceViewerConfiguration;

/*
 * The page for setting the editor options.
 */
public class PerlEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private static final String BOLD= PreferenceConstants.EDITOR_BOLD_SUFFIX;
	

	public final OverlayPreferenceStore.OverlayKey[] fKeys= new OverlayPreferenceStore.OverlayKey[] {
		
        //-------------------------------------
		// Appearance Tab
        //-------------------------------------
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PreferenceConstants.EDITOR_TAB_WIDTH),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PreferenceConstants.INSERT_TABS_ON_INDENT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.SPACES_INSTEAD_OF_TABS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_LINE_NUMBER_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_LINE_NUMBER_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_CURRENT_LINE_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CURRENT_LINE),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_PRINT_MARGIN),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_OVERVIEW_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.SOURCE_FOLDING),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.AUTO_COMPLETION_QUOTE1),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.AUTO_COMPLETION_QUOTE2),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.AUTO_COMPLETION_BRACKET1),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.AUTO_COMPLETION_BRACKET2),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.AUTO_COMPLETION_BRACKET3),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.AUTO_COMPLETION_BRACKET4),
        new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_HOME_END),
        new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION),
		//-------------------------------------
		
        //-------------------------------------
		// Syntax Tab
		//-------------------------------------
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOREGROUND_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BACKGROUND_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_STRING_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_STRING_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_KEYWORD1_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_KEYWORD1_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_KEYWORD2_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_KEYWORD2_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_VARIABLE_COLOR),
	    new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_VARIABLE_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_COMMENT1_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_COMMENT1_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_COMMENT2_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_COMMENT2_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LITERAL1_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_LITERAL1_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LITERAL2_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_LITERAL2_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LABEL_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_LABEL_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FUNCTION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FUNCTION_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_MARKUP_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_MARKUP_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_OPERATOR_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_OPERATOR_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_NUMBER_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_NUMBER_COLOR_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_INVALID_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_INVALID_COLOR_BOLD),
		
		
		
		
	
        //-------------------------------------
        
        //-------------------------------------
		// Annotations Tab
	    //-------------------------------------
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_PROBLEM_INDICATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WARNING_INDICATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_TASK_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_TASK_INDICATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BOOKMARK_INDICATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_UNKNOWN_INDICATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER),
        //-------------------------------------

	};
	
	private final String[][] fSyntaxColorListModel= new String[][] {
		{ PreferencesMessages.getString("PerlEditorPreferencePage.nullColor"), PreferenceConstants.EDITOR_FOREGROUND_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.keyword1Color"), PreferenceConstants.EDITOR_KEYWORD1_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.keyword2Color"), PreferenceConstants.EDITOR_KEYWORD2_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.variableColor"), PreferenceConstants.EDITOR_VARIABLE_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.comment1Color"), PreferenceConstants.EDITOR_COMMENT1_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.comment2Color"), PreferenceConstants.EDITOR_COMMENT2_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.literal1Color"), PreferenceConstants.EDITOR_LITERAL1_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.literal2Color"), PreferenceConstants.EDITOR_LITERAL2_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.labelColor"), PreferenceConstants.EDITOR_LABEL_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.functionColor"), PreferenceConstants.EDITOR_FUNCTION_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.markupColor"), PreferenceConstants.EDITOR_MARKUP_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.operatorColor"), PreferenceConstants.EDITOR_OPERATOR_COLOR},
		{ PreferencesMessages.getString("PerlEditorPreferencePage.numberColor"), PreferenceConstants.EDITOR_NUMBER_COLOR},
	    { PreferencesMessages.getString("PerlEditorPreferencePage.invalidColor"), PreferenceConstants.EDITOR_INVALID_COLOR},
	};
	
	private final String[][] fAppearanceColorListModel= new String[][] {
		{PreferencesMessages.getString("PerlEditorPreferencePage.lineNumberForegroundColor"), PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR}, //$NON-NLS-1$
		{PreferencesMessages.getString("PerlEditorPreferencePage.currentLineHighlighColor"), PreferenceConstants.EDITOR_CURRENT_LINE_COLOR}, //$NON-NLS-1$
		{PreferencesMessages.getString("PerlEditorPreferencePage.printMarginColor2"), PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR}, //$NON-NLS-1$
	};
	
	private final String[][] fAnnotationColorListModel= new String[][] {
		{PreferencesMessages.getString("PerlEditorPreferencePage.annotations.errors"), PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR, PreferenceConstants.EDITOR_PROBLEM_INDICATION, PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER }, //$NON-NLS-1$
		{PreferencesMessages.getString("PerlEditorPreferencePage.annotations.warnings"), PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR, PreferenceConstants.EDITOR_WARNING_INDICATION, PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER }, //$NON-NLS-1$
		{PreferencesMessages.getString("PerlEditorPreferencePage.annotations.tasks"), PreferenceConstants.EDITOR_TASK_INDICATION_COLOR, PreferenceConstants.EDITOR_TASK_INDICATION, PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER }, //$NON-NLS-1$
		{PreferencesMessages.getString("PerlEditorPreferencePage.annotations.searchResults"), PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER }, //$NON-NLS-1$
		{PreferencesMessages.getString("PerlEditorPreferencePage.annotations.bookmarks"), PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR, PreferenceConstants.EDITOR_BOOKMARK_INDICATION, PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER }, //$NON-NLS-1$
		{PreferencesMessages.getString("PerlEditorPreferencePage.annotations.others"), PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR, PreferenceConstants.EDITOR_UNKNOWN_INDICATION, PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER } //$NON-NLS-1$
	};


	private OverlayPreferenceStore fOverlayStore;
	
	private Map fColorButtons= new HashMap();
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	private Map fTextFields= new HashMap();
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			Text text= (Text) e.widget;
			fOverlayStore.setValue((String) fTextFields.get(text), text.getText());
		}
	};

	private ArrayList fNumberFields= new ArrayList();
	private ModifyListener fNumberFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			numberFieldChanged((Text) e.widget);
		}
	};
	
	private List fSyntaxColorList;
	private List fAppearanceColorList;
	private List fAnnotationList;
	private ColorEditor fSyntaxForegroundColorEditor;
	private ColorEditor fAppearanceColorEditor;
	private ColorEditor fAnnotationForegroundColorEditor;
	private ColorEditor fBackgroundColorEditor;
	private Button fBackgroundDefaultRadioButton;
    private Button fBackgroundCustomRadioButton;
	private Button fBackgroundColorButton;
	private Button fBoldCheckBox;
	private SourceViewer fPreviewViewer;
	private Color fBackgroundColor;
	private Button fShowInTextCheckBox;
	private Button fShowInOverviewRulerCheckBox;
	private Button fCompletionInsertsRadioButton;
	private Button fCompletionOverwritesRadioButton;
	
	public PerlEditorPreferencePage() {
		setDescription(PreferencesMessages.getString("PerlEditorPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), fKeys);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init()
	 */	
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
	}

	private void handleSyntaxColorListSelection() {	
		int i= fSyntaxColorList.getSelectionIndex();
		String key= fSyntaxColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fSyntaxForegroundColorEditor.setColorValue(rgb);		
		fBoldCheckBox.setSelection(fOverlayStore.getBoolean(key + BOLD));
	}

	private void handleAppearanceColorListSelection() {	
		int i= fAppearanceColorList.getSelectionIndex();
		String key= fAppearanceColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fAppearanceColorEditor.setColorValue(rgb);		
	}

	private void handleAnnotationListSelection() {
		int i= fAnnotationList.getSelectionIndex();
		
		String key= fAnnotationColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fAnnotationForegroundColorEditor.setColorValue(rgb);
		
		key= fAnnotationColorListModel[i][2];
		fShowInTextCheckBox.setSelection(fOverlayStore.getBoolean(key));
		
		key= fAnnotationColorListModel[i][3];
		fShowInOverviewRulerCheckBox.setSelection(fOverlayStore.getBoolean(key));				
	}
	
	private Control createSyntaxPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NULL);
		colorComposite.setLayout(new GridLayout());

		Group backgroundComposite= new Group(colorComposite, SWT.SHADOW_ETCHED_IN);
		backgroundComposite.setLayout(new RowLayout());
		backgroundComposite.setText(PreferencesMessages.getString("PerlEditorPreferencePage.backgroundColor"));//$NON-NLS-1$
	
		SelectionListener backgroundSelectionListener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				boolean custom= fBackgroundCustomRadioButton.getSelection();
				fBackgroundColorButton.setEnabled(custom);
				fOverlayStore.setValue(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR, !custom);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};

		fBackgroundDefaultRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundDefaultRadioButton.setText(PreferencesMessages.getString("PerlEditorPreferencePage.systemDefault")); //$NON-NLS-1$
		fBackgroundDefaultRadioButton.addSelectionListener(backgroundSelectionListener);

		fBackgroundCustomRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundCustomRadioButton.setText(PreferencesMessages.getString("PerlEditorPreferencePage.custom")); //$NON-NLS-1$
		fBackgroundCustomRadioButton.addSelectionListener(backgroundSelectionListener);

		fBackgroundColorEditor= new ColorEditor(backgroundComposite);
		fBackgroundColorButton= fBackgroundColorEditor.getButton();

		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PerlEditorPreferencePage.foreground")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);		

		fSyntaxColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fSyntaxColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PerlEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fSyntaxForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fSyntaxForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);
		
		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
		fBoldCheckBox.setText(PreferencesMessages.getString("PerlEditorPreferencePage.bold")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fBoldCheckBox.setLayoutData(gd);
		
		label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PerlEditorPreferencePage.preview")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control previewer= createPreviewer(colorComposite);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(20);
		gd.heightHint= convertHeightInCharsToPixels(5);
		previewer.setLayoutData(gd);

		
		fSyntaxColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleSyntaxColorListSelection();
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fSyntaxColorList.getSelectionIndex();
				String key= fSyntaxColorListModel[i][1];
				
				PreferenceConverter.setValue(fOverlayStore, key, fSyntaxForegroundColorEditor.getColorValue());
			}
		});

		fBackgroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				PreferenceConverter.setValue(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR, fBackgroundColorEditor.getColorValue());					
			}
		});

		fBoldCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fSyntaxColorList.getSelectionIndex();
				String key= fSyntaxColorListModel[i][1];
				fOverlayStore.setValue(key + BOLD, fBoldCheckBox.getSelection());
			}
		});
				
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		fPreviewViewer= new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);		
		fPreviewViewer.configure(new PerlSourceViewerConfiguration(fOverlayStore, null));
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		fPreviewViewer.setEditable(false);
		
		initializeViewerColors(fPreviewViewer);
		
		String content= loadPreviewContentFromFile("ColorSettingPreviewCode.txt"); //$NON-NLS-1$

		IDocument document= new Document(content);
		IDocumentPartitioner partitioner =
            new PerlPartitioner(PerlEditorPlugin.getDefault().getLog()); 
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
	
		fPreviewViewer.setDocument(document);
		
		fOverlayStore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String p= event.getProperty();
				if (p.equals(PreferenceConstants.EDITOR_BACKGROUND_COLOR) ||
					p.equals(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR))
				{
					initializeViewerColors(fPreviewViewer);
				}

				int topIndex =  fPreviewViewer.getTextWidget().getTopIndex();
		        int caretOffset = fPreviewViewer.getTextWidget().getCaretOffset();
				fPreviewViewer.unconfigure();
                fPreviewViewer.configure(new PerlSourceViewerConfiguration(fOverlayStore, null));
				
				// Set editor foreground
				fPreviewViewer.getTextWidget().setForeground(
                    PerlEditorPlugin.getDefault().getColor(PreferenceConstants.EDITOR_STRING_COLOR));
				
				fPreviewViewer.refresh();
				fPreviewViewer.getTextWidget().setTopIndex(topIndex);
				fPreviewViewer.getTextWidget().setCaretOffset(caretOffset);				
				fPreviewViewer.invalidateTextPresentation();
			}
		});
		
		return fPreviewViewer.getControl();
	}

	
	/**
	 * Initializes the given viewer's colors.
	 * 
	 * @param viewer the viewer to be initialized
	 */
	private void initializeViewerColors(ISourceViewer viewer) {
		
		IPreferenceStore store= fOverlayStore;
		if (store != null) {
			
			StyledText styledText= viewer.getTextWidget();
						
			// ---------- background color ----------------------
			Color color= store.getBoolean(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR)
				? null
				: createColor(store, PreferenceConstants.EDITOR_BACKGROUND_COLOR, styledText.getDisplay());
			styledText.setBackground(color);
				
			if (fBackgroundColor != null)
				fBackgroundColor.dispose();
				
			fBackgroundColor= color;
		}
	}

	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 */
	private Color createColor(IPreferenceStore store, String key, Display display) {
	
		RGB rgb= null;		
		
		if (store.contains(key)) {
			
			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);
		
			if (rgb != null)
				return new Color(display, rgb);
		}
		
		return null;
	}	
	
	private Control createAppearancePage(Composite parent) {

		Composite appearanceComposite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		appearanceComposite.setLayout(layout);

		String label= PreferencesMessages.getString("PerlEditorPreferencePage.displayedTabWidth"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, PreferenceConstants.EDITOR_TAB_WIDTH, 3, 0, true);
		
		label= PreferencesMessages.getString("PerlEditorPreferencePage.insertTabsOnIndent"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, PreferenceConstants.INSERT_TABS_ON_INDENT, 3, 0, true);

		label= PreferencesMessages.getString("PerlEditorPreferencePage.printMarginColumn"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, 3, 0, true);
		
		label= PreferencesMessages.getString("PerlEditorPreferencePage.spacesInsteadOfTabs");
				addCheckBox(appearanceComposite, label, PreferenceConstants.SPACES_INSTEAD_OF_TABS, 0);
				
		//label= PreferencesMessages.getString("PerlEditorPreferencePage.synchronizeOnCursor"); //$NON-NLS-1$
		//addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, 0);

		label= PreferencesMessages.getString("PerlEditorPreferencePage.showOverviewRuler"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_OVERVIEW_RULER, 0);
				
		label= PreferencesMessages.getString("PerlEditorPreferencePage.showLineNumbers"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_LINE_NUMBER_RULER, 0);

		//label= PreferencesMessages.getString("PerlEditorPreferencePage.highlightMatchingBrackets"); //$NON-NLS-1$
		//addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_MATCHING_BRACKETS, 0);
		
		label= PreferencesMessages.getString("PerlEditorPreferencePage.highlightCurrentLine"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_CURRENT_LINE, 0);
				
		label= PreferencesMessages.getString("PerlEditorPreferencePage.showPrintMargin"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_PRINT_MARGIN, 0);
		
		label= PreferencesMessages.getString("PerlEditorPreferencePage.sourceFolding"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.SOURCE_FOLDING, 0);
		
		Label l= new Label(appearanceComposite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
		
		l= new Label(appearanceComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("PerlEditorPreferencePage.appearanceOptions")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		l.setLayoutData(gd);

		Composite editorComposite= new Composite(appearanceComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		

		fAppearanceColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(8);
		fAppearanceColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l= new Label(stylesComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("PerlEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		l.setLayoutData(gd);

		fAppearanceColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fAppearanceColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		fAppearanceColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleAppearanceColorListSelection();
			}
		});
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fAppearanceColorList.getSelectionIndex();
				String key= fAppearanceColorListModel[i][1];
				
				PreferenceConverter.setValue(fOverlayStore, key, fAppearanceColorEditor.getColorValue());
			}
		});
		return appearanceComposite;
	}
	
	
	private Control createAnnotationsPage(Composite parent) {
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		composite.setLayout(layout);
		
		addFiller(composite);
				
		Label label= new Label(composite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PerlEditorPreferencePage.annotationPresentationOptions")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);

		Composite editorComposite= new Composite(composite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		

		fAnnotationList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(8);
		fAnnotationList.setLayoutData(gd);
						
		Composite optionsComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		optionsComposite.setLayout(layout);
		optionsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fShowInTextCheckBox= new Button(optionsComposite, SWT.CHECK);
		fShowInTextCheckBox.setText(PreferencesMessages.getString("PerlEditorPreferencePage.annotations.showInText")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fShowInTextCheckBox.setLayoutData(gd);
		
		fShowInOverviewRulerCheckBox= new Button(optionsComposite, SWT.CHECK);
		fShowInOverviewRulerCheckBox.setText(PreferencesMessages.getString("PerlEditorPreferencePage.annotations.showInOverviewRuler")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fShowInOverviewRulerCheckBox.setLayoutData(gd);
		
		label= new Label(optionsComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PerlEditorPreferencePage.annotations.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fAnnotationForegroundColorEditor= new ColorEditor(optionsComposite);
		Button foregroundColorButton= fAnnotationForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		fAnnotationList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				handleAnnotationListSelection();
			}
		});
		
		fShowInTextCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				int i= fAnnotationList.getSelectionIndex();
				String key= fAnnotationColorListModel[i][2];
				fOverlayStore.setValue(key, fShowInTextCheckBox.getSelection());
			}
		});
		
		fShowInOverviewRulerCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				int i= fAnnotationList.getSelectionIndex();
				String key= fAnnotationColorListModel[i][3];
				fOverlayStore.setValue(key, fShowInOverviewRulerCheckBox.getSelection());
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				int i= fAnnotationList.getSelectionIndex();
				String key= fAnnotationColorListModel[i][1];
				PreferenceConverter.setValue(fOverlayStore, key, fAnnotationForegroundColorEditor.getColorValue());
			}
		});
		
		return composite;
	}

	private Control createTypingPage(Composite parent) {
		
		Composite typingComposite= new Composite(parent, SWT.NULL);
		typingComposite.setLayout(new GridLayout());

		Label l= new Label(typingComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("PerlEditorPreferencePage.typing.autoCompletion")); //$NON-NLS-1$

		String label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.autoCompletionQuote1"); //$NON-NLS-1$
		addCheckBox(typingComposite, label, PreferenceConstants.AUTO_COMPLETION_QUOTE1, 0);

		label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.autoCompletionQuote2"); //$NON-NLS-1$
		addCheckBox(typingComposite, label, PreferenceConstants.AUTO_COMPLETION_QUOTE2, 0);

		label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.autoCompletionBracket1"); //$NON-NLS-1$
		addCheckBox(typingComposite, label, PreferenceConstants.AUTO_COMPLETION_BRACKET1, 0);

		label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.autoCompletionBracket2"); //$NON-NLS-1$
		addCheckBox(typingComposite, label, PreferenceConstants.AUTO_COMPLETION_BRACKET2, 0);

		label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.autoCompletionBracket3"); //$NON-NLS-1$
		addCheckBox(typingComposite, label, PreferenceConstants.AUTO_COMPLETION_BRACKET3, 0);

		label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.autoCompletionBracket4"); //$NON-NLS-1$
		addCheckBox(typingComposite, label, PreferenceConstants.AUTO_COMPLETION_BRACKET4, 0);
        
        addFiller(typingComposite);
        
        label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.smartHomeEnd"); //$NON-NLS-1$ 
        addCheckBox(typingComposite, label, PreferenceConstants.EDITOR_SMART_HOME_END, 0);

        label= PreferencesMessages.getString("PerlEditorPreferencePage.typing.subWordNavigation"); //$NON-NLS-1$ 
        addCheckBox(typingComposite, label, PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION, 0);

		return typingComposite;
	}
	
	private void addFiller(Composite composite) {
		Label filler= new Label(composite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		filler.setLayoutData(gd);
	}
	
	private static void indent(Control control) {
		GridData gridData= new GridData();
		gridData.horizontalIndent= 20;
		control.setLayoutData(gridData);		
	}
	
	private static void createDependency(final Button master, final Control slave) {
		indent(slave);
		master.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		});		
	}

	private void addCompletionRadioButtons(Composite contentAssistComposite) {
		Composite completionComposite= new Composite(contentAssistComposite, SWT.NONE);
		GridData ccgd= new GridData();
		ccgd.horizontalSpan= 2;
		completionComposite.setLayoutData(ccgd);
		GridLayout ccgl= new GridLayout();
		ccgl.marginWidth= 0;
		ccgl.numColumns= 2;
		completionComposite.setLayout(ccgl);
		
		SelectionListener completionSelectionListener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {				
				boolean insert= fCompletionInsertsRadioButton.getSelection();
			}
		};
		
		fCompletionInsertsRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionInsertsRadioButton.setText(PreferencesMessages.getString("PerlEditorPreferencePage.completionInserts")); //$NON-NLS-1$
		fCompletionInsertsRadioButton.setLayoutData(new GridData());
		fCompletionInsertsRadioButton.addSelectionListener(completionSelectionListener);
		
		fCompletionOverwritesRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionOverwritesRadioButton.setText(PreferencesMessages.getString("PerlEditorPreferencePage.completionOverwrites")); //$NON-NLS-1$
		fCompletionOverwritesRadioButton.setLayoutData(new GridData());
		fCompletionOverwritesRadioButton.addSelectionListener(completionSelectionListener);
	}

	/**
	 * Computes the state mask for the given modifier string.
	 * 
	 * @param modifiers	the string with the modifiers, separated by '+', '-', ';', ',' or '.'
	 * @return the state mask or -1 if the input is invalid
	 */
	private int computeStateMask(String modifiers) {
		if (modifiers == null)
			return -1;
		
		if (modifiers.length() == 0)
			return SWT.NONE;

		int stateMask= 0;
		StringTokenizer modifierTokenizer= new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
		while (modifierTokenizer.hasMoreTokens()) {
			int modifier= Action.findModifier(modifierTokenizer.nextToken());
			if (modifier == 0 || (stateMask & modifier) == modifier)
				return -1;
			stateMask= stateMask | modifier;
		}
		return stateMask;
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		initializeDefaultColors();
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("PerlEditorPreferencePage.general")); //$NON-NLS-1$
		item.setControl(createAppearancePage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("PerlEditorPreferencePage.colors")); //$NON-NLS-1$
		item.setControl(createSyntaxPage(folder));
		

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("PerlEditorPreferencePage.annotationsTab.title")); //$NON-NLS-1$
		item.setControl(createAnnotationsPage(folder));

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("PerlEditorPreferencePage.typing.title")); //$NON-NLS-1$
		item.setControl(createTypingPage(folder));

		initialize();
		
		Dialog.applyDialogFont(folder);
		return folder;
	}
	
	private void initialize() {
		
		initializeFields();
		
		for (int i= 0; i < fSyntaxColorListModel.length; i++)
			fSyntaxColorList.add(fSyntaxColorListModel[i][0]);
		fSyntaxColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fSyntaxColorList != null && !fSyntaxColorList.isDisposed()) {
					fSyntaxColorList.select(0);
					handleSyntaxColorListSelection();
				}
			}
		});
		
		for (int i= 0; i < fAppearanceColorListModel.length; i++)
			fAppearanceColorList.add(fAppearanceColorListModel[i][0]);
		fAppearanceColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fAppearanceColorList != null && !fAppearanceColorList.isDisposed()) {
					fAppearanceColorList.select(0);
					handleAppearanceColorListSelection();
				}
			}
		});
		
		for (int i= 0; i < fAnnotationColorListModel.length; i++)
			fAnnotationList.add(fAnnotationColorListModel[i][0]);
		fAnnotationList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fAnnotationList != null && !fAnnotationList.isDisposed()) {
					fAnnotationList.select(0);
					handleAnnotationListSelection();
				}
			}
		});

	}
	
	private void initializeFields() {
		
		Iterator e= fColorButtons.keySet().iterator();
		while (e.hasNext()) {
			ColorEditor c= (ColorEditor) e.next();
			String key= (String) fColorButtons.get(c);
			RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
			c.setColorValue(rgb);
		}
		
		e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
		
		e= fTextFields.keySet().iterator();
		while (e.hasNext()) {
			Text t= (Text) e.next();
			String key= (String) fTextFields.get(t);
			t.setText(fOverlayStore.getString(key));
		}
		
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR);
		fBackgroundColorEditor.setColorValue(rgb);		
		
		boolean default_= fOverlayStore.getBoolean(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR);
		fBackgroundDefaultRadioButton.setSelection(default_);
		fBackgroundCustomRadioButton.setSelection(!default_);
		fBackgroundColorButton.setEnabled(!default_);

	
	}

	private void initializeDefaultColors() {	
		if (!getPreferenceStore().contains(PreferenceConstants.EDITOR_BACKGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), PreferenceConstants.EDITOR_BACKGROUND_COLOR, rgb);
		}
		if (!getPreferenceStore().contains(PreferenceConstants.EDITOR_FOREGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, PreferenceConstants.EDITOR_FOREGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), PreferenceConstants.EDITOR_FOREGROUND_COLOR, rgb);
		}
	}
	
	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fOverlayStore.propagate();
		PerlEditorPlugin.getDefault().savePluginPreferences();

		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		
		fOverlayStore.loadDefaults();

		initializeFields();

		handleSyntaxColorListSelection();
		handleAppearanceColorListSelection();
		handleAnnotationListSelection();
		
		super.performDefaults();

		fPreviewViewer.invalidateTextPresentation();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		if (fBackgroundColor != null && !fBackgroundColor.isDisposed())
			fBackgroundColor.dispose();
		
		super.dispose();
	}
	
	private Button addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		
		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
	}
	
	private Text addTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		return getTextControl(addLabelledTextField(composite, label, key, textLimit, indentation, isNumber));
	}
	
	private static Label getLabelControl(Control[] labelledTextField){
		return (Label)labelledTextField[0];
	}

	private static Text getTextControl(Control[] labelledTextField){
		return (Text)labelledTextField[1];
	}
	
	/**
	 * Returns an array of size 2:
	 *  - first element is of type <code>Label</code>
	 *  - second element is of type <code>Text</code>
	 * Use <code>getLabelControl</code> and <code>getTextControl</code> to get the 2 controls.
	 */
	private Control[] addLabelledTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		Text textControl= new Text(composite, SWT.BORDER | SWT.SINGLE);		
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint= convertWidthInCharsToPixels(textLimit + 1);
		textControl.setLayoutData(gd);
		textControl.setTextLimit(textLimit);
		fTextFields.put(textControl, key);
		if (isNumber) {
			fNumberFields.add(textControl);
			textControl.addModifyListener(fNumberFieldListener);
		} else {
			textControl.addModifyListener(fTextFieldListener);
		}
			
		return new Control[]{labelControl, textControl};
	}
	
	private String loadPreviewContentFromFile(String filename) {
		String line;
		String separator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer(512);
		BufferedReader reader= null;
		try {
			reader= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			while ((line= reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		} catch (IOException io) {
			///PerlEditorPlugin.log(io);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
		return buffer.toString();
	}
	
	private void numberFieldChanged(Text textControl) {
		String number= textControl.getText();
		IStatus status= validatePositiveNumber(number);
		if (!status.matches(IStatus.ERROR))
			fOverlayStore.setValue((String) fTextFields.get(textControl), number);
		updateStatus(status);
	}
	
	private IStatus validatePositiveNumber(String number) {
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.getString("PerlEditorPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0)
					status.setError(PreferencesMessages.getFormattedString("PerlEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				status.setError(PreferencesMessages.getFormattedString("PerlEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	void updateStatus(IStatus status) {
		if (!status.matches(IStatus.ERROR)) {
			for (int i= 0; i < fNumberFields.size(); i++) {
				Text text= (Text) fNumberFields.get(i);
				IStatus s= validatePositiveNumber(text.getText());
				status= StatusUtil.getMoreSevere(s, status);
			}
		}	

		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}
}