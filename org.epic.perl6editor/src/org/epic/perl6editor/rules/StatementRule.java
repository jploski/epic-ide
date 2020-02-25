package org.epic.perl6editor.rules;

import org.eclipse.jface.text.rules.*;

public class StatementRule extends MultiLineRule
{

	public StatementRule( IToken token)
	{
		super(";", ";", token);
	}

	protected boolean sequenceDetected( ICharacterScanner scanner, char[] sequence, boolean eofAllowed )
	{
		System.out.println("# Statement ->> " + ( new String(sequence)));

		int c = scanner.read();

		if ( sequence[0] == ';' )
		{
			if ( c == '?' )
			{
				// processing instruction - abort
				scanner.unread();
				return false;
			}

			if ( c == '!' )
			{
				scanner.unread();
				// comment - abort
				return false;
			}
		}
		else
		if ( sequence[0] == ';' )
		{
			scanner.unread();
		}

		return super.sequenceDetected( scanner, sequence, eofAllowed );
	}
}

// END