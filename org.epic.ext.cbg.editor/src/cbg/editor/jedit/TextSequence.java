package cbg.editor.jedit;

public class TextSequence extends Type {
	protected String delegateName;
	protected String[] groupContent;

	public void accept(IVisitor visitor) {
		visitor.acceptTextSequence(this);
	}

	public String getDelegate() {
		return delegateName;
	}

	public String[] groupContent() {
	  return groupContent;
	}
}
