package org.epic.perleditor.editors.perl;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

import org.eclipse.jface.text.rules.PatternRule;

public class PerlPartitionScanner extends RuleBasedPartitionScanner {

	private final static String SKIP = "__skip";
	public final static String PERL_MULTI_LINE_COMMENT =
		"__perl_multiline_comment";
	public final static String PERL_SINGLE_LINE_COMMENT =
			"__perl_singleline_comment";
	public final static String PERL_POD_COMMENT =
				"__perl_pod_comment";
	public final static String PERL_SUBROUTINE = "__perl_subroutine";
	public final static String PERL_MODULE = "__perl_module";

	public final static IToken TOKEN_SUBROUTINE = new Token(PERL_SUBROUTINE);
	public final static IToken TOKEN_MODULE = new Token(PERL_MODULE);

	/**
	 * Detector for empty comments.
	 */
	static class EmptyCommentDetector implements IWordDetector {

		/* 
		* Method declared on IWordDetector
			*/
		public boolean isWordStart(char c) {
			return (c == '/');
		}

		/*
		* Method declared on IWordDetector
			*/
		public boolean isWordPart(char c) {
			return (c == '*' || c == '/');
		}
	}

	/**
	 * 
	 */
	static class WordPredicateRule extends WordRule implements IPredicateRule {

		private IToken fSuccessToken;

		public WordPredicateRule(IToken successToken) {
			super(new EmptyCommentDetector());
			fSuccessToken = successToken;
			addWord("/**/", fSuccessToken);
		}

		/*
		 * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(ICharacterScanner, boolean)
		 */
		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return super.evaluate(scanner);
		}

		/*
		 * @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()
		 */
		public IToken getSuccessToken() {
			return fSuccessToken;
		}
	}

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	public PerlPartitionScanner() {
		super();

		IToken comment = new Token(PERL_SINGLE_LINE_COMMENT);
		IToken pod = new Token(PERL_POD_COMMENT);
		IToken multilineComment = new Token(PERL_MULTI_LINE_COMMENT);

		List rules = new ArrayList();

		rules.add(new SingleLineRule("sub ", "{", TOKEN_SUBROUTINE));
		rules.add(new SingleLineRule("use ", ";", TOKEN_MODULE));

		// Add rule for single line comments.
		//rules.add(new EndOfLineRule("#", Token.UNDEFINED));
		rules.add(new EndOfLineRule("#", comment));
		

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\'));
		rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\'));
		
		// Add rules for multi-line pod comments.
		//MultiLineRule podRule1 = new MultiLineRule("=", "\n=cut\n", pod);
		/* This hopefully fixes bug [ e-p-i-c-Bugs-778262 ] =cut breaks Outline/subroutine list */
		MultiLineRule podRule1 = new MultiLineRule("=", "\n=", pod);
		podRule1.setColumnConstraint(0);
		rules.add(podRule1);
		
		/*
		MultiLineRule podRule2 = new MultiLineRule("=", "\n=pod\n", pod);
		podRule2.setColumnConstraint(0);
		rules.add(podRule2);
		*/

		// Handle __END__ statement
		rules.add(new PatternRule("__END__", "",  multilineComment, '\\', false, true));
       
       // Handle __DATA__ statement
	   rules.add(new PatternRule("__DATA__", "",  multilineComment, '\\', false, true));

		// Add special case word rule.
		//rules.add(new WordPredicateRule(comment));

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
	
	

}
