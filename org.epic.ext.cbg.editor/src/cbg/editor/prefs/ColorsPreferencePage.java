package cbg.editor.prefs;

import cbg.editor.BooleanColorFieldEditor;
import cbg.editor.EditorPlugin;
import cbg.editor.rules.ColorManager;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ColorsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String NULL_COLOR = "nullColor";
	public static final String COMMENT1_COLOR = "comment1Color";
	public static final String COMMENT2_COLOR = "comment2Color";	
	public static final String LITERAL1_COLOR = "literal1Color";
	public static final String LITERAL2_COLOR = "literal2Color";	
	public static final String LABEL_COLOR = "labelColor";	
	public static final String KEYWORD1_COLOR = "keyword1Color";
	public static final String KEYWORD2_COLOR = "keyword2Color";
	public static final String KEYWORD3_COLOR = "keyword3Color";
	public static final String FUNCTION_COLOR = "functionColor";
	public static final String MARKUP_COLOR = "markupColor";
	public static final String OPERATOR_COLOR = "operatorColor";
	public static final String DIGIT_COLOR = "digitColor";
	public static final String INVALID_COLOR = "invalidColor";

	public static final String BOLD_SUFFIX = "Bold";
		
	public ColorsPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(EditorPlugin.getDefault().getPreferenceStore());
	}

	private static void setDefaultColors(IPreferenceStore store) {
		PreferenceConverter.setDefault(store, COMMENT1_COLOR, ColorManager.DEFAULT_COMMENT1_COLOR);
		PreferenceConverter.setDefault(store, COMMENT2_COLOR, ColorManager.DEFAULT_COMMENT2_COLOR);		
		PreferenceConverter.setDefault(store, DIGIT_COLOR, ColorManager.DEFAULT_DIGIT_COLOR);		
		PreferenceConverter.setDefault(store, FUNCTION_COLOR, ColorManager.DEFAULT_FUNCTION_COLOR);		
		PreferenceConverter.setDefault(store, INVALID_COLOR, ColorManager.DEFAULT_INVALID_COLOR);
		PreferenceConverter.setDefault(store, KEYWORD1_COLOR, ColorManager.DEFAULT_KEYWORD1_COLOR);
		PreferenceConverter.setDefault(store, KEYWORD2_COLOR, ColorManager.DEFAULT_KEYWORD2_COLOR);
		PreferenceConverter.setDefault(store, KEYWORD3_COLOR, ColorManager.DEFAULT_KEYWORD3_COLOR);		
		PreferenceConverter.setDefault(store, LABEL_COLOR, ColorManager.DEFAULT_LABEL_COLOR);
		PreferenceConverter.setDefault(store, LITERAL1_COLOR, ColorManager.DEFAULT_LITERAL1_COLOR);
		PreferenceConverter.setDefault(store, LITERAL2_COLOR, ColorManager.DEFAULT_LITERAL2_COLOR);
		PreferenceConverter.setDefault(store, MARKUP_COLOR, ColorManager.DEFAULT_MARKUP_COLOR);
		PreferenceConverter.setDefault(store, OPERATOR_COLOR, ColorManager.DEFAULT_OPERATOR_COLOR);		
		PreferenceConverter.setDefault(store, NULL_COLOR, ColorManager.DEFAULT_STRING_COLOR);
		store.setDefault(COMMENT1_COLOR + BOLD_SUFFIX, false);
		store.setDefault(COMMENT2_COLOR + BOLD_SUFFIX, false);		
		store.setDefault(DIGIT_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(FUNCTION_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(INVALID_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(KEYWORD1_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(KEYWORD2_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(KEYWORD3_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(LABEL_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(LITERAL1_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(LITERAL2_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(MARKUP_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(OPERATOR_COLOR + BOLD_SUFFIX,  false);
		store.setDefault(NULL_COLOR + BOLD_SUFFIX,  false);
	}

	protected void createFieldEditors() {
		Composite p = getFieldEditorParent();
		String b = "&Bold";
		addField(new BooleanColorFieldEditor(NULL_COLOR, "Default color", NULL_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(KEYWORD1_COLOR, "Keyword1 color", KEYWORD1_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(KEYWORD2_COLOR, "Keyword2 color", KEYWORD2_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(KEYWORD3_COLOR, "Keyword3 color", KEYWORD3_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(COMMENT1_COLOR, "Comment1 color", COMMENT1_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(COMMENT2_COLOR, "Comment2 color", COMMENT2_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(LITERAL1_COLOR, "Literal1 color", LITERAL1_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(LITERAL2_COLOR, "Literal2 color", LITERAL2_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(LABEL_COLOR, "Label color", LABEL_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(FUNCTION_COLOR, "Function color", FUNCTION_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(MARKUP_COLOR, "Markup color", MARKUP_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(OPERATOR_COLOR, "Operator color", OPERATOR_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(DIGIT_COLOR, "Digit color", DIGIT_COLOR + BOLD_SUFFIX, b, p));
		addField(new BooleanColorFieldEditor(INVALID_COLOR, "Invalid color", INVALID_COLOR + BOLD_SUFFIX, b, p));
	}

	public void init(IWorkbench workbench) {
		if(workbench == null) {}
	}

}