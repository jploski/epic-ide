package cbg.editor.rules;

import java.util.*;
import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class ColoringWhitespaceDetector implements IWhitespaceDetector {

	//	TODO EPIC workaround
	// static private Map whitespaces = new HashMap();  static private String whitespaces="";

	//	TODO Added by EPIC (workaround)
	 public void addWhiteSpaceChar(String whitespaceChar) {
		   whitespaces += whitespaceChar;
	 }

    public boolean isWhitespace(char c) {
		//	TODO Added by EPIC (workaround)
        //return Character.isWhitespace(c);
		return Character.isWhitespace(c) || (whitespaces.indexOf(c) >= 0);
    }

}
