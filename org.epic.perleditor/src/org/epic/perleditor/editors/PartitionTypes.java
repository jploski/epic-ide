package org.epic.perleditor.editors;

/**
 * Types of partitions that can appear in a Perl document,
 * also referred to as ITypedRegions and "content types".
 * 
 * @author jploski
 */
public class PartitionTypes
{
    public final static String DEFAULT = "__dftl_partition_content_type";
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
    
    public static String[] getTypes()
    {
        String[] tmp = new String[contentTypes.length];
        System.arraycopy(contentTypes, 0, tmp, 0, tmp.length);
        return tmp;
    }
    
    private PartitionTypes() { }
}
