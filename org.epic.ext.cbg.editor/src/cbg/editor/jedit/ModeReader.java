package cbg.editor.jedit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
	char escape='\u0000';
	private static String begin="";  //static usage of begin, so we could use it in readGroupContent
	
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
		begin=seqElement.getText();
		String[] myGroupContent=readGroupContent(seqElement, false);
		listener.newTextSequence(type, begin, myGroupContent, atLineStart, atWhitespaceEnd, atWordStart, delegate);
	}


	private void createEOL(Element eolElement) {
		listener.newEOLSpan(eolElement.attributeValue("TYPE"), eolElement.getText());
	}


	private void createSpan(Element spanElement) {
		String type = spanElement.attributeValue("TYPE");
		boolean atLineStart = bool(spanElement, "AT_LINE_START", false); //NOTE: Never used anywhere
		boolean excludeMatch = bool(spanElement, "EXCLUDE_MATCH", false);
		boolean noLineBreak = bool(spanElement, "NO_LINE_BREAK", false);
		boolean noWordBreak = bool(spanElement, "NO_WORD_BREAK", false);
		
		//added by LeO to support specific Perl-Spanning
		boolean matchBracket = bool(spanElement, "MATCH_BRACKET", false);
		String MultipleEndTag = spanElement.attributeValue("NO_OF_MULTIPLE_ENDTAG");
		int noMultipleEndTag = (MultipleEndTag == null) ? 1 : Integer.parseInt(MultipleEndTag);
		if (noMultipleEndTag < 1) {
		  noMultipleEndTag = 1;
		}
		boolean ignoreTextAfterStartTag = bool(spanElement, "IGNORE_TEXT_AFTER_START_TAG", false);
		boolean requireEndTag = bool(spanElement, "REQUIRE_END_TAG", false);
		boolean dynamicTagging = bool(spanElement, "DYNAMIC_TAGGING", false);
		String maxChar= spanElement.attributeValue("DELIMITER_MAX_CHAR");
		int noMaxChar = (maxChar == null) ? 1 : Integer.parseInt(maxChar);
		//The future project would be: if == 0 => dyanamic search
		if (noMaxChar < 1) {
		  noMaxChar = 1;
		}
		String countDelimterChars = spanElement.attributeValue("DELIMITER_TAG_CHARS");
		if (countDelimterChars == null)
		  countDelimterChars = "";
		boolean requireBeforeWhitespace = bool(spanElement, "REQUIRE_WHITESPACE_BEFORE", false);		boolean requireAfterWhitespace = bool(spanElement, "REQUIRE_WHITESPACE_AFTER", false);		String optinalModifiers= spanElement.attributeValue("OPTIONAL_MODIFIER");		if (optinalModifiers == null) {		  optinalModifiers ="";		}
		String delegate = spanElement.attributeValue("DELEGATE");
	  begin ="";
		if (spanElement.element("BEGIN") != null) {
		  begin = spanElement.element("BEGIN").getText();
		}

		String end = spanElement.element("END").getText();
		if (ignoreTextAfterStartTag) {
		  end ="";
		}
		
		String beforeTag = spanElement.attributeValue("REQUIRE_BEFORE_DELIMITER_CHAR");
		if (beforeTag == null)
		  beforeTag = "";

		String afterTag = spanElement.attributeValue("REQUIRE_AFTER_DELIMITER_CHAR");
		if (afterTag == null)
		  afterTag = "";

		String[] myGroupContent=readGroupContent(spanElement, true); //so the 'begin is modified properly
		listener.newSpan(type, begin, end, atLineStart, excludeMatch, noLineBreak, 
		                 noWordBreak, matchBracket, noMultipleEndTag, 
		                 requireEndTag, dynamicTagging, noMaxChar, 
		                 myGroupContent, countDelimterChars, beforeTag, afterTag, 
		                 delegate, requireBeforeWhitespace, requireAfterWhitespace, optinalModifiers);
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
		escape = rulesElement.attributeValue("ESCAPE", "" + (char)0).charAt(0);
		String defaultTokenType = rulesElement.attributeValue("DEFAULT", ColoringPartitionScanner.NULL);
		listener.newRules(name, highlightDigits, ignoreCase, digitRE, escape, defaultTokenType);
	}

	protected boolean bool(Element element, String attributeName, boolean defaultValue) {
		return Boolean.valueOf(
			element.attributeValue(attributeName, String.valueOf(defaultValue))).booleanValue();
	}
	
	private String[] readGroupContent(Element myElement, boolean beginHasUsefulContent) {
		int i=0;
		ArrayList groupContentArray=new ArrayList();
		String[] groupContent= null;
		for (Iterator iter = myElement.elementIterator("GROUP"); iter.hasNext();) {
      Element element = (Element) iter.next();
      groupContentArray.add(element.getText());
    }
		//make out of the Group-Elements a speedy String-Array instead of Collection
		if (groupContentArray.size() > 0) {
		  //Let's add also the field of "BEGIN"
		  if (begin.length() == 0 || !beginHasUsefulContent ) {
		    begin = (String) groupContentArray.get(0);
		  } else {
		    groupContentArray.add(begin);
		  }
		  
			Field contentDataField = null;
	    try {
	      contentDataField = ArrayList.class.getDeclaredField("elementData");
	    } catch (SecurityException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	    } catch (NoSuchFieldException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	    }
	    contentDataField.setAccessible(true);
			Object[] contentData = null;
	    try {
	      contentData = (Object[]) contentDataField.get(groupContentArray);
	    } catch (IllegalArgumentException e1) {
	      // TODO Auto-generated catch block
	      e1.printStackTrace();
	    } catch (IllegalAccessException e1) {
	      // TODO Auto-generated catch block
	      e1.printStackTrace();
	    }
	    //max Length of all Strings
	    int maxLength=0;
	    for (int j = 0; j < groupContentArray.size(); j++) {
        if (((String) contentData[j]).length() > maxLength) 
          maxLength = ((String) contentData[j]).length();
      }

	    //convert into a String-array!
	    groupContent= new String[maxLength+1];
	    //TODO other possibility, if escape is NOT defined???
	    if (escape == '\u0000') {
	      escape = '\u0785';
	    }
	    groupContent[0]=escape + "";
			for (int j = 0; j < groupContentArray.size(); j++) {
			  maxLength =contentData[j].toString().length();
			  if (groupContent[maxLength] == null) {
			    groupContent[maxLength] = escape + ( (String) contentData[j]);
			  } else {
			    if (groupContent[maxLength].indexOf((String) contentData[j]) < 0) {
			      //only add non-existing fields
			      groupContent[maxLength] += escape + ( (String) contentData[j]);
			    }
			  }
      }
			for (int j = 1; j < groupContent.length; j++) {
        if (groupContent[j] == null ) {
          groupContent[j] = "";
        } else {
          groupContent[j] += escape ;
        }
      }
		}
		return groupContent;
	}
}
