package org.epic.perleditor.views;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class PerlDocViewMessages
{
    private static final String BUNDLE_NAME = "org.epic.perleditor.views.PerlDocViewMessages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE =
        ResourceBundle.getBundle(BUNDLE_NAME);

    private PerlDocViewMessages()
    {
    }

    public static String getString(String key)
    {
        try
        {
            return RESOURCE_BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }

    public static ResourceBundle getBundle()
    {
        return RESOURCE_BUNDLE;
    }
}
