package cbg.editor.rules;

/**
 * This class extends the given Patternrule by using either CaseSensitive,
 * MultipleMatch, BracketMatch or Dynamic-Tagging. The given parameters 
 * specify the usage of this class more precesily.
 * 
 * @author LeO
 * @version .3
 * @change Feb, 04, 2005
 * TODO ????
 */

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.Token;
import cbg.editor.ColoringPartitionScanner;

public class ExtendedPatternRule extends PatternRule {
  private char openingBracket;
  private char closingBracket;
  private String[] groupContent;
  private boolean isExistingGroup, isBracketMatch, isMultiple, isCaseInSensitive, isDynamicTagging;
  private int myStepCounter = 0;
  private int noMultipleEndTag;
  private boolean requireEndTag, requireBeforeWhitespace, requireAfterWhitespace; 
	private final String optinalModifiers;
  private char curScannerChar;
  private final char EOFChar= (char) ICharacterScanner.EOF;
  private int noDynamicDelimiterChars=0;
  private final String countDelimterChars, requireBeforeTag, requireAfterTag, dynamicEndTerminate;
  private boolean continueCheck = true;
	private IWhitespaceDetector whiteSpace;
  
	public ExtendedPatternRule(String startSequence, String endSequence, IToken token, 
	       char escapeCharacter, boolean breaksOnEOL, int noMaxChar, String[] groupContent, 
	       boolean bracketMatch, int noMultipleEndTag, boolean requireEndTag, 
	       boolean CaseInSensitive, boolean isDynamicTagging,
	       String countDelimterChars, String beforeTag, String afterTag,
	       boolean  requireBeforeWhitespace, boolean requireAfterWhitespace, 
	   		String optinalModifiers,
	       IWhitespaceDetector whiteSpace) {
	  //the last parameter makes a default handling, 
	  //i.e. if the End-Tag is missing => mark till the end of File
	  super (startSequence, endSequence, token, escapeCharacter, breaksOnEOL,true);
	  
		this.isBracketMatch=bracketMatch;
		if (isBracketMatch) {
		  if (endSequence.indexOf("}") >= 0) {
		    closingBracket = '}';
		    openingBracket = '{';
		  }else if (endSequence.indexOf(")") >= 0) {
		    closingBracket = ')';
		    openingBracket = '(';
		  }else if (endSequence.indexOf("]") >= 0) {
		    closingBracket = ']';
		    openingBracket = '[';
		  }else if (endSequence.indexOf(">") >= 0) {
		    closingBracket = '>';
		    openingBracket = '<';
		  }
		}
		
		this.groupContent = groupContent;
		if (groupContent == null) {
		  isExistingGroup = false;
		} else {
		  isExistingGroup = true;
		}
		
		this.requireEndTag = requireEndTag;
		this.noDynamicDelimiterChars = noMaxChar;
		
		isCaseInSensitive = CaseInSensitive;
		//TODO check if it really required, i.e. lowercase = uppercase
	  dynamicEndTerminate = endSequence;
	  this.isDynamicTagging = isDynamicTagging;
	  if (isDynamicTagging) {
	    isCaseInSensitive = false; //case-sensitive is nonsense with dynamic Tags 
	  }
	  
	  this.countDelimterChars = countDelimterChars; //Programmers lazyness: we check only if content will exists!!!
	  this.requireBeforeTag = beforeTag;
	  this.requireAfterTag = afterTag;
	  this.requireBeforeWhitespace = requireBeforeWhitespace ;
	  this.requireAfterWhitespace = requireAfterWhitespace;
	  this.optinalModifiers = optinalModifiers;

		if (isCaseInSensitive) {
		  //rewrite the values for caseInSensitive!!!
		  fStartSequence= startSequence.toLowerCase().toCharArray();
		  fEndSequence= (endSequence == null ? new char[0] : endSequence.toLowerCase().toCharArray());
		}

		this.noMultipleEndTag = noMultipleEndTag;
		if (noMultipleEndTag > 1 || isDynamicTagging) {
		  isMultiple = true;
		}
		
		this.whiteSpace= whiteSpace;
	}
	
	/* Copied from my superclass and modified to support case sensitivity. */
	protected IToken doEvaluate(ICharacterScanner scanner, boolean resume) {
	  continueCheck = true;
	  IToken myResultToken=Token.UNDEFINED;
	  myStepCounter = 0;
	  if (resume) {
	    if (isBracketMatch || isMultiple) {
	      //we have to search back to the beginning of the partion and then start the scanning!!!  
	      unwindToStartToken(scanner);
	    } else {
	      if (endSequenceDetected(scanner)) {
	        return fToken;
	      } else {
	        continueCheck = false;
	      }
	    }
	  } else {
	    if (isDynamicTagging || requireBeforeWhitespace) {
	      if (((ColoringPartitionScanner) scanner).getOffset() > 0) {
	        scanner.unread();
	        curScannerChar = (char) scanner.read();
	        if (!whiteSpace.isWhitespace(curScannerChar)) {
	          //we do not check anything, since the leading char before this is not
	          //whitespace or equivalent, so we could assume a single keyword for 
	          //dynamic tagging!!!
	          continueCheck = false;
	        }
	      }
	    }
	  }
	  if (continueCheck) {
	    if (isExistingGroup) {
	      if (forwardStartSequenceDetected(scanner)) {
	        if (endCheck(scanner, resume)) {
	          if (optinalModifiers.length() > 0) {
	            /*
	             * We have already found the char, we only search forward for optional
	             * modifiers
	             */
	        	  curScannerChar = (char) scanner.read();
	        	  while (curScannerChar != EOFChar 
	        	        && optinalModifiers.indexOf( curScannerChar ) >=0) {
	        	    curScannerChar = (char) scanner.read();
	        	  }
	        	  scanner.unread() ;
	          }
            return fToken;
	        }
	      }
	    } else {
	      //check from the regular startSequence
	      curScannerChar= (char) scanner.read();
	      myStepCounter++;
	      if (isCaseInSensitive) {
	        curScannerChar = Character.toLowerCase(curScannerChar);
	      }
	      if (curScannerChar == fStartSequence[0]) {
	        if (sequenceDetected(scanner, fStartSequence, fBreaksOnEOF)) {
	          if (endCheck(scanner, resume)) {
	            return fToken;
	          }
	        }
	      }
	    }
	  }
	  unwindScanner(scanner);
	  return myResultToken;
	}
	
	/**
	 * This method is mainly for simple handling of the doEvaluate-issue
	 */
	private final boolean endCheck(ICharacterScanner scanner, boolean resume) {
    //we have also to check if after the found Start-tag it is required to have Whitespace
    if (requireAfterWhitespace) {
      curScannerChar = (char) scanner.read();
      scanner.unread();
      if (curScannerChar == EOFChar || !whiteSpace.isWhitespace(curScannerChar)) {
        return false;
      } 
    }

    boolean myIsBracketMatch=isBracketMatch;
    if (isDynamicTagging) {
      myIsBracketMatch = retrieveDynamicEndTag(scanner);
      if (fEndSequence.length == 0) {
        return false;
      }
    }
    if (isBracketMatch || myIsBracketMatch) {
      if (resume) {
	      //we have to search back to the beginning of the partion and then start the scanning!!!  
	      unwindToStartToken(scanner);
      } else {
        //rewind the scanner, so we can also take care about brackets in the fStartSequence
        unwindScanner(scanner);
      }
    } 
    if (isBracketMatch) {
      return (endBracketSequenceDetected(scanner) && myStepCounter >= 0);
    } else if (isMultiple) {
      return (multipleEndSequenceDetected(scanner, myIsBracketMatch) && myStepCounter >= 0);
    } else if (myIsBracketMatch) {
      //from dynamic with only one 
      return (endBracketSequenceDetected(scanner) && myStepCounter >= 0);
    } else {      
      return (endSequenceDetected(scanner) && myStepCounter >= 0);
    }
	}
	
	private final boolean retrieveDynamicEndTag(ICharacterScanner scanner) {
	  StringBuffer tmpEnd=new StringBuffer();
	  curScannerChar = (char) scanner.read();
	  myStepCounter++;
	  int thisCounter = noDynamicDelimiterChars;
	  while (Character.isWhitespace(curScannerChar) && curScannerChar != EOFChar) {
	    curScannerChar = (char) scanner.read();
	    myStepCounter++;
	  }
	  if (requireBeforeTag.length() > 0) {
	    if (requireBeforeTag.charAt(0) !=curScannerChar) {
	      fEndSequence = "".toCharArray();
	      return false;
	    } else {
	      curScannerChar = (char) scanner.read();
	      myStepCounter++;
	    }
	  }
	  boolean previousCharWasEscape=false;
	  while (--thisCounter >= 0 &&
	         !Character.isWhitespace(curScannerChar) && 
	         !(countDelimterChars.length() == 0 
	            && Character.isLetterOrDigit(curScannerChar)) &&
	         curScannerChar != EOFChar
	        ) {
	    if (curScannerChar == fEscapeCharacter) {
	      previousCharWasEscape = true;
	    } else if (dynamicEndTerminate.indexOf(curScannerChar) < 0) {
	      if (previousCharWasEscape) {
	        if (requireBeforeTag.length() == 0 || 
	            requireBeforeTag.charAt(0) != curScannerChar) {
	          //we only insert fEscape + curScannerChar when  
	          //requireBeforeTag != curScannerChar (= the Escape marks the requireBeforeTag)
	          tmpEnd.append(fEscapeCharacter);
	        }
	        tmpEnd.append(curScannerChar);
	        previousCharWasEscape = false;
	      } else {
	        tmpEnd.append(curScannerChar);
	      }
	    } else if (previousCharWasEscape && requireBeforeTag.length() >0 
	                 && requireBeforeTag.charAt(0) == curScannerChar) {
        tmpEnd.append(curScannerChar);
        previousCharWasEscape = false;
	    } else {
	      break;
	    }
	    curScannerChar = (char) scanner.read();
	    myStepCounter++;
	  }
	  
	  if (previousCharWasEscape) {
	    tmpEnd.append(fEscapeCharacter);
	  }
	  
	  scanner.unread();
	  myStepCounter--;
	  if (tmpEnd.length() == 0 && countDelimterChars.length() > 0 ) {
	    //Transform the empty string only if countDelimterChars 
	    tmpEnd.append(((ColoringPartitionScanner) scanner).getCurrentLineDelimiter());
	  }
	  
	  if (requireAfterTag.length() > 0) {
	    /* TODO 
	     * currently we assume it is :LINEFEED: what should be appended after the Tag
	     * IF for any reason it should be something else then :LINEFEED: it has to coded
	     * here!
	     */
	    tmpEnd.append(((ColoringPartitionScanner) scanner).getCurrentLineDelimiter());
	  }
	  fEndSequence =  tmpEnd.toString().toCharArray();
	  if (fEndSequence.length == 1) {
	    if (fEndSequence[0] == '{') {
	      openingBracket = '{';
	      closingBracket  = '}';
	      return true;
	    } else if (fEndSequence[0] == '(') {
	      openingBracket = '(';
	      closingBracket = ')';
	      return true;
	    } else if (fEndSequence[0] == '[') {
	      openingBracket = '[';
	      closingBracket = ']';
	      return true;
	    } else if (fEndSequence[0] == '<') {
	      openingBracket = '<';
	      closingBracket = '>';
	      return true;
	    } 
	  }
	  return false;
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

	private final void unwindToStartToken(ICharacterScanner scanner) {
	  int  scannerOffset =  ((ColoringPartitionScanner) scanner).getOffset(); 
	  int  tokenOffset =  ((ColoringPartitionScanner) scanner).getTokenOffset(); 
	  for (int i= scannerOffset - tokenOffset; i> 0; i--) {
	    scanner.unread();
	    myStepCounter--;
	  }
	  
	  if (isMultiple) {
	    //forward the scanner after the detected Sequence, to prevent counting the chars in StartSequence
	    if (isExistingGroup) {
	      forwardStartSequenceDetected(scanner);
	    } else {
	      for (int i=fStartSequence.length ; i >0; i--) {
	        scanner.read();
	        myStepCounter++;
	      }
	    }
	  }
	}


	/**
   * @param scanner
   */
  public boolean multipleEndSequenceDetected(ICharacterScanner scanner, boolean dynamicBrackets) {
    int count= noMultipleEndTag;
    boolean resultEndSearch=true;
    while (--count >= 0 && resultEndSearch) {
      if (dynamicBrackets) {
        resultEndSearch = endBracketSequenceDetected(scanner);
      } else {
        resultEndSearch  = endSequenceDetected(scanner);
      }
    }
    if (count < 0) {
      return true;
    }
    return false;
  }
	
	/** 
	 * Search a matching Bracket from the current scanner-postion. Ignore a Bracket with leading
	 * Escape-Char.<p>
	 * Note: Bracket are NOT case-senstive!!!
	 * 
	 * @author LeO
	 * @param scanner
	 * @return true => found the matching Brackets
	 * @since 11 Nov 2004
	 * @version 0.2
	 *      escape-Sequence => only one char to skip and not two!
	 */

	protected boolean endBracketSequenceDetected(ICharacterScanner scanner) {
		int pairs = 0;
		char[][] delimiters= scanner.getLegalLineDelimiters();
		while ((curScannerChar=(char) scanner.read()) != EOFChar) {
		  myStepCounter++;
			if (curScannerChar == fEscapeCharacter) {
				// Skip the escaped character.
			  curScannerChar=(char) scanner.read();
				if (curScannerChar == EOFChar) {
				  return false;
				}
				myStepCounter++;
			} else {
			  if (curScannerChar == closingBracket) {
			    if (--pairs == 0) {
			      return true;
			    }
			  } else if (curScannerChar == openingBracket){
			    pairs++;
			  } else if (fBreaksOnEOL) {
			    // Check for end of line since it can be used to terminate the pattern.
			    for (int i= 0; i < delimiters.length; i++) {
			      if (curScannerChar == delimiters[i][0] && sequenceDetected(scanner, delimiters[i], fBreaksOnEOF))
			        return !requireEndTag;
			    }
			  }
			}
		}
		return false;
	}

	
  /*
   * mainly from the super-class except a counter was introduced!
   */
  
  protected boolean endSequenceDetected(ICharacterScanner scanner) {
    curScannerChar= (char) scanner.read();
    if (isCaseInSensitive) {
      curScannerChar = Character.toLowerCase(curScannerChar);
    }
		++myStepCounter;
		
	//	char[][] delimiters= scanner.getLegalLineDelimiters();
		char[] lineDelimiter=((ColoringPartitionScanner) scanner).getCurrentLineDelimiter().toCharArray(); 
		boolean previousWasEscapeCharacter = false;	
		while (curScannerChar != EOFChar) {
			if (curScannerChar == fEscapeCharacter) {
				// Skip the escaped character.
				curScannerChar = (char) scanner.read();
				if (curScannerChar == EOFChar) {
				  return false;
				}
				++myStepCounter;
				previousWasEscapeCharacter = true;
			} else if (fEndSequence.length > 0 && curScannerChar == fEndSequence[0]) {
				// Check if the specified end sequence has been found.
				if (sequenceDetected(scanner, fEndSequence, fBreaksOnEOF)) {
				  return true;
				}
			} else if (fBreaksOnEOL) {
				// Check for end of line since it can be used to terminate the pattern.
		//		for (int i= 0; i < delimiters.length; i++) {
					if (curScannerChar == lineDelimiter[0] && sequenceDetected(scanner, lineDelimiter, fBreaksOnEOF)) {
						if (!fEscapeContinuesLine || !previousWasEscapeCharacter)
							return !requireEndTag;
		//			}
				}
				 previousWasEscapeCharacter = false;
			}
      curScannerChar= (char) scanner.read();
      if (isCaseInSensitive) {
        curScannerChar = Character.toLowerCase(curScannerChar);
      }
			++myStepCounter;
		}
		return fBreaksOnEOF;
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