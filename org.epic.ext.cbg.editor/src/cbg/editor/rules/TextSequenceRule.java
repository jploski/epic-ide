package cbg.editor.rules;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import cbg.editor.ColoringPartitionScanner;

public class TextSequenceRule extends Object implements IPredicateRule {/**
 * An implementation of <code>IRule</code> capable of detecting words
 * Word rules also allow for the association of tokens with specific words. 
 * That is, not only can the rule be used to provide tokens for exact matches, 
 * but also for the generalized notion of a word in the context in which it is used.
 * A word rules uses a word detector to determine what a word is.
 *
 * @see IWordDetector
 */
	protected static final int UNDEFINED= -1;
	
	/** The word detector used by this rule */
	protected IWordDetector fDetector;
	/** The token to be returned on success */
	protected IToken token;
	/** The column constraint */
	protected int fColumn= UNDEFINED;
	protected char[] word;
	protected boolean isCaseInSensitive;
	private StringBuffer fBuffer= new StringBuffer();

  private String[] groupContent;
  private boolean isExistingGroup;
  private char curScannerChar;
  private int myStepCounter = 0;
  private final char EOFChar= (char) ICharacterScanner.EOF;
	private IWhitespaceDetector whiteSpace;
	private boolean isNotSequenceWhitespace;


	public TextSequenceRule(String wordToMatch, String[] groupContent, IToken token, 
	                        boolean ignoreCase, IWhitespaceDetector whiteSpace,
	                        boolean isNotSequenceWhitespace) {
		this.token = token;
		word = (ignoreCase ? wordToMatch.toLowerCase().toCharArray() : wordToMatch.toCharArray());
		this.isCaseInSensitive = ignoreCase;
		
		this.groupContent = groupContent;
		if (groupContent == null) {
		  isExistingGroup = false;
		} else {
		  isExistingGroup = true;
		}
		this.whiteSpace= whiteSpace;
		this.isNotSequenceWhitespace = isNotSequenceWhitespace;

	}
	
	/**
	 * Sets a column constraint for this rule. If set, the rule's token
	 * will only be returned if the pattern is detected starting at the 
	 * specified column. If the column is smaller then 0, the column
	 * constraint is considered removed.
	 *
	 * @param column the column in which the pattern starts
	 */
	public void setColumnConstraint(int column) {
		if (column < 0)
			column= UNDEFINED;
		fColumn= column;
	}
	
	/**
	 * Returns the characters in the buffer to the scanner.
	 *
	 * @param scanner the scanner to be used
	 */
	protected void unreadBuffer(ICharacterScanner scanner) {
		for (int i= fBuffer.length() - 1; i >= 0; i--)
			scanner.unread();
	}
	
  /* (non-Javadoc)
   * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner, boolean)
   */
  public IToken evaluate(ICharacterScanner scanner, boolean resume) {
    // TODO Auto-generated method stub
    return evaluate(scanner, false);
  }

	public IToken evaluate(ICharacterScanner scanner) {
		if (fColumn == UNDEFINED)
			return doEvaluateFinally(scanner);
		
		int c = scanner.read();
		scanner.unread();
		if (c == word[0])
			return (fColumn == scanner.getColumn() ? doEvaluateFinally(scanner) : Token.UNDEFINED);
		else
			return Token.UNDEFINED;	
	}
	/**
	 * Evaluates this rules without considering any column constraints.
	 *
	 * @param scanner the character scanner to be used
	 * @return the token resulting from this evaluation
	 */
	protected IToken doEvaluate(ICharacterScanner scanner) {
		return doEvaluate(scanner, false);
	}

	protected IToken doEvaluate(ICharacterScanner scanner, boolean resume) {
	  return doEvaluateFinally(scanner);
	}
	
	/**
	 * Same code as in ExtendePatternRule
	 */
	private final IToken doEvaluateFinally(ICharacterScanner scanner) {
	  //boolean continueCheck=true;
	  
	  if (isNotSequenceWhitespace) {
	    if (((ColoringPartitionScanner) scanner).getOffset() > 0) {
	      scanner.unread();
	      curScannerChar = (char) scanner.read();
	      if (!whiteSpace.isWhitespace(curScannerChar)) {
	        //we do not check anything, since the leading char before this is not
	        //whitespace or equivalent
	        //BUT only if the current char is not already a Whitespace!!!
	        //for speed improvements we return immediately
	        // continueCheck = false;
	        return Token.UNDEFINED; 
	      }
	    }
	  }
	  
	  myStepCounter = 0;

	    if (isExistingGroup) {
	      if (forwardStartSequenceDetected(scanner)) {
	  	    curScannerChar= (char) scanner.read();
	  	    scanner.unread();
	  	    if (!isNotSequenceWhitespace || whiteSpace.isWhitespace(curScannerChar)) {
	  	      /* We only return the token,
	  	       * - either the Sequence is Whitespace 
	  	       * - or the Keyword has also an Whitespace-Ending! 
	  	       */
	  	      return token;
	  	    }
	      }
	    } else {
	      //check from the regular startSequence
	      curScannerChar= (char) scanner.read();
	      myStepCounter++;
	      if (isCaseInSensitive) {
	        curScannerChar = Character.toLowerCase(curScannerChar);
	      }
	      if (curScannerChar == word[0]) {
	        if (sequenceDetected(scanner, word, true)) {
		  	    curScannerChar= (char) scanner.read();
		  	    scanner.unread();
		  	    if (!isNotSequenceWhitespace || whiteSpace.isWhitespace(curScannerChar)) {
		  	      /* We only return the token,
		  	       * - either the Sequence is Whitespace 
		  	       * - or the Keyword has also an Whitespace-Ending! 
		  	       */
		  	      return token;
		  	    }
	        }
	      }
	    }
	  
	  if (myStepCounter != 0) {  
	    unwindScanner(scanner);
	  }
	  return Token.UNDEFINED;
	}	
	
	public IToken getSuccessToken() {
		return token;
	}
	
	private final boolean forwardStartSequenceDetected(ICharacterScanner scanner) {
	  StringBuffer c = new StringBuffer();
	  int i=0;
	  int j=0;
	  int elementSize=0;
	  elementSize = groupContent.length - 1;
	  
	  String escape= groupContent[0];
	  c.append(escape);
	
	  while (i++ <= elementSize) {
	    curScannerChar= (char) scanner.read();
	    myStepCounter++;
	    if (curScannerChar == EOFChar) {
	      return false;
	    }
	    if (isCaseInSensitive) {
	      curScannerChar = Character.toLowerCase(curScannerChar);
	    }
	    c.append(curScannerChar);
	    if (groupContent[i].indexOf(c.toString()) >= 0) {
	      return true;
	    } else if (i == elementSize) {
	      return false;
	    } else {
	      j= i+1;
	      while(j < elementSize && groupContent[j].indexOf(c.toString()) < 0) 
	      {
	        j++;
	      }
	      if (j < elementSize || groupContent[j].indexOf(c.toString()) >= 0) {
	        for (int k = j-i-1; k> 0; k--) {
	          curScannerChar= (char) scanner.read();
	          myStepCounter++;
	    	    if (curScannerChar == EOFChar) {
	    	      return false;
	    	    }
	    	    if (isCaseInSensitive) {
	    	      curScannerChar = Character.toLowerCase(curScannerChar);
	    	    }
	          c.append(curScannerChar);
	          i++;
	        }
	      } else {
	        return false;
	      }
	    }
	  }
	  return false;
	}
	 
 //copied from the superclass to provide a counter, what has read
	protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {
		for (int i= 1; i < sequence.length; i++) {
      curScannerChar= (char) scanner.read();
			myStepCounter++;
      if (isCaseInSensitive) {
        curScannerChar = Character.toLowerCase(curScannerChar);
      }
			if (curScannerChar == EOFChar && eofAllowed) {
				return true;
			} else if (curScannerChar != sequence[i]) {
				return false;
			}
		}
		return true;
	}

	/*
   * unwind the scanner to the orginal position
   */
  
  private final void unwindScanner(ICharacterScanner scanner) {
    if (myStepCounter < 0) {
      for (; myStepCounter < 0; myStepCounter++ )
        scanner.read();
    } else {
      for (; myStepCounter > 0; myStepCounter--)
        scanner.unread();
    }
  }
}
