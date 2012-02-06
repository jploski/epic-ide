import antlr.ParserSharedInputState;
import antlr.RecognitionException;
import antlr.TokenBuffer;
import antlr.TokenStream;

/*
 * Created on 31.01.2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlParser extends PerlParserSimple {

	int mHasError = 0;
	/**
	 * @param tokenBuf
	 * @param k
	 */
	public PerlParser(TokenBuffer tokenBuf, int k) {
		super(tokenBuf, k);
		
		
	}

	/**
	 * @param tokenBuf
	 */
	public PerlParser(TokenBuffer tokenBuf) {
		super(tokenBuf);
		
	}

	
	public PerlParser(TokenStream lexer, int k) {
		super(lexer, k);
		}

	
	public PerlParser(TokenStream lexer) {
		super(lexer);
		}

	/**
	 * @param state
	 */
	public PerlParser(ParserSharedInputState state) {
		super(state);
		
	}

	
	/* (non-Javadoc)
	 * @see antlr.Parser#reportError(antlr.RecognitionException)
	 */
	public void reportError(RecognitionException arg0) {
		mHasError++;
		super.reportError(arg0);
	}
	
	public int hasError()
	{
		return(mHasError);
	}
}
