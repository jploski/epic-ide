package cbg.editor.rules;

import org.eclipse.jface.text.rules.IToken;

import cbg.editor.jedit.Type;

public interface ITokenFactory {
	IToken makeToken(Type type);
}
