package org.epic.perleditor.editors;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * An implementation of ListIterator to traverse a {@link TokensList}.
 * 
 * @author jploski
 */
class TokensListIterator implements ListIterator
{
    private TokensList list;
    private int index;
    
    public TokensListIterator(TokensList list)
    {
        this(list, 0);
    }
    
    public TokensListIterator(TokensList list, int index)
    {
        this.list = list;
        this.index = index-1;
    }

    public int nextIndex()
    {
        return index < list.size() ? index+1 : list.size();
    }

    public int previousIndex()
    {
        return index >= 0 ? index-1 : -1;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext()
    {
        return index+1 < list.size();
    }

    public boolean hasPrevious()
    {
        return index-1 >= 0;
    }

    public Object next()
    {
        if (!hasNext()) throw new NoSuchElementException();
        return list.get(++index);
    }

    public Object previous()
    {
        if (!hasPrevious()) throw new NoSuchElementException();
        return list.get(--index);
    }

    public void add(Object o)
    {
        throw new UnsupportedOperationException();            
    }

    public void set(Object o)
    {
        throw new UnsupportedOperationException();
    }        
}