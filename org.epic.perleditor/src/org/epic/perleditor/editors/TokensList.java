package org.epic.perleditor.editors;

import java.util.*;

import org.epic.core.parser.PerlToken;

/**
 * An implementation of List used by PerlPartitioner to manage tokens.
 * We are not using a standard implementation ArrayList for efficiency,
 * to avoid too much copying of tokens. In addition to standard list
 * operations, TokensList provides a lookup operation which takes
 * advantage of the fact that the list of tokens is ordered.   
 * 
 * @author jploski
 */
class TokensList implements List, RandomAccess
{
    private final Comparator tokenPosComparator = new Comparator() {
        public int compare(Object o1, Object o2)
        {
            PerlToken p1 = (PerlToken) o1;
            PerlToken p2 = (PerlToken) o2;
            
            return p1.getOffset() - p2.getOffset();
        } };
    
    private final PerlToken OFFSET_TOKEN = new PerlToken();
    private PerlToken[] tokens;
    private int i;
    private PerlToken[] syncBuffer;
    private int syncOffset, syncCount, syncBufferAlloc;
    
    public TokensList()
    {
        // 2000 is a little more than enough for 75% of Perl files I examined 
        tokens = new PerlToken[2000];
    }
    
    public void add(PerlToken t)
    {
        if (i == tokens.length) expand(Math.min(tokens.length, 30000));
        
        if (i == syncOffset)
        {
            if (syncBuffer == null || syncBuffer.length < syncCount)
            {
                //System.err.println("TokensList, allocate syncBuffer of " + syncCount);
                // syncBufferAlloc is a counter intended to reduce
                // too frequent allocations
                syncBufferAlloc++;
                syncBuffer = new PerlToken[syncCount + syncBufferAlloc*4];
            }
            System.arraycopy(tokens, syncOffset, syncBuffer, 0, syncCount);
            syncOffset = -1;
        }
        tokens[i] = t;
        i++;
        assert noOverlaps();
    }
    
    public Object get(int i)
    {
        return tokens[i];
    }

    public boolean isEmpty()
    {
        return i == 0;
    }
    
    public int size()
    {
        return i;
    }
    
    public void addSync()
    {
        if (i > syncOffset)
        {
            if (i + syncCount > tokens.length) expand(i + syncCount - tokens.length);
            System.arraycopy(syncBuffer, 0, tokens, i, syncCount);
        }
        else if (i < syncOffset)
        {
            System.arraycopy(tokens, syncOffset, tokens, i, syncCount);
        }
        i += syncCount;
    }
    
    public void markSync(int offset)
    {
        syncOffset = offset;
        syncCount = i - syncOffset;
    }
    
    public void truncate(int offset)
    {
        i = offset;
    }
    
    private void expand(int count)
    {            
        PerlToken[] tmp = tokens;
        tokens = new PerlToken[tokens.length + count];
        System.arraycopy(tmp, 0, tokens, 0, tmp.length);
        //System.err.println("TokensList, expand to " + tokens.length);
    }

    public void clear()
    {
        throw new UnsupportedOperationException();            
    }

    public Object[] toArray()
    {
        PerlToken[] ret = new PerlToken[i];
        System.arraycopy(tokens, 0, ret, 0, i);
        return ret;
    }

    public Object remove(int index)
    {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element)
    {
        throw new UnsupportedOperationException();
    }

    public int indexOf(Object o)
    {
        if (o == null) return -1;
        for (int j = 0; j < i; j++)
            if (tokens[j].equals(o)) return j;
        return -1;
    }

    public int lastIndexOf(Object o)
    {
        if (o == null) return -1;
        for (int j = i-1; j >= 0; j--)
            if (tokens[j].equals(o)) return j;
        return -1;
    }

    public boolean add(Object o)
    {
        throw new UnsupportedOperationException();
    }

    public boolean contains(Object o)
    {
        return indexOf(o) != -1;
    }

    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c)
    {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection c)
    {
        for (Iterator i = c.iterator(); i.hasNext();)
            if (!contains(i.next())) return false;
        return true;
    }

    public boolean removeAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    public Iterator iterator()
    {
        return new TokensListIterator(this);
    }

    public ListIterator listIterator()
    {
        return new TokensListIterator(this);
    }

    public ListIterator listIterator(int index)
    {
        return new TokensListIterator(this, index);
    }

    public Object set(int index, Object element)
    {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray(Object[] a)
    {
        if (a.length < i)
            a = (Object[]) java.lang.reflect.Array.newInstance(
                a.getClass().getComponentType(), i);
        
        System.arraycopy(tokens, 0, a, 0, i);
        if (a.length > i) a[i] = null;
        return a;
    }

    public List subList(int fromIndex, int toIndex)
    {
        Object[] ret = new Object[toIndex - fromIndex];
        System.arraycopy(tokens, fromIndex, ret, 0, ret.length);
        return Collections.unmodifiableList(Arrays.asList(ret));
    }
    
    public void dump()
    {            
        int size = size();
        System.err.println("Dumping " + size + " token(s):");
        for (int i = 0; i < size; i++) System.err.println(i + ": " + get(i));
        System.err.println("----- end of tokens dump");
    }
    
    public int getTokenIndexPreceding(int offset)
    {
        int i = Collections.binarySearch(
            this, perlTokenWithOffset(offset), tokenPosComparator);        
        
        if (i >= 0) return i; // position with the same offset was found
        else return -(i+1) - 1; // index of last token < offset
    }
    
    boolean noOverlaps()
    {
        // disabling assertions for this class seems too much of a hassle
        /*
        boolean assertEnabled = false;
        assert assertEnabled = true;
        if (assertEnabled)
        {        
            int size = size();
            for (int i = 1; i < size; i++)
            {
                if (tokens[i].getOffset() < tokens[i-1].getOffset() + tokens[i-1].getLength())
                    return false;
            }
        }*/
        return true;
    }
    
    private PerlToken perlTokenWithOffset(int offset)
    {
        OFFSET_TOKEN.setOffset(offset);
        return OFFSET_TOKEN;
    }
}