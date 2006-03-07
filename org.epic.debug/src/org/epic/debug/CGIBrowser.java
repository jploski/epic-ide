package org.epic.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.browser.BrowserDescriptor;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.epic.core.views.browser.BrowserView;
import org.epic.debug.cgi.CustomBrowser;

/**
 * Represents a web browser instance launched to request a CGI script
 * during execution of a "Perl CGI" launch configuration. Encapsulates
 * the code for opening and closing the browser and for reporting failure
 * to user.
 */
public class CGIBrowser
{
    private final ILaunch mLaunch;
    private final int mWebserverPort;
    private final String mHtmlRootFileRel;
    private IBrowser mBrowser;
    
    /**
     * Creates an instance which will be used to request the given
     * relative URI from a web server running on the given port as
     * part of the given launch.
     */
    public CGIBrowser(ILaunch launch, String relURL, int port)
    {
        mLaunch = launch;
        mWebserverPort = port;
        mHtmlRootFileRel = relURL;
    }
    
    /**
     * Closes the web browser opened by a call to {@link #open}.
     * If open has not been called or has failed, this method has no effect.
     */
    public void close()
    {
        if (mBrowser != null) mBrowser.close();
    }
    
    /**
     * Opens the browser with the URL configured at construction time.
     * If an appropriate web browser is already running, it may be reused.
     * In case of misconfiguration, the method may fail and display
     * an error dialog instead. 
     */
    public void open()
    {
        DebugPlugin.getDefault().asyncExec(new Runnable() {
            public void run()
            {
                try { startBrowserImpl(); }
                catch (Exception e)
                {
                    PerlDebugPlugin.getDefault().logError(
                        "Could not start browser for CGI debugging",
                        e);
                }
            } });
    }

    private String getLaunchAttribute(String attrName)
        throws CoreException
    {
        return mLaunch.getLaunchConfiguration().getAttribute(
            attrName, (String) null);
    }

    private void startBrowserImpl() throws Exception
    {   
        String browserID = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_BROWSER_ID);

        String browserPath = getLaunchAttribute(
            PerlLaunchConfigurationConstants.ATTR_CUSTOM_BROWSER_PATH);
        
        if (browserID.equals(BrowserView.ID_BROWSER))
        {
            showBrowserView();
            return;
        }               
        else if (CustomBrowser.isCustomBrowserID(browserID))
        {
            mBrowser = new CustomBrowser(browserPath);
        }
        else
        {
            BrowserDescriptor[] browserDescr = getBrowserDescriptor();
            for (int i = 0; i < browserDescr.length; i++)
            {
                BrowserDescriptor descr = browserDescr[i];
                if (descr.getID().equals(browserID))
                {
                    mBrowser = descr.getFactory().createBrowser();
                    break;
                }
            }
        }
        if (mBrowser != null)
        {
            mBrowser.displayURL(
                "http://localhost:" + mWebserverPort + "/"+ mHtmlRootFileRel);
        }
        else
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not find browser for CGI debugging.");
        }
    }
    
    private BrowserDescriptor[] getBrowserDescriptor()
    {
        Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();        
        if (shell == null) return new BrowserDescriptor[0];
        
        final Object[] ret = new Object[1];
        shell.getDisplay().syncExec(new Runnable() {
            public void run() {
                ret[0] = BrowserManager.getInstance().getBrowserDescriptors();
            } });

        return (BrowserDescriptor[]) ret[0];
    }
    
    private void showBrowserView() throws PartInitException
    {
        Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();        
        if (shell == null) return;
        
        shell.getDisplay().syncExec(new Runnable() {
            public void run()
            {
                try
                {
                    IWorkbenchPage activePage =
                        PerlDebugPlugin.getWorkbenchWindow().getActivePage();
                    BrowserView view =
                        (BrowserView) activePage.showView(BrowserView.ID_BROWSER);
                    view.setUrl("http://localhost:" + mWebserverPort + "/");
                }
                catch (PartInitException e)
                {
                    PerlDebugPlugin.getDefault().logError(
                        "Could not open browser view.",
                        e);
                }
            } });
    }
}
