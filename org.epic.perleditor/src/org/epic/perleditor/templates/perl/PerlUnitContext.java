/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates.perl;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.epic.perleditor.templates.*;

/**
 * A compilation unit context.
 */
public class PerlUnitContext extends DocumentTemplateContext
{
    /** The platform default line delimiter. */
    private static final String PLATFORM_LINE_DELIMITER =
        System.getProperty("line.separator"); //$NON-NLS-1$

    // To allow templates starting with "= # $ @ %"
    private static final String specialChars = "=#$@%<";

    /**
     * Creates a compilation unit context.
     * 
     * @param type
     *            the context type.
     * @param document
     *            the document.
     * @param completionPosition
     *            the completion position within the document.
     */
    protected PerlUnitContext(
        ContextType type,
        IDocument document,
        int completionPosition)
    {
        super(type, document, completionPosition, 0);
    }

    public boolean canEvaluate(Template template)
    {
        return template.matches(getKey(), getContextType().getName());
    }

    /**
     * Returns <code>true</code> if template matches the prefix and context,
     * <code>false</code> otherwise.
     */
    public boolean canEvaluate(String identifier, boolean showAllOnEmpty)
    {
        String prefix = getKey();

        if (showAllOnEmpty)
        {
            return
                prefix.length() == 0 ||
                identifier.toLowerCase().startsWith(prefix.toLowerCase());
        }
        else
        {
            return
                prefix.length() != 0 &&
                identifier.toLowerCase().startsWith(prefix.toLowerCase());
        }
    }

    public TemplateBuffer evaluate(Template template) throws CoreException
    {
        if (!canEvaluate(template)) return null;

		// make sure line delimiters match to what's supposed to be used in the document.
        String pattern = template.getPattern();
		pattern = pattern.replaceAll("\\r\\n|\\r|\\n", TextUtilities.getDefaultLineDelimiter(this.getDocument()));

        TemplateTranslator translator = new TemplateTranslator();
		TemplateBuffer buffer = translator.translate(pattern);

        getContextType().edit(buffer, this);

        String lineDelimiter = null;
        try
        {
            lineDelimiter = getDocument().getLineDelimiter(0);
        }
        catch (BadLocationException e)
        {
        }

        if (lineDelimiter == null) lineDelimiter = PLATFORM_LINE_DELIMITER;

        return buffer;
    }

    public int getStart()
    {
        IDocument document = getDocument();
        try
        {
            int start = getCompletionOffset();

            while (
                (start != 0 &&
                 Character.isUnicodeIdentifierPart(document.getChar(start-1))) ||
                 (start != 0 && specialChars.indexOf(document.getChar(start-1)) != -1))
            {
                start--;
            }

            if ((start != 0 && Character.isUnicodeIdentifierStart(document.getChar(start - 1))) ||
                (start != 0 && specialChars.indexOf(document.getChar(start - 1)) != -1))
            {
                start--;
            }
            return start;
        }
        catch (BadLocationException e)
        {
            return getCompletionOffset();
        }
    }

    /**
     * Returns the character before start position of completion.
     */
    public char getCharacterBeforeStart()
    {
        int start = getStart();

        try
        {
            return start == 0 ? ' ' : getDocument().getChar(start - 1);
        }
        catch (BadLocationException e)
        {
            return ' ';
        }
    }
}
