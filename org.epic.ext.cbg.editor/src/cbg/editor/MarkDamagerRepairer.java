package cbg.editor;

import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import cbg.editor.rules.LToken;

public class MarkDamagerRepairer extends DefaultDamagerRepairer {

	/**
	 * 
	 * @see org.eclipse.jface.text.rules.DefaultDamagerRepairer#DefaultDamagerRepairer(ITokenScanner, TextAttribute)
	 * @deprecated 
	 */
	public MarkDamagerRepairer(ITokenScanner scanner, TextAttribute defaultTextAttribute) {
		super(scanner, defaultTextAttribute);
	}

	public MarkDamagerRepairer(ITokenScanner scanner) {
		super(scanner);
	}

	public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
		if (fScanner == null) {
			// will be removed if deprecated constructor will be removed
			addRange(presentation, damage.getOffset(), damage.getLength(), fDefaultTextAttribute);
			return;
		}
		
		int lastStart= damage.getOffset();
		int length= 0;
		IToken lastToken= Token.UNDEFINED;
		TextAttribute lastAttribute= getTokenTextAttribute(lastToken);
		
		fScanner.setRange(fDocument, lastStart, damage.getLength());
		
		while (true) {
			IToken token= fScanner.nextToken();			
			if (token.isEOF())
				break;
			
			TextAttribute attribute= getTokenTextAttribute(token);			
			if (lastAttribute != null && lastAttribute.equals(attribute)) {
				length += fScanner.getTokenLength();
			} else {
				addRange(presentation, lastStart, length, lastAttribute);
				lastToken= token;
				lastAttribute= attribute;
				lastStart= fScanner.getTokenOffset();
				length= fScanner.getTokenLength();						    
			}
		}
		int delta = 0;
		int offset = 0;
		if(lastToken instanceof LToken) {
			LToken token = (LToken)lastToken;
			delta = token.isPrevious() ? token.getLength() : 0;
			offset = token.isPrevious() ? 0 : token.getLength();
		}
		addRange(presentation, lastStart + offset, length - delta, lastAttribute);
	}

}
