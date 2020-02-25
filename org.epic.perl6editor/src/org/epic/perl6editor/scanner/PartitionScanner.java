package org.epic.perl6editor.scanner;

import org.eclipse.jface.text.rules.*;
import org.epic.perl6editor.rules.StatementRule;
import org.epic.perl6editor.rules.TagRule;

public class PartitionScanner extends RuleBasedPartitionScanner
{
    public final static String P6_COMMENT  = "__p6_comment";
    public final static String P6_TAG      = "__p6_tag";

    public PartitionScanner()
    {
        IToken perl_Comment  = new Token( P6_COMMENT );
        IToken tag           = new Token( P6_TAG );

        IPredicateRule[] rules = new IPredicateRule[4];

        rules[0] = new SingleLineRule( "#", "\n", perl_Comment );
        rules[1] = new MultiLineRule( "<!--", "-->", perl_Comment );
        rules[2] = new StatementRule( tag );
        rules[3] = new TagRule(tag);

		setPredicateRules( rules );
	}
}

// END