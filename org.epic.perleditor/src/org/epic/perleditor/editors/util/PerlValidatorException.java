package org.epic.perleditor.editors.util;

import java.io.IOException;

/**
 * Thrown when unexpected low-level problems are encountered during
 * PerlValidator's execution.
 * 
 * @author jploski
 */
public class PerlValidatorException extends Exception
{
    /**
     * Low-level communication problems with the Perl interpreter occurred.
     */
    public PerlValidatorException(InterruptedException cause)
    {
        super(cause);
    }
    
    /**
     * Unexpected thread interruption during communication with the Perl
     * interpreter.
     */
    public PerlValidatorException(IOException cause)
    {
        super(cause);
    }
}
