package cbg.editor.rules;

import org.eclipse.jface.text.rules.IToken;

public class SingleLineRule extends CasedPatternRule {

	public SingleLineRule(String startSequence, String endSequence, IToken token, char escapeCharacter) {
		this(startSequence, endSequence, token, escapeCharacter, false);
	}

	public SingleLineRule(String startSequence, String endSequence, IToken token, char escapeCharacter, boolean ignoreCase) {
		super(startSequence, endSequence, token, escapeCharacter, true, ignoreCase);
	}

}
