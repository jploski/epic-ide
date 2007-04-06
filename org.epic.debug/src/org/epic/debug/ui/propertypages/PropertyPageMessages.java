package org.epic.debug.ui.propertypages;

import org.eclipse.osgi.util.NLS;


/**
 * Property page messages
 */
public class PropertyPageMessages extends NLS
{
    //~ Static fields/initializers

    static
    {
        final String BUNDLE_NAME = "org.epic.debug.ui.propertypages.PropertyPageMessages";
        NLS.initializeMessages(BUNDLE_NAME, PropertyPageMessages.class);
    }

    public static String hitCountErrorMessage;

    public static String enableConditionWithoutCA;

    public static String suspendWhen;

    public static String conditionIsTrue;

    public static String conditionHasChanged;

    public static String conditionMatchesRegExp;

    public static String regExpBlankErrorMessage;

    public static String line;

    public static String hitCount;

    public static String enabled;

    public static String conditionBlankErrorMessage;

    public static String regExpInvalidErrorMessage;

    public static String unableToStore;

    public static String createContentsError;

}
