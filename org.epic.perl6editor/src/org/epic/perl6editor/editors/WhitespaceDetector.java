package org.epic.perl6editor.editors;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class WhitespaceDetector implements IWhitespaceDetector
{

	public boolean isWhitespace( char c )
	{
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}

// END