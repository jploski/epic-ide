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
		List results = new ArrayList();

		try {
			RE reg;

			// Remove POD and comments
			if (deleteComments) {
				// Remove POD
				reg = new RE("^(=.*)((\\n.*)+?)(\\n=cut)$", RE.REG_MULTILINE);
				//text = reg.substituteAll(text, "");
				REMatch[] matches = reg.getAllMatches(text);
				for(int i=0; i < matches.length; i++) {
					REMatch match = matches[i];
					int newlines = match.toString().split("\n").length;
					char[] nls= new char[newlines];
					for(int x =0; x < newlines; ++x)
						nls[x]='\n';
						
					match.substituteInto(new String(nls));
				}

				// Remove comments
				reg = new RE("[\\n);]\\s*(#.*)$");
				text = reg.substituteAll(text, "\n");
			}
			// support all types of linebreaks on all platforms
			reg = new RE(regexp, RE.REG_MULTILINE);
			text =text.replaceAll("\r\n","\n");
			text =text.replaceAll("\r","\n");
			text =text.replaceAll("\n",System.getProperty("line.separator"));
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

				Model func =
					new Model(preFix + match + postFix, start, end - start);
				results.add(func);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

}