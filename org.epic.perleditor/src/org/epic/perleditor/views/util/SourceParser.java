package org.epic.perleditor.views.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;

import org.epic.perleditor.editors.perl.PerlPartitionScanner;

import org.epic.perleditor.views.model.Model;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import gnu.regexp.RESyntax;

public class SourceParser {
	public static final String FUNCTION = "sub ";

	/**
	 * line separator
	 */
	public static final String LINE_SEPARATOR =
		System.getProperty("line.separator");

	public SourceParser() {
		//super();
	}

	public List getElements(
		String str,
		IToken tokenMatch,
		String suppressString,
		String invalidCharacters) {

		List funcList = new ArrayList();
		try {
			PerlPartitionScanner scanner = new PerlPartitionScanner();
			IDocument doc = new Document(str);
			scanner.setRange(doc, 0, doc.getLength());
			IToken token = scanner.nextToken();
			while (!token.isEOF()) {
				if (token.equals(tokenMatch)) {

					int offset = scanner.getTokenOffset();
					int length = scanner.getTokenLength();
					String foundName = str.substring(offset, offset + length);

					// Check the whole linefor invalid characters

					int currentLine = doc.getLineOfOffset(offset);
					int lineOffset = doc.getLineOffset(currentLine);

					String wholeLine =
						str.substring(
							lineOffset,
							lineOffset + doc.getLineLength(currentLine));
					boolean containsInvalidCharacters = false;
					char[] ic = invalidCharacters.toCharArray();

					for (int i = 0; i < ic.length; i++) {
						if (wholeLine.indexOf(ic[i]) != -1) {
							containsInvalidCharacters = true;
							break;
						}
					}

					if (containsInvalidCharacters) {
						token = scanner.nextToken();
						continue;
					}

					if (foundName.endsWith(suppressString)) {

						foundName =
							foundName.substring(0, foundName.length() - 1);
					}
					// The suppressString character has to be the last character in the string.
					// If not this is not a match.
					// Somehow it seems that the "end sequence" of the token is not checked
					// accordingly (at least not always). That's why the check is needed.
					// This seems only to be the case for ';' as ending character!
					else if (suppressString.equals(";")) {
						token = scanner.nextToken();
						continue;
					}

					foundName = foundName.trim();
					length = foundName.length();

					String naked = getNaked(foundName);

					if (naked != null) {
						Model func = new Model(naked, offset, length);

						funcList.add(func);
					}

				}
				token = scanner.nextToken();
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return funcList;
	}

	private String getNaked(String foundName) {
		if (foundName == null) {
			return null;
		}

		if (foundName.length() <= FUNCTION.length()) {
			return null;
		}

		foundName = foundName.trim().substring(FUNCTION.length());
		foundName = replaceInString(foundName.trim(), LINE_SEPARATOR, "");

		StringBuffer strBuf = new StringBuffer("");
		int len = foundName.length();
		boolean wasSpace = false;
		for (int i = 0; i < len; i++) {
			char ch = foundName.charAt(i);
			if (ch == ' ') {
				wasSpace = true;
			} else // not space
				{
				if (wasSpace) {
					strBuf.append(' ');
				}
				strBuf.append(ch);
				wasSpace = false;
			}
		}
		return strBuf.toString();
	}

	/**
	 * replace in a string a string sequence with another string sequence
	 */
	public static String replaceInString(
		String source,
		String whatBefore,
		String whatAfter) {
		if (null == source || source.length() == 0) {
			return source;
		}
		int beforeLen = whatBefore.length();
		if (beforeLen == 0) {
			return source;
		}
		StringBuffer result = new StringBuffer("");
		int lastIndex = 0;
		int index = source.indexOf(whatBefore, lastIndex);
		while (index >= 0) {
			result.append(source.substring(lastIndex, index));
			result.append(whatAfter);
			lastIndex = index + beforeLen;

			// get next
			index = source.indexOf(whatBefore, lastIndex);
		}
		result.append(source.substring(lastIndex));
		return result.toString();
	}

	/**
	 * Gets elements from sourcecode by using regular expressions.
	 * @param document
	 * @param regexp
	 * @param preFix
	 * @param postFix
	 * @param deleteComments
	 * @return
	 */
	public static List getElements(
		IDocument document,
		String regexp,
		String preFix,
		String postFix,
		boolean deleteComments) {
			String text = document.get();
			return getElements(text, regexp, preFix, postFix, deleteComments);
	}
	
	/**
	 * Gets elements from sourcecode by using regular expressions.
	 * @param text
	 * @param regexp
	 * @param preFix
	 * @param postFix
	 * @param deleteComments
	 * @return
	 */
	public static List getElements(
		String text,
		String regexp,
		String preFix,
		String postFix,
		boolean deleteComments) {
	  	return getElements(text,regexp,preFix,postFix,deleteComments,true);
	}
	
	/**
	 * Gets elements from sourcecode by using regular expressions.
	 * @param text
	 * @param regexp
	 * @param preFix
	 * @param postFix
	 * @param deleteComments
	 * @param posWordOnly   (postioning for word-only or for complete line)
	 * @return
	 */
	public static List getElements(
		String text,
		String regexp,
		String preFix,
		String postFix,
		boolean deleteComments,
		boolean posWordOnly) {
		List results = new ArrayList();

		Document docOrg = new Document(text);
		try {
			RE reg;

			// support all types of linebreaks on all platforms
			//TODO it would be fine to have user-option to set for the project the line-separator
			/* we assume a clever Linebreak-handling! 
			 * (i.e. if the line-break could be found => the whole file consists of this kind of line-break 
			 * 
			 * otherwise, it would be a Preference-setting! (which is currently not available)
			 * 
			 *reason for this, is that you can edit on a Windows-system Unix-files, which are treated 
			 *within Eclipse with Unix-separtors!
			 *and the effect is an incorrect positioning!
			 */
			
			RESyntax lineSep=new RESyntax(RESyntax.RE_SYNTAX_PERL5);
			if (text.indexOf(System.getProperty("line.separator")) == -1) {
			  if (text.indexOf("\r\n") > 0) {
			    lineSep.setLineSeparator("\r\n");
			  } else if (text.indexOf("\n") > 0) {
			    lineSep.setLineSeparator("\n");
			  } else if(text.indexOf("\r") > 0) {
			    lineSep.setLineSeparator("\r");
			  }
			  //no other possibility left for linesep, right?
			}

			// Remove POD and comments
			if (deleteComments) {
				// Remove POD
				reg = new RE("^(=.*)(("+ lineSep.getLineSeparator() +".*)+?)(" + lineSep.getLineSeparator() + "=cut)$", RE.REG_MULTILINE, lineSep);
				
				REMatch match;
				while((match = reg.getMatch(text)) != null ) {
					int newlines = match.toString().split(lineSep.getLineSeparator()).length;
					char[] nls= new char[newlines];
					for(int x =0; x < newlines; ++x)
						nls[x]='\n';
					
					String tmpText1 = text.substring(0, match.getStartIndex());
					String tmpText2 = text.length()-1 > match.getEndIndex() ? text.substring(match.getEndIndex()+1) : "";
					text = tmpText1 + match.substituteInto(new String(nls)) + tmpText2;
				}

				// Remove comments
				reg = new RE("[" + lineSep.getLineSeparator() + ");]\\s*(#.*)$");
				text = reg.substituteAll(text, lineSep.getLineSeparator());
			}
			
			reg = new RE(regexp, RE.REG_MULTILINE,lineSep);
			REMatch[] matches = reg.getAllMatches(text);

			for (int i = 0; i < matches.length; i++) {
				String match = null;
				int start;
				int end;
				
				if (reg.getNumSubs() > 0) {
					match = matches[i].toString(1);
					start = matches[i].getStartIndex(1);
					end = matches[i].getEndIndex(1);
				} else {
					match = matches[i].toString();
					start = matches[i].getStartIndex();
					end = matches[i].getEndIndex();
				}
				
				end -= match.length() - match.trim().length();
				match = match.trim();
				
				Model func;
				if (posWordOnly) {
				  /*
				   * the following lines is for matching the word only =>
				   * is required for Subs and Packages as well for Open Declaration
				   */
				  func = new Model(preFix + match + postFix, start, end - start);
				} else {
					/*
					 * the following line is for matching the whole line of the regexp. =>
					 * required for ??? problems with ???
					 */
					Document doc = new Document(text);
					int line = doc.getLineOfOffset(start);
					
					func = new Model(preFix + match + postFix, docOrg.getLineOffset(line), docOrg.getLineLength(line)-1);
				}
				results.add(func);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

}