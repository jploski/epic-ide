package cbg.editor.jedit;


public interface SyntaxListener {
	void newRules(String name, boolean highlightDigits, boolean ignoreCase, 
		String digitRE, char escape, String defaultTokenType);
	void newEOLSpan(String type, String text);
	void newSpan(String type, String begin, String end, boolean atLineStart, 
		boolean excludeMatch, boolean noLineBreak, boolean noWordBreak, 
		boolean matchBracket, int noMultipleEndTag, 
	  boolean requireEndTag,
		boolean dynamicTagging, int noMaxChar, String[] groupContent, 
		String beforeTag, String afterTag,
		String delegate);
	void newKeywords(KeywordMap keywords);
	void newTextSequence(String type, String text, String[] groupContent,  boolean atLineStart, boolean atWhitespaceEnd, 
		boolean atWordStart, String delegate);
	void newMark(String type, String text, boolean atLineStart, boolean atWhitespaceEnd, 
		boolean atWordStart, String delegate, boolean isPrevious, boolean excludeMatch);
}
