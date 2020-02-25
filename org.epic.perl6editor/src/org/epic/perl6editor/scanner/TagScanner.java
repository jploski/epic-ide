package org.epic.perl6editor.scanner;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import org.epic.perl6editor.editors.ColorManager;
import org.epic.perl6editor.editors.IColorConstants;
import org.epic.perl6editor.editors.WhitespaceDetector;

public class TagScanner extends RuleBasedScanner
{

    public TagScanner( ColorManager manager )
    {
        IToken string =
            new Token(
                new TextAttribute( manager.getColor( IColorConstants.STRING )));

        IRule[] rules = new IRule[3];

        // Add rule for double quotes
        rules[0] = new SingleLineRule( "\"", "\"", string, '\\' );
        // Add a rule for single quotes
        rules[1] = new SingleLineRule( "'", "'", string, '\\' );
        // Add generic whitespace rule.
        rules[2] = new WhitespaceRule( new WhitespaceDetector());

        setRules( rules );
    }
}

// END