package cbg.editor.jedit;

public class Span extends TextSequence {
	protected String begin, end;
	protected boolean noLineBreak, noWordBreak, excludeMatch;
	public void accept(IVisitor visitor) {
		visitor.acceptSpan(this);
	}
	public String getStart() {
		return begin;
	}
	public String getEnd() {
		return end;
	}
	public boolean hasDelegate() {
		return getDelegate() != null;
	}

	public boolean noLineBreak() {
		return noLineBreak;
	}

	public boolean isNoWordBreak() {
		return noWordBreak;
	}

	public boolean getExcludeMatch() {
		return excludeMatch;
	}

	public String getDelegateContentType() {
		return getDelegate() + getContentType();
	}

}
