package cbg.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import cbg.editor.jedit.Mode;
import cbg.editor.jedit.Rule;
import cbg.editor.jedit.Type;
import cbg.editor.rules.CToken;
import cbg.editor.rules.ITokenFactory;

public class ColoringPartitionScanner extends RuleBasedPartitionScanner {
	public static final String NULL = "NULL";
	public final static String COMMENT1 = "COMMENT1";
	public final static String COMMENT2 = "COMMENT2";
	public static final String LITERAL1 = "LITERAL1";
	public static final String LITERAL2 = "LITERAL2";
	public static final String LABEL = "LABEL";
	public final static String KEYWORD1 = "KEYWORD1";
	public static final String KEYWORD2 = "KEYWORD2";
	public static final String KEYWORD3 = "KEYWORD3";
	public static final String FUNCTION = "FUNCTION";
	public static final String MARKUP = "MARKUP";
	public static final String OPERATOR = "OPERATOR";
	public static final String DIGIT = "DIGIT";
	public static final String INVALID = "INVALID";
	protected Mode mode;
	protected int markLength = -1;
	protected char escape;
	private Map ruleSets;
	
	class LitePartitionScanner extends RuleBasedPartitionScanner {
		public int getOffset() {
			return fOffset;
		}
		public void setTokenOffset(int off) {
			fTokenOffset = off;
		}
		public void setOffset(int off) {
			fOffset = off;
		}
	}
	
	/** 
	 * This will setup the rules for elements *not* defined as
	 * children of the keywords elements.
	 * JEdit allows the "type" of an element to appear inside the
	 * keywords element or outside. For example you could have the
	 * LITERAL2 tag inside the keywords element and as a top level
	 * element.	 */
	public ColoringPartitionScanner(String filename) {
		this(Modes.getModeFor(filename));
	}

	public ColoringPartitionScanner(Mode mode) {
		super();
		this.mode = mode;
		escape = mode.getDefaultRuleSet().getEscape();
		setPredicateRules(createRuleSet(mode.getDefaultRuleSet()));
		ruleSets = new HashMap();
	}

	private IPredicateRule[] createRuleSet(Rule rule) {
		List rules = new ArrayList();
		ColoringEditorTools.add(rule, rules, new ITokenFactory() {
			public IToken makeToken(Type type) {
				return new CToken(type);
			}
		});
		return (IPredicateRule[]) rules.toArray(new IPredicateRule[rules.size()]);
	}


	public int getTokenLength() {
		if(markLength != -1) {
			int length = markLength;
			markLength = -1;
			return length;
		}
		return super.getTokenLength();
	}

	public int getOffset() {
		return fOffset;
	}
	
	public void setOffset(int loc) {
		fOffset = loc;
	}
	public void setMarkLength(int length) {
		markLength = length;
	}
	public String[] getContentTypes() {
		return mode.getContentTypes();
	}
	
	public int backup() {
		--fOffset;
		if (fOffset > -1) {
			try {
				return fDocument.getChar(fOffset);
			} catch(BadLocationException bl) {}
		} else {
			fOffset = 0;
		}
		return ICharacterScanner.EOF;
	}
	
	/** Move the token offset by a delta.
	 * 	 * @param delta - a positive or negative amount to move the token offset.
	 * @nonapi
	 */
	public void moveTokenOffset(int delta) {
		fTokenOffset = fTokenOffset + delta;
	}

	private IPredicateRule[] getRuleSet(Rule rule) {
		IPredicateRule[] ruleSet = (IPredicateRule[])ruleSets.get(rule.getName());
		if(ruleSet == null) {
			ruleSet = createRuleSet(rule);
			ruleSets.put(rule.getName(), ruleSet);
		}
		return ruleSet;
	}

}