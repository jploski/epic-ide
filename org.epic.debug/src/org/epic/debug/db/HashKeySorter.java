package org.epic.debug.db;

import java.util.*;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Helper class for PerlValue, takes care of sorting hash keys according
 * to a list specified by the DEBUG_PREVIEW_KEYS preference. This ensures
 * that the "important" keys of a hash appear first in the hash content
 * preview (to make it more convenient to see their values at a glance).
 */
public class HashKeySorter
{
    private static Map<String, Integer> weights;
    
    /**
     * Initializes the weights list used by {{@link #sort(IVariable[])} to match
     * the order of keys specified in EPIC preferences.
     */
    public static void init()
    {
        String debugPreviewKeys = PerlEditorPlugin.getDefault()
            .getPreferenceStore().getString(PreferenceConstants.DEBUG_PREVIEW_KEYS);
        
        if (debugPreviewKeys != null && debugPreviewKeys.trim().length() > 0)
        {
            weights = new HashMap<String, Integer>();
            StringTokenizer st = new StringTokenizer(debugPreviewKeys, ",");
            
            int i = 0;
            while(st.hasMoreTokens())
            {
                weights.put(st.nextToken().trim(), new Integer(i++));
            }
        }
    }
    
    /**
     * Sorts the specified list of HashKeys (in place) or does nothing at all
     * if the DEBUG_PREVIEW_KEYS preference was not set at the beginning of
     * the debug session.
     */
    public static void sort(IVariable[] ivarArr)
    {
        if (weights == null) return;
           
        Arrays.sort(ivarArr, new Comparator<Object>() {
            public int compare(Object o1, Object o2)
            {
                if (o1 == o2) return 0;
                else
                {
                    try
                    {
                        String k1 = ((HashKey) o1).getName();
                        String k2 = ((HashKey) o2).getName();
                        
                        Integer w1 = weights.get(k1);
                        Integer w2 = weights.get(k2);
                        
                        if (w1 == null && w2 == null)
                        {
                            return k1.compareTo(k2);
                        }
                        else if (w1 == null && w2 != null)
                        {
                            return 1;
                        }
                        else if (w2 == null && w1 != null)
                        {
                            return -1;
                        }
                        else return w1.compareTo(w2);
                    }
                    catch (DebugException e)
                    {
                        // this fallback should never happen
                        return String.valueOf(o1).compareTo(String.valueOf(o2));
                    }
                }
            }
        });
    }
}
