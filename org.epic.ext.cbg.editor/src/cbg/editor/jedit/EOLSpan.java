package cbg.editor.jedit;

public class EOLSpan extends Span {

	public void accept(IVisitor visitor) {
		visitor.acceptEolSpan(this);
	}

}
