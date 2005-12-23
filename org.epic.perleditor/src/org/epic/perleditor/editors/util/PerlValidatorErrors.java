package org.epic.perleditor.editors.util;

import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.epic.core.ResourceMessages;

/**
 * A collection of error message patterns and detailed explanations
 * which can be shown in the Explain Errors/Warnings view. 
 * 
 * @author luelljoc
 * @author jploski
 */ 
public class PerlValidatorErrors
{
    private final Set errorMessages;
    private final ErrorMessage unknownErrorMessage;
    
    public PerlValidatorErrors()
    {
        unknownErrorMessage = new ErrorMessage(
            ResourceMessages.getString("PerlValidatorErrors.unknownErrorMessage"),
            ResourceMessages.getString("PerlValidatorErrors.unknownErrorMessage.descr"),
            true);
        
        errorMessages = new HashSet();
        
        ResourceBundle errorBundle = ResourceBundle.getBundle(
            "org.epic.perleditor.editors.errorsAndWarnings");

        Pattern re1, re2, re3;

        re1 = Pattern.compile("[\\$\\(\\)\\[\\]\\{\\}\\?\\|\\*\\+\\\\]");
        re2 = Pattern.compile("%([sdcl][x]{0,1})");
        re3 = Pattern.compile("%\\.[0-9]s");

        // Populate the error messages hash
        for (Enumeration e = errorBundle.getKeys(); e.hasMoreElements();)
        {
            String key = (String) e.nextElement();
            String value = errorBundle.getString(key);
            
            int tabIndex = value.indexOf("\t");
            String msg = value.substring(0, tabIndex);
            String descr = value.substring(tabIndex + 1);

            // Substitute "( ) { } [ ]$?|*+\" with .
            msg = re1.matcher(msg).replaceAll(".");

            //  Substitute "%s %c %d %lx" with .*?
            msg = re2.matcher(msg).replaceAll(".*?");

            // Substitute %.[0-9]s with .*?
            msg = re3.matcher(msg).replaceAll(".*?");

            errorMessages.add(new ErrorMessage(msg, descr, false));
        }    
    }
    
    /**
     * @return an ErrorMessage instance matching the given error line.
     *         If the error line does not match any known error message,
     *         the returned instance will return true for isUnknown().
     */
    public ErrorMessage getErrorMessage(String line)
    {
        for (Iterator i = errorMessages.iterator(); i.hasNext();)
        {
            ErrorMessage errorMessage = (ErrorMessage) i.next();
            if (errorMessage.matches(line)) return errorMessage;
        }
        return unknownErrorMessage;
    }

    /**
     * An error message together with its severity and explanation.
     */
    public static class ErrorMessage
    {
        private final Pattern pattern;
        private final String descr;
        private final boolean warning;
        private final boolean unknown;
        
        /**
         * Only used by the enclosing outer class.
         */
        private ErrorMessage(String regex, String descr, boolean unknown)
        {
            this.descr = descr;
            this.unknown = unknown;
            this.pattern = Pattern.compile(regex);            
            this.warning = 
                descr.startsWith("(W") ||
                descr.startsWith("(D") ||
                descr.startsWith("(S");
        }
        
        /**
         * @return an explanation text for this error message
         */
        public String getExplanation()
        {
            return descr;
        }
        
        /**
         * @return IMarker.SEVERITY_WARNING or
         *         IMarker.SEVERITY_ERROR
         */
        public Integer getSeverity()
        {
            return new Integer(
                warning ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_ERROR);
        }
        
        /**
         * @return true if this error message was not recognized
         *         and therefore does not have a specific explanation text,
         *         false otherwise
         */
        public boolean isUnknown()
        {
            return unknown;
        }
        
        /**
         * @return getSeverity() == IMarker.SEVERITY_WARNING
         */
        public boolean isWarning()
        {
            return warning;
        }
        
        public String toString()
        {
            return descr;
        }

        /**
         * Only used by the enclosing outer class. 
         */
        private boolean matches(String error)
        {
            return pattern.matcher(error).find();
        }
    }
}
