package cbg.editor.jedit;

public class Mark extends Span {
	protected boolean isPrevious;
	
	public boolean isMarkPrevious() {
		return isPrevious;
	}
	public boolean isMarkFollowing() {
		return !isPrevious;
	}
	
	public void accept(IVisitor visitor) {
		visitor.acceptMark(this);
	}

	public String getType() {
		return super.getType() + "@" + text.length();
	}

}
