/*
 * Created on Apr 22, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.core.util;

import java.io.*;

/**
 * @author skoehler
 */
public class SafeClose {

    private SafeClose() {}
    
    public static void close(InputStream s)
    {
        if (s==null) return;
        try { s.close(); } catch (Throwable e) { e.printStackTrace(); }
    }
    public static void close(OutputStream s)
    {
        if (s==null) return;
        try { s.close(); } catch (Throwable e) { e.printStackTrace(); }
    }
}
