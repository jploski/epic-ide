package org.epic.perleditor.actions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.*;
import org.epic.core.model.SourceFile;
import org.epic.perleditor.editors.PartitionTypes;

/**
 * Attempts to find and open the declaration of a symbol that is referenced
 * through the arrow operator, i.e. <code>Some::Class-&gt;CONSTANT</code> or
 * <code>Some::Class-&gt;method</code>.
 * <p>
 * The Perl lexer folds <code>::</code>-separated names into a single token
 * but treats <code>-&gt;</code> as a separate operator token, so an access
 * such as <code>Some::Class-&gt;CONSTANT</code> is lexed as three tokens:
 * <code>Some::Class</code>, <code>-&gt;</code> and <code>CONSTANT</code>.
 * The generic open-declaration logic therefore only ever sees the bare
 * <code>CONSTANT</code> (or <code>method</code>) without any package context,
 * which is not enough to locate its declaration.
 * <p>
 * This class recovers the package from the token to the left of the arrow
 * operator. Once the base class has located the corresponding module file,
 * the symbol is resolved within it using the following heuristics:
 * <ol>
 * <li>If the symbol name consists solely of uppercase letters, digits and
 *     underscores, it is treated as a constant and the module is searched
 *     for a <code>use constant</code> statement declaring it.</li>
 * <li>Otherwise (and if no constant declaration has been found) it is
 *     treated as a class method and the module is searched for a
 *     <code>sub</code> declaration.</li>
 * </ol>
 *
 * @author jploski
 */
class OpenArrowDeclaration extends AbstractOpenDeclaration
{
    //~ Constructors

    public OpenArrowDeclaration(OpenDeclarationAction action)
    {
        super(action);
    }
    
    //~ Methods

    protected IRegion findDeclaration(SourceFile sourceFile, String name)
    {
        if (sourceFile == null) return null;
        IDocument doc = sourceFile.getDocument();
        if (doc == null) return null;

        String text = doc.get();

        if (isConstantName(name))
        {
            IRegion region = findConstantDeclaration(text, name);
            if (region != null) return region;
        }
        return findSubDeclaration(text, name);
    }
    
    protected String getLocalSearchString(String searchString)
    {
        int lastSepIndex = searchString.lastIndexOf("::");
        return lastSepIndex != -1
            ? searchString.substring(lastSepIndex + 2) : null;
    }
    
    protected String getSearchString(ITextSelection selection)
    {
        // Note that we rely heavily on the correct partitioning delivered
        // by PerlPartitioner. When in doubt, fix PerlPartitioner instead of
        // adding workarounds here.

        IDocument doc = getSourceDocument();

        try
        {
            ITypedRegion symbol = PartitionTypes.getPerlPartition(
                doc, selection.getOffset());
            if (!PartitionTypes.DEFAULT.equals(symbol.getType())) return null;

            String symbolName = doc.get(symbol.getOffset(), symbol.getLength());
            if (!isIdentifier(symbolName)) return null;

            ITypedRegion arrow = previousNonWhitespaceToken(
                doc, symbol.getOffset());
            if (arrow == null || !isArrowOperator(arrow, doc)) return null;

            ITypedRegion lhs = previousNonWhitespaceToken(doc, arrow.getOffset());
            if (lhs == null) return null;

            String packageName = doc.get(lhs.getOffset(), lhs.getLength());
            if (!isPackagePath(packageName)) return null;

            return packageName + "::" + symbolName;
        }
        catch (BadLocationException e)
        {
            return null; // should never happen
        }
    }
    
    protected String getTargetModule(String searchString)
    {
        int lastSepIndex = searchString.lastIndexOf("::");
        return lastSepIndex != -1 ? searchString.substring(0, lastSepIndex) : null;
    }

    //~ Helpers

    /**
     * Searches for a declaration of the given constant in a
     * 'use constant' statement of the given source text.
     * <p>
     * The constant name is required to be followed by '=>' so that it is
     * matched as a key of the 'use constant' list rather than as a value.
     */
    private static IRegion findConstantDeclaration(String text, String name)
    {
        Pattern pattern = Pattern.compile(
            "\\buse\\s+constant\\b[^;]*?\\b(" + Pattern.quote(name) + ")\\s*=>",
            Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? nameRegion(matcher) : null;
    }

    /**
     * Searches for a 'sub' declaration with the given name in the given
     * source text.
     */
    private static IRegion findSubDeclaration(String text, String name)
    {
        Pattern pattern = Pattern.compile(
            "\\bsub\\s+(" + Pattern.quote(name) + ")\\b");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? nameRegion(matcher) : null;
    }

    /**
     * @return a region covering the first capture group of the given match
     */
    private static IRegion nameRegion(Matcher matcher)
    {
        return new Region(
            matcher.start(1), matcher.end(1) - matcher.start(1));
    }

    /**
     * @return true if the given partition is the arrow operator
     */
    private static boolean isArrowOperator(ITypedRegion region, IDocument doc)
        throws BadLocationException
    {
        if (!PartitionTypes.OPERATOR.equals(region.getType())) return false;
        return "->".equals(
            doc.get(region.getOffset(), region.getLength()).trim());
    }

    /**
     * @return true if the given name consists solely of uppercase letters,
     *         digits and underscores, i.e. looks like a constant name
     */
    private static boolean isConstantName(String name)
    {
        if (name == null || name.length() == 0) return false;
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (!(Character.isUpperCase(c) || Character.isDigit(c) || c == '_'))
                return false;
        }
        return true;
    }

    /**
     * @return true if the given string is a valid Perl identifier
     */
    private static boolean isIdentifier(String s)
    {
        if (s == null || s.length() == 0) return false;
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (i == 0)
            {
                if (!(Character.isLetter(c) || c == '_')) return false;
            }
            else if (!(Character.isLetterOrDigit(c) || c == '_')) return false;
        }
        return true;
    }

    /**
     * @return true if the given string is a valid package path, i.e. one or
     *         more identifiers separated by double colons
     */
    private static boolean isPackagePath(String s)
    {
        if (s == null || s.length() == 0) return false;
        String[] parts = s.split("::", -1);
        for (int i = 0; i < parts.length; i++)
        {
            if (!isIdentifier(parts[i])) return false;
        }
        return true;
    }

    /**
     * @return the first non-whitespace partition located strictly before
     *         the given offset, or null if there is none
     */
    private static ITypedRegion previousNonWhitespaceToken(
        IDocument doc, int offset) throws BadLocationException
    {
        int pos = offset - 1;
        while (pos >= 0)
        {
            ITypedRegion partition = PartitionTypes.getPerlPartition(doc, pos);
            if (partition.getLength() > 0)
            {
                String text = doc.get(
                    partition.getOffset(), partition.getLength());
                if (text.trim().length() > 0) return partition;
            }
            pos = partition.getOffset() - 1;
        }
        return null;
    }
}
