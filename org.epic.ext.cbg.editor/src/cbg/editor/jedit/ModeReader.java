package cbg.editor.jedit;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import cbg.editor.ColoringEditorTools;
import cbg.editor.ColoringPartitionScanner;

public class ModeReader {
	protected SyntaxListener listener;
	
	public ModeReader(SyntaxListener listener) {
		super();
		this.listener = listener;
	}
	
	public void read(String filename) {
		SAXReader reader = new SAXReader();
		Document doc = null;
		try {
			doc = reader.read(ColoringEditorTools.getFile(filename));
		} catch (DocumentException e) {
			e.printStackTrace();
			return;
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		Element root = doc.getRootElement();
		//List properties = root.elements("PROPS");
		//todo parse the properties
		for (Iterator iter = root.elementIterator("RULES"); iter.hasNext();) {
			Element rulesElement = (Element) iter.next();
			createRule(rulesElement);
			/* Because over of rules is important I must read the elements in
			 * order instead of using elementIterators("SPAN", etc)
			 */
			List allTypes = rulesElement.elements();
			for (Iterator allTypesI = allTypes.iterator(); allTypesI.hasNext();) {
				Element element = (Element) allTypesI.next();
				createType(element);
			}
		}
	}

	private void createType(Element element) {
		if(element.getName().equals("SPAN")) {
			createSpan(element);
		} else if(element.getName().equals("EOL_SPAN")) {
			createEOL(element);
		} else if(element.getName().equals("SEQ")) {
			createTextSequence(element);
		} else if(element.getName().equals("KEYWORDS")) {
			createKeywords(element);
		} else if(element.getName().equals("MARK_PREVIOUS")) {
			createMark(element, true);
		} else if(element.getName().equals("MARK_FOLLOWING")) {
			createMark(element, false);
		} else if(element.getName().equals("WHITESPACE")) {
			// ignore for now
		} else {
			System.out.println("Ignore unknown element " + element.getName());
		}
	}


	private void createKeywords(Element keywordsE) {
		KeywordMap keywords = new KeywordMap(bool(keywordsE, "IGNORE_CASE", true));
		keywords.put(ColoringPartitionScanner.KEYWORD1, toStringArray(keywordsE.elements("KEYWORD1")));
		keywords.put(ColoringPartitionScanner.KEYWORD2, toStringArray(keywordsE.elements("KEYWORD2")));
		keywords.put(ColoringPartitionScanner.KEYWORD3, toStringArray(keywordsE.elements("KEYWORD3")));
		keywords.put(ColoringPartitionScanner.COMMENT1, toStringArray(keywordsE.elements("COMMENT1")));
		keywords.put(ColoringPartitionScanner.COMMENT2, toStringArray(keywordsE.elements("COMMENT2")));
		keywords.put(ColoringPartitionScanner.LITERAL1, toStringArray(keywordsE.elements("LITERAL1")));
		keywords.put(ColoringPartitionScanner.LITERAL2, toStringArray(keywordsE.elements("LITERAL2")));
		keywords.put(ColoringPartitionScanner.LABEL, toStringArray(keywordsE.elements("LABEL")));
		keywords.put(ColoringPartitionScanner.FUNCTION, toStringArray(keywordsE.elements("FUNCTION")));
		keywords.put(ColoringPartitionScanner.MARKUP, toStringArray(keywordsE.elements("MARKUP")));
		keywords.put(ColoringPartitionScanner.OPERATOR, toStringArray(keywordsE.elements("OPERATOR")));
		keywords.put(ColoringPartitionScanner.DIGIT, toStringArray(keywordsE.elements("DIGIT")));
		keywords.put(ColoringPartitionScanner.INVALID, toStringArray(keywordsE.elements("INVALID")));
		listener.newKeywords(keywords);
	}

	private String[] toStringArray(List list) {
		if(list.isEmpty()) return new String[0];
		String[] strings = new String[list.size()];
		int i = 0;
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			strings[i] = ((Element)iter.next()).getText();
			i++;
		}
		return strings;
	}



	private void createTextSequence(Element seqElement) {
		boolean atLineStart = bool(seqElement, "AT_LINE_START", false);
		boolean atWhitespaceEnd = bool(seqElement, "AT_WHITESPACE_END", false);
		boolean atWordStart = bool(seqElement, "AT_WORD_START", false);
		String type = seqElement.attributeValue("TYPE");
		String delegate = seqElement.attributeValue("DELEGATE");
		listener.newTextSequence(type, seqElement.getText(), atLineStart, atWhitespaceEnd, atWordStart, delegate);
	}


	private void createEOL(Element eolElement) {
		listener.newEOLSpan(eolElement.attributeValue("TYPE"), eolElement.getText());
	}


	private void createSpan(Element spanElement) {
		String type = spanElement.attributeValue("TYPE");
		boolean atLineStart = bool(spanElement, "AT_LINE_START", false);
		boolean excludeMatch = bool(spanElement, "EXCLUDE_MATCH", false);
		boolean noLineBreak = bool(spanElement, "NO_LINE_BREAK", false);
		boolean noWordBreak = bool(spanElement, "NO_WORD_BREAK", false);
		String delegate = spanElement.attributeValue("DELEGATE");
		String begin = spanElement.element("BEGIN").getText();
		String end = spanElement.element("END").getText();
		listener.newSpan(type, begin, end, atLineStart, excludeMatch, noLineBreak, noWordBreak, delegate);
	}

	private void createMark(Element markElement, boolean isPrevious) {
		boolean atLineStart = bool(markElement, "AT_LINE_START", false);
		boolean atWhitespaceEnd = bool(markElement, "AT_WHITESPACE_END", false);
		boolean excludeMatch = bool(markElement, "EXCLUDE_MATCH", false);
		boolean atWordStart = bool(markElement, "AT_WORD_START", false);
		String type = markElement.attributeValue("TYPE");
		String delegate = markElement.attributeValue("DELEGATE");
		listener.newMark(type, markElement.getText(), atLineStart, atWhitespaceEnd, 
			atWordStart, delegate, isPrevious, excludeMatch);
	}

	protected void createRule(Element rulesElement) {
		String name = rulesElement.attributeValue("SET", Rule.DEFAULT_NAME);
		boolean highlightDigits = bool(rulesElement, "HIGHLIGHT_DIGITS", false);
		boolean ignoreCase = bool(rulesElement, "IGNORE_CASE", true);
		String digitRE = rulesElement.attributeValue("DIGIT_RE");
		char escape = rulesElement.attributeValue("ESCAPE", "" + (char)0).charAt(0);
		String defaultTokenType = rulesElement.attributeValue("DEFAULT", ColoringPartitionScanner.NULL);
		listener.newRules(name, highlightDigits, ignoreCase, digitRE, escape, defaultTokenType);
	}

	protected boolean bool(Element element, String attributeName, boolean defaultValue) {
		return Boolean.valueOf(
			element.attributeValue(attributeName, String.valueOf(defaultValue))).booleanValue();
	}
}
