package cbg.editor.rules;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.rules.IWordDetector;

public class ColoringWordDetector implements IWordDetector {
	protected Set charMap = new HashSet();
    public boolean isWordStart(char c) {
    	return Character.isLetter(c) || '_' == c || isTextSequencePart(c);
    }

	private boolean isTextSequencePart(char c) {
		return charMap.contains(new Integer(c));
	}

	public void addWord(String word) {
		charMap.add(new Integer(word.charAt(0)));
	}

    public boolean isWordPart(char c) {
    	/* added the dot so properties file would mark_following correctly
    	 * when they path contained dots. For example a.b.c=124    	 */
		return isWordStart(c) || Character.isDigit(c) || '.' == c;
    }
}
