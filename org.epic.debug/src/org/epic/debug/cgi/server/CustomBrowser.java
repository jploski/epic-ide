/*
 * Created on 12.04.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug.cgi.server;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.HelpPlugin;
import org.epic.debug.PerlDebugPlugin;

/**
 *
 */
public class CustomBrowser implements IBrowser
{
//   public static final String CUSTOM_BROWSER_PATH_KEY = "custom_browser_path";

    /**
     * @see org.eclipse.help.ui.browser.IBrowser#close()
     */
    String mPath;
    Process pr;

    public CustomBrowser(String fPath)
    {
        mPath = fPath;
    }

    public void close()
    {
        pr.destroy();
    }

    /**
     * @see org.eclipse.help.ui.browser.IBrowser#isCloseSupported()
     */
    public boolean isCloseSupported()
    {
        return false;
    }

    /**
     * @see org.eclipse.help.ui.browser.IBrowser#displayURL(java.lang.String)
     */
    public void displayURL(String url) throws Exception
    {
        String path = mPath;

        String[] command = prepareCommand(path, url);

        try
        {
            pr = Runtime.getRuntime().exec(command);
            Thread outConsumer = new StreamConsumer(pr.getInputStream());
            outConsumer.setName("Custom browser adapter output reader");
            outConsumer.start();
            Thread errConsumer = new StreamConsumer(pr.getErrorStream());
            errConsumer.setName("Custom browser adapter error reader");
            errConsumer.start();
        }
        catch (Exception e)
        {
            PerlDebugPlugin.getDefault().logError(
                "CustomBrowser.errorLaunching " + path,
                e);

        }
    }

    /**
     * @see org.eclipse.help.ui.browser.IBrowser#isSetLocationSupported()
     */
    public boolean isSetLocationSupported()
    {
        return false;
    }

    /**
     * @see org.eclipse.help.ui.browser.IBrowser#isSetSizeSupported()
     */
    public boolean isSetSizeSupported()
    {
        return false;
    }

    /**
     * @see org.eclipse.help.ui.browser.IBrowser#setLocation(int, int)
     */
    public void setLocation(int x, int y)
    {
    }

    /**
     * @see org.eclipse.help.ui.browser.IBrowser#setSize(int, int)
     */
    public void setSize(int width, int height)
    {
    }

    /**
     * Creates the final command to launch.
     * @param path
     * @param url
     * @return String[]
     */
    private String[] prepareCommand(String path, String url)
    {
        ArrayList<String> tokenList = new ArrayList<String>();
        //Divide along quotation marks
        StringTokenizer qTokenizer =
            new StringTokenizer(path.trim(), "\"", true);
        boolean withinQuotation = false;
        String quotedString = "";
        while (qTokenizer.hasMoreTokens())
        {
            String curToken = qTokenizer.nextToken();
            if (curToken.equals("\""))
            {
                if (withinQuotation)
                {
                    tokenList.add("\"" + quotedString + "\"");
                } else
                {
                    quotedString = "";
                }
                withinQuotation = !withinQuotation;
                continue;
            } else
                if (withinQuotation)
                {
                    quotedString = curToken;
                    continue;
                } else
                {
                    //divide unquoted strings along white space
                    StringTokenizer parser =
                        new StringTokenizer(curToken.trim());
                    while (parser.hasMoreTokens())
                    {
                        tokenList.add(parser.nextToken());
                    }
                }
        }
        // substitute %1 by url
        boolean substituted = false;
        for (int i = 0; i < tokenList.size(); i++)
        {
            String token = tokenList.get(i);
            if ("%1".equals(token))
            {
                tokenList.set(i, url);
                substituted = true;
            } else
                if ("\"%1\"".equals(token))
                {
                    tokenList.set(i, "\"" + url + "\"");
                    substituted = true;
                }
        }
        // add the url if not substituted already
        if (!substituted)
            tokenList.add(url);

        String[] command = new String[tokenList.size()];
        tokenList.toArray(command);
        return command;
    }

    public static boolean isCustomBrowserID(String fID)
    {
        return (HelpPlugin.PLUGIN_ID + ".base.custombrowser").equals(fID);
    }
}