package cbg.editor.jedit;

public class Span extends TextSequence {	protected String begin, end, countDelimterChars, afterTag, beforeTag;
	protected String optinalModifiers;	protected boolean noLineBreak, noWordBreak, excludeMatch, matchBracket;
	protected boolean requireEndTag, dynamicTagging;
  protected boolean  requireBeforeWhitespace, requireAfterWhitespace; 
	protected int noMultipleEndTag, noMaxChar;
	protected String[] groupContent;
	public void accept(IVisitor visitor) {
		visitor.acceptSpan(this);
	}
	public final String getStart() {
		return begin;
	}
	public final String getEnd() {
	  return end;
	}
	public final boolean hasDelegate() {
		return getDelegate() != null;
	}

	public final boolean noLineBreak() {
		return noLineBreak;
	}

	public final boolean matchBracket() {
	  return matchBracket;
	}
	
	public final int noMultipleEndTag() {
	  return noMultipleEndTag;
	}
	public final boolean isNoWordBreak() {
		return noWordBreak;
	}

	public final boolean getExcludeMatch() {
		return excludeMatch;
	}
		
	public final boolean requireEndTag() {
	  return requireEndTag;
	}
	
	public final boolean dynamicTagging() {
	  return dynamicTagging;
	}
	
	public final int noMaxChar() {
	  return noMaxChar;
	}
	
	public final String[] getGroupContent() {
	  return groupContent;
	}
	
	public final String getCountDelimterChars() {
	  return countDelimterChars;
	}
	public final String getBeforeTag(){
	  return beforeTag;
	}
	
	public final String getAfterTag() {
	  return afterTag;
	}
	public String getDelegateContentType() {
		return getDelegate() + getContentType();
	}
	public boolean  getRequireBeforeWhitespace() {	  return requireBeforeWhitespace;	}		public boolean getRequireAfterWhitespace() {	  return requireAfterWhitespace;	}		public String optinalModifiers() {	  return optinalModifiers;	}
}

	