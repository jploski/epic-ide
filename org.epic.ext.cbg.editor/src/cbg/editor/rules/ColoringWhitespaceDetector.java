package cbg.editor.rules;

import java.util.*;
import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class ColoringWhitespaceDetector implements IWhitespaceDetector {
	
	// TODO EPIC workaround
	static private Map whitespaces = new HashMap();
    
	public ColoringWhitespaceDetector() {
			super();
	}
    
    // TODO Added by EPIC (workaround)
    public static void addWhiteSpaceChar(String whitespaceChar) {
    	  whitespaces.put(whitespaceChar, "");
    }

    public boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || whitespaces.get(String.valueOf(c)) != null;
    }

}
