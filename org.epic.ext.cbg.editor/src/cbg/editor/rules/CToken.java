package cbg.editor.rules;

import org.eclipse.jface.text.rules.Token;

import cbg.editor.jedit.Type;

public class CToken extends Token {
	protected Type type;
	
	public CToken(Type type) {
		super(type.getType());
		this.type = type;
	}

	public String getColor() {
		return type.getColor();
	}

	public Object getData() {
		return type.getContentType();
	}
}
