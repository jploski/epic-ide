package cbg.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import cbg.editor.jedit.Mode;
import cbg.editor.jedit.Rule;
import cbg.editor.jedit.TextSequence;
import cbg.editor.jedit.Type;
import cbg.editor.prefs.ColorsPreferencePage;
import cbg.editor.rules.CasedWordRule;
import cbg.editor.rules.ColorManager;
import cbg.editor.rules.ColoringWhitespaceDetector;
import cbg.editor.rules.ColoringWordDetector;
import cbg.editor.rules.ITokenFactory;
import cbg.editor.rules.LToken;

public class ColoringSourceViewerConfiguration extends SourceViewerConfiguration {
	/** 
	 * Preference key used to look up display tab width.
	 * 
	 */
	public final static String PREFERENCE_TAB_WIDTH= "cbg.editor.tab.width"; //$NON-NLS-1$
	/** 
	 * Preference key for inserting spaces rather than tabs.
	 * 
	 */
	public final static String SPACES_FOR_TABS= "spacesForTabs"; //$NON-NLS-1$

	protected ColorManager colorManager;
	private ColoringEditorTools tools;
	private Mode mode;
	protected IAutoIndentStrategy autoIndentStrategy;
	private PreferenceListener prefListener;
	protected Map tokenMap;
	protected Map markTokenMap;
	public static final String MARK_SUFFIX = "+mark";
	
	class PreferenceListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			adaptToPreferenceChange(event);
		}
	};
	
	/**
	 * Adapts the behavior of the contained components to the change
	 * encoded in the given event.
	 * 
	 * @param event the event to which to adapt
	 * @since 2.0
	 */
	protected void adaptToPreferenceChange(PropertyChangeEvent event) {
		if(event.getOldValue() instanceof Boolean) {
			adaptToStyleChange(event);
		} else {
			adaptToColorChange(event);
		}
	}

	/** Copied from org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration
	 */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		List list = new ArrayList();
		// prefix[0] is either '\t' or ' ' x tabWidth, depending on useSpaces
					
		int tabWidth = getPreferenceStore().getInt(PREFERENCE_TAB_WIDTH);
		boolean useSpaces = !getPreferenceStore().getBoolean(SPACES_FOR_TABS);
		for (int i= 0; i <= tabWidth; i++) {
		    StringBuffer prefix = new StringBuffer();
			if (useSpaces) {
			    for (int j = 0; j + i < tabWidth; j++)
			    	prefix.append(' ');
		    	
				if (i != 0)
		    		prefix.append('\t');				
			} else {    
			    for (int j = 0; j < i; j++)
			    	prefix.append(' ');
				if (i != tabWidth)
		    		prefix.append('\t');
			}
			list.add(prefix.toString());
		}
		list.add(""); //$NON-NLS-1$
		return (String[]) list.toArray(new String[list.size()]);
	}

	public int getTabWidth(ISourceViewer sourceViewer) {
		return getPreferenceStore().getInt(PREFERENCE_TAB_WIDTH);
	}

	private IPreferenceStore getPreferenceStore() {
		return EditorPlugin.getDefault().getPreferenceStore();
	}

	
	private void adaptToColorChange(PropertyChangeEvent event) {
		RGB rgb= null;
		Token token = (Token) tokenMap.get(event.getProperty());
		LToken lToken = (LToken) tokenMap.get(event.getProperty() + MARK_SUFFIX);
		if(token == null && lToken == null) {
			return;
		}
		Object value= event.getNewValue();
		if (value instanceof RGB)
			rgb= (RGB) value;
		else if (value instanceof String)
			rgb= StringConverter.asRGB((String) value);
		if (rgb != null) {
			String property= event.getProperty();
			Object data= token.getData();
			if (data instanceof TextAttribute) {
				TextAttribute oldAttr= (TextAttribute) data;
				TextAttribute newAttr = new TextAttribute(colorManager.getColor(property), oldAttr.getBackground(), oldAttr.getStyle());
				if(token != null) token.setData(newAttr);
				if(lToken != null) lToken.setData(newAttr);
			}
		}
	}
	
	private void adaptToStyleChange(PropertyChangeEvent event) {
		boolean bold = false;
		// bold properties need to be converted to colors for the map
		String colorPlusStyle = event.getProperty();
		String colorName = colorPlusStyle.substring(0, colorPlusStyle.length() - ColorsPreferencePage.BOLD_SUFFIX.length());
		Token token = (Token)tokenMap.get(colorName);
		LToken lToken = (LToken) tokenMap.get(colorName + MARK_SUFFIX);
		if(token == null && lToken == null) return;
		Object value = event.getNewValue();
		bold = ((Boolean) value).booleanValue();
		
		Object data= token.getData();
		if (data instanceof TextAttribute) {
			TextAttribute oldAttr= (TextAttribute) data;
			TextAttribute newAttr= new TextAttribute(oldAttr.getForeground(), oldAttr.getBackground(), bold ? SWT.BOLD : SWT.NORMAL);
			boolean isBold = (oldAttr.getStyle() == SWT.BOLD);
			if (isBold != bold) {
				if(token != null) token.setData(newAttr);
				if(lToken != null) lToken.setData(newAttr);
			}
		}
	}

	public ColoringSourceViewerConfiguration(ColorManager colorManager, ColoringEditorTools tools) {
		super();
		this.colorManager = colorManager;
		this.tools = tools;
		tokenMap = new HashMap();
		autoIndentStrategy = new DefaultAutoIndentStrategy();
		prefListener = new PreferenceListener();
		if(EditorPlugin.getDefault() == null) return;
		EditorPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(prefListener);
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		addKeywordHighlighting(reconciler);
		addAllButKeywordHighlighting(reconciler);
		addDelegatedKeywords(reconciler);
		return reconciler;
	}

	private void addDelegatedKeywords(PresentationReconciler reconciler) {
		Collection delegates = mode.getDelegates().keySet();
		for (Iterator rules = delegates.iterator(); rules.hasNext();) {
			String mungedName = (String)rules.next();
			Rule rule = (Rule) mode.getDelegates().get(mungedName);
			RuleBasedScanner scanner = getDelegateScanner(rule);
			DefaultDamagerRepairer dr= new DefaultDamagerRepairer(scanner);
			reconciler.setDamager(dr, mungedName);
			reconciler.setRepairer(dr, mungedName);
		}
	}

	private RuleBasedScanner getDelegateScanner(Rule rule) {
		RuleBasedScanner scanner = new RuleBasedScanner();
		List rules = new ArrayList();
		
		String colorName = colorManager.colorForType(rule.getDefaultTokenType());
		IToken defaultToken = newToken(colorName);
		scanner.setDefaultReturnToken(defaultToken);
		ColoringEditorTools.add(rule, rules, new ITokenFactory() {
			public IToken makeToken(Type type) {
				String color = colorManager.colorForType(type.getColor());
				return newToken(color);
			}
		});
		addTextSequenceRules(rule, rules, defaultToken);
		
		scanner.setRules((IRule[]) rules.toArray(new IRule[rules.size()]));
		return scanner;
	}

	private void addAllButKeywordHighlighting(PresentationReconciler reconciler) {
		setupScannerType(reconciler, Type.SINGLE_S);
		setupScannerType(reconciler, Type.MULTI_S);
		setupScannerType(reconciler, Type.SEQ);
		setupScannerType(reconciler, Type.EOL_SPAN);
		setupScannerTypeForMark(reconciler);
	}

	private void setupScannerType(PresentationReconciler reconciler, String typeName) {
		String[] contentTypes = mode.getContentTypes();
		for (int i = 0; i < contentTypes.length; i++) {
			String contentType = contentTypes[i];
			if(!contentType.startsWith(typeName)) continue;
			RuleBasedScanner scanner = new RuleBasedScanner();			
			String type = contentType.substring(contentType.lastIndexOf('.') + 1);
			IToken defaultToken = newToken(colorManager.colorForType(type));
			scanner.setDefaultReturnToken(defaultToken);
			DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
			reconciler.setDamager(dr, contentType);
			reconciler.setRepairer(dr, contentType);
		}
	}
	private void setupScannerTypeForMark(PresentationReconciler reconciler) {
		String[] contentTypes = mode.getContentTypes();
		for (int i = 0; i < contentTypes.length; i++) {
			String contentType = contentTypes[i];
			if(contentType.startsWith(Type.MARK_PREVIOUS) || contentType.startsWith(Type.MARK_FOLLOWING)) {
				RuleBasedScanner scanner = new RuleBasedScanner();			
				String colorType = contentType.substring(contentType.lastIndexOf('.') + 1);
				LToken defaultToken = (LToken) newToken(colorManager.colorForType(colorType), true);
				defaultToken.setLength(getLength(contentType));
				defaultToken.isPrevious(contentType.startsWith(Type.MARK_PREVIOUS));
				scanner.setDefaultReturnToken(defaultToken);
				DefaultDamagerRepairer dr = new MarkDamagerRepairer(scanner);
				reconciler.setDamager(dr, contentType);
				reconciler.setRepairer(dr, contentType);
			}
		}
	}

	private int getLength(String contentType) {
		int start = contentType.indexOf('@');
		if(start== -1) return 0;
		int end = contentType.indexOf('.');
		if(end == -1) return 0;
		try {
			return Integer.parseInt(contentType.substring(start + 1, end));
		} catch(NumberFormatException ne) {
			return 0;
		}
	}

	private void addKeywordHighlighting(PresentationReconciler reconciler) {
		RuleBasedScanner codeScanner = getCodeScanner();
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(codeScanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	}

	public RuleBasedScanner getCodeScanner() {
		RuleBasedScanner scanner = new RuleBasedScanner();
		List rules = new ArrayList();
		Rule main = mode.getDefaultRuleSet();
		addWhitespaceRule(rules);

		IToken defaultToken = newToken(ColorsPreferencePage.NULL_COLOR);
		addTextSequenceRules(main, rules, defaultToken);

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		scanner.setRules(result);
		return scanner;
	}

	private void addWhitespaceRule(List rules) {
		rules.add(new WhitespaceRule(new ColoringWhitespaceDetector()));
	}

	private void addTextSequenceRules(Rule ruleSet, List rules, IToken defaultToken) {
		ColoringWordDetector wordDetector = new ColoringWordDetector();
		if(ruleSet.getHighlightDigits()) rules.add(new NumberRule(newToken(ColorsPreferencePage.DIGIT_COLOR)));
		CasedWordRule wordRule = new CasedWordRule(wordDetector, defaultToken, ruleSet.getKeywords().ignoreCase());

		addKeywordRule(ruleSet, "COMMENT1", ColorsPreferencePage.COMMENT1_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "COMMENT2", ColorsPreferencePage.COMMENT2_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "LITERAL1", ColorsPreferencePage.LITERAL1_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "LITERAL2", ColorsPreferencePage.LITERAL2_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "LABEL", ColorsPreferencePage.LABEL_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "KEYWORD1", ColorsPreferencePage.KEYWORD1_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "KEYWORD2", ColorsPreferencePage.KEYWORD2_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "KEYWORD3", ColorsPreferencePage.KEYWORD3_COLOR, wordRule, wordDetector);		
		addKeywordRule(ruleSet, "FUNCTION", ColorsPreferencePage.FUNCTION_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "MARKUP", ColorsPreferencePage.MARKUP_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "OPERATOR", ColorsPreferencePage.OPERATOR_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "DIGIT", ColorsPreferencePage.DIGIT_COLOR, wordRule, wordDetector);
		addKeywordRule(ruleSet, "INVALID", ColorsPreferencePage.INVALID_COLOR, wordRule, wordDetector);				
		rules.add(wordRule);
	}

	protected IToken newToken(String colorName) {
		return newToken(colorName, false);
	}
	private IToken newToken(String colorName, boolean isMark) {
		String suffix = isMark ? MARK_SUFFIX : "";
		IToken token = (IToken) tokenMap.get(colorName + suffix);
		if(token != null) return token;
		int style = colorManager.getStyleFor(colorName);
		TextAttribute ta = new TextAttribute(colorManager.getColor(colorName), null, style);	
		token = isMark ? new LToken(ta) : new Token(ta);
		tokenMap.put(colorName + suffix, token);
		return token;
	}


	private void addKeywordRule(Rule ruleSet, String type, String tokenName, CasedWordRule keywordRule, ColoringWordDetector wordDetector) {
		String[] keywords = ruleSet.getKeywords().get(type);
		IToken keywordToken = newToken(tokenName);
		if(keywords!= null && keywords.length != 0) {
			for (int i = 0; i < keywords.length; i++) {
				wordDetector.addWord(keywords[i]);
				keywordRule.addWord(keywords[i], keywordToken);
			}
		}
		List allOfType = ruleSet.get(type);
		for (Iterator allI = allOfType.iterator(); allI.hasNext();) {
			Type aType = (Type) allI.next();
			if(aType.getType() == Type.SEQ && 
				wordDetector.isWordStart(((TextSequence)aType).getText().charAt(0))) {
				
				keywordRule.addWord(aType.getText(), keywordToken);
			}
		}
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return mode.getContentTypes();
	}

	public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer, String contentType) {
		return autoIndentStrategy;
	}

	public Mode getMode() {
		return mode;
	}

	/** 
	 * Inform the SourceViewerConfiguration of the filename of
	 * the editor. This information is needed so the receiver
	 * can setup it's mode.
	 * 	 * @param filename	 */
	public void setFilename(String filename) {
		setMode(Modes.getModeFor(filename));
	}
	public void setMode(Mode theMode) {
		mode = theMode;
	}
}