package cbg.editor.jedit;

public class TextSequence extends Type {
	protected String delegateName;

	public void accept(IVisitor visitor) {
		visitor.acceptTextSequence(this);
	}

	public String getDelegate() {
		return delegateName;
	}

}
