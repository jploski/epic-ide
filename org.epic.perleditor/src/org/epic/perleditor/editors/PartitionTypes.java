package org.epic.perleditor.editors;

import org.eclipse.jface.text.*;

/**
 * Types of partitions that can appear in a Perl document,
 * also referred to as ITypedRegions and "content types".
 * 
 * @author jploski
 */
public class PartitionTypes
{
    /**
     * Name of the document partitioning managed by PerlPartitioner.
     */
    public static final String PERL_PARTITIONING = "org.epic.perleditor.perlPartitioning";

    public final static String DEFAULT = IDocument.DEFAULT_CONTENT_TYPE;
    public final static String COMMENT = "COMMENT";
    public final static String POD = "POD";
    public final static String KEYWORD1 = "KEYWORD1";
    public final static String KEYWORD2 = "KEYWORD2";
    public final static String VARIABLE = "VARIABLE";
    public final static String LITERAL1 = "LITERAL1";
    public final static String LITERAL2 = "LITERAL2";
    public final static String NUMBER = "NUMBER";
    public final static String OPERATOR = "OPERATOR";
    
    private final static String[] contentTypes = new String[] {
        DEFAULT,
        COMMENT,
        KEYWORD1,
        KEYWORD2,
        VARIABLE,
        LITERAL1,
        LITERAL2,
        POD,
        NUMBER,
        OPERATOR
        };
    
    /**
     * Helper method which acts as
     * org.eclipse.jface.text.IDocument#getPartition
     * for the document partitioning managed by PerlPartitioner. 
     */
    public static ITypedRegion getPerlPartition(IDocument doc, int offset)
        throws BadLocationException
    {
        if (!(doc instanceof IDocumentExtension3))
            return doc.getPartition(offset); // should never occur

        try
        {
            return ((IDocumentExtension3) doc).getPartition(
                PERL_PARTITIONING, offset, false);
        }
        catch (BadPartitioningException e)
        {
            return doc.getPartition(offset); // should never occur
        }
    }
    
    /**
     * Helper method which acts as
     * org.eclipse.jface.text.IDocument#getDocumentPartitioner
     * for the document partitioning managed by PerlPartitioner. 
     */
    public static IDocumentPartitioner getPerlPartitioner(IDocument doc)
    {
        if (!(doc instanceof IDocumentExtension3)) return null;
        else return ((IDocumentExtension3) doc).getDocumentPartitioner(PERL_PARTITIONING); 
    }
    
    public static String[] getTypes()
    {
        String[] tmp = new String[contentTypes.length];
        System.arraycopy(contentTypes, 0, tmp, 0, tmp.length);
        return tmp;
    }
    
    private PartitionTypes() { }
}
