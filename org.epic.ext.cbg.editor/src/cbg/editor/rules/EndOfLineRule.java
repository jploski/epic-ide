package cbg.editor.rules;

import org.eclipse.jface.text.rules.IToken;

public class EndOfLineRule extends SingleLineRule {

	public EndOfLineRule(String startSequence, IToken token, boolean ignoreCase) {
		this(startSequence, token, (char)0, ignoreCase);
	}

	public EndOfLineRule(String startSequence, IToken token, char escapeCharacter, boolean ignoreCase) {
		super(startSequence, null, token, escapeCharacter, ignoreCase);
	}

}
