package cbg.editor.rules;

import org.eclipse.jface.text.rules.Token;

public class LToken extends Token {
	protected int length;
	protected boolean isPrevious;
	
	public LToken(Object data) {
		super(data);
	}
	
	public LToken(Object data, int length) {
		this(data);
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
	public void isPrevious(boolean value) {
		isPrevious = value;
	}
	public boolean isPrevious() {
		return isPrevious;
	}
}
