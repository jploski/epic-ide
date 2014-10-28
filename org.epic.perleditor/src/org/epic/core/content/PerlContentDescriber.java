package org.epic.core.content;

import java.io.*;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;

/**
 * Content type describer for Perl source files.
 * 
 * @author jploski
 */
public class PerlContentDescriber implements ITextContentDescriber
{
    private static final QualifiedName[] NO_OPTIONS = new QualifiedName[0];
    
    public int describe(Reader contents, IContentDescription description) throws IOException
    {
        BufferedReader reader = new BufferedReader(contents);
        String line = reader.readLine();
        
        return
            line == null ||
            !line.startsWith("#!") ||
            !(line.indexOf("perl") != -1) ? INDETERMINATE : VALID;
    }

    public int describe(InputStream contents, IContentDescription description) throws IOException
    {
        // TODO do we need support for other charsets here?
        return describe(new InputStreamReader(contents, "ISO-8859-1"), description);
    }

    public QualifiedName[] getSupportedOptions()
    {
        return NO_OPTIONS;
    }
}
