package org.epic.debug.db;

/**
 * A Java-side representation of an entity dumped by dumpvar_epic.pm.
 * 
 * @see POD section in dumpvar_epic.pm, which explains dumped entities
 * @author jploski
 */
class DumpedEntity
{
    private final String name;
    private final String[] refChain;
    private final String value;
    private final int valueLength;
    private final boolean cyclic;
    private final boolean undef;
    
    /**
     * @param name          dumped entity's name
     * @param refChain      dumped entity's address and reference chain
     * @param value         dumped entity's value
     * @param valueLength   original length of value in characters
     */
    public DumpedEntity(
        String name, String[] refChain, String value, int valueLength)
    {
        this.name = name;
        this.refChain = refChain;
        this.valueLength = valueLength;
        
        if (value.length() >= 1 && value.charAt(0) == '\'')
        {
            this.value = value.substring(1, value.length()-1);
            this.cyclic = false;
            this.undef = false;
        }
        else
        {
            this.value = null;
            undef = "undef".equals(value);
            cyclic = "cycle".equals(value);
        }
    }
    
    /**
     * @return the address uniquely representing this entity
     */
    public String getAddress()
    {
        // Return just the hex address, without the type qualifier.
        // The reason is that we want uniqueness, but the type
        // qualifier may change from SCALAR to REF after a value
        // is assigned to the entity.
        return addressFrom(refChain[0]);
    }
    
    /**
     * @return the address of whatever the entity directly points to;
     *         same as {@link #getValue()} if the entity is not a reference 
     */
    public String getImmediateValue()
    {
        return refChain.length > 1 ? addressFrom(refChain[1]) : value;
    }
    
    /**
     * @return name of the dumped entity, e.g. "$x", "%x", "\@x" for
     *         a lexical variable, "key" for a hash key, or "123" for
     *         an array index
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * @return original character length of the value;
     *         this will be greater than getValue().length()
     *         if {@link #isTruncated} yields true, otherwise equal
     */
    public int getOriginalValueLength()
    {
        return valueLength;
    }
    
    /**
     * @return the entity's address and reference chain.
     *         The first element is the entity's own address
     *         as provided in Perl by overload::StrVal($entity).
     *         The second element is the address of whatever
     *         the entity refers to, and so on recursively.
     *         The last element is the address of hash, list
     *         or scalar value ultimately referred to by the chain,
     *         or a REF address equal to the first element if
     *         {@link #isCyclicReference} yields true.
     */
    public String[] getReferenceChain()
    {
        return refChain;
    }
    
    /**
     * @return 0 if the entity is a list, hash or scalar;
     *         otherwise the number of references which have
     *         to be traversed to reach a list, hash or scalar
     */
    public int getReferenceCount()
    {
        return refChain.length - 1;
    }
    
    /**
     * @return describes what the reference chain ultimately points to; 
     *         this can be "SCALAR", "ARRAY", "HASH", "REF"
     *         or something unsupported (such as "CODE" etc.)
     */
    public String getReferenceType()
    {
        String varType = refChain[refChain.length-1];
        int i = varType.indexOf('=') + 1;
        if (i <= 0) i = 0;
        return varType.substring(i, varType.indexOf('(', i));
    }
    
    /**
     * @return string value of whatever the entity's reference chain
     *         points to; null if either {@link #isCyclicReference()}
     *         yields true or {@link #isDefined()} yields false
     */
    public String getValue()
    {
        return value;
    }
    
    /**
     * @return true if the entity is a reference which, when followed,
     *         eventually points to itself; false otherwise
     */
    public boolean isCyclicReference()
    {
        return cyclic;
    }
    
    /**
     * @return true if the entity's reference chain points to a defined
     *         value; false if it points to undef
     */
    public boolean isDefined()
    {
        return !undef;
    }
    
    /**
     * @return true if the value returned by {@link #getValue} was
     *         truncated on the Perl side (and thus does not reflect
     *         to the actual value pointed to by the reference chain);
     *         false otherwise
     */
    public boolean isTruncated()
    {
        return isDefined() && value.length() < valueLength;
    }
    
    private String addressFrom(String ref)
    {
        try
        {
            int i = ref.indexOf('(');
            return ref.substring(i+1, ref.indexOf(')'));
        }
        catch (Exception e) { return ref; }
    }
}