package org.epic.debug.cgi;

import java.util.*;

import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;

/**
 * Provides access to configuration settings of an EpicCgiHandler.
 * This configuration is passed down from EPIC using the Brazil
 * properties file.
 */
public class CGIConfig
{
    private final Server server;
    private final String propsPrefix;    
    
    private final boolean debugMode;
    private String debugInc;
    private List runInc;
    private final String hostname; 
    private final int portIn;
    private final int portOut;
    private final int portErr;
    private final String protocol;
    private final String perlExecutable;
    private final int serverPort;
    
    /**
     * Creates the CGIConfig instance.
     * 
     * @param server        Brazil server from which configuration
     *                      should be retrieved
     * @param propsPrefix   prefix used by all EPIC-specific configuration
     *                      properties
     */
    public CGIConfig(Server server, String propsPrefix)
    {
        this.server = server;
        this.propsPrefix = propsPrefix;
        
        serverPort = server.listen.getLocalPort();
        hostname = server.hostName;
        protocol = server.protocol;
        
        portIn = getIntProperty("InPort");
        portOut = getIntProperty("OutPort");
        portErr = getIntProperty("ErrorPort");
        
        debugMode = getProperty("Debug").equalsIgnoreCase("true");
        debugInc = getProperty("DebugInclude");
        
        runInc = getListProperty("RunInclude");
        
        perlExecutable = getProperty("executable");
    }
    
    /**
     * @return a string with include (-I) command-line options to
     *         be used when executing scripts in debug mode 
     */
    public String getDebugInclude()
    {
        return debugInc;
    }
    
    /**
     * @return true if scripts should execute in debug mode;
     *         false otherwise
     */
    public boolean getDebugMode()
    {
        return debugMode;
    }
    
    /**
     * @return a TCP port on localhost to which stderr of executed
     *         CGI scripts should be forwarded
     */
    public int getErrorPort()
    {
        return portErr;
    }
    
    /**
     * @return DNS name or dot-quad IP address of the web server
     */
    public String getHostname()
    {
        return hostname;
    }   
    
    /**
     * @return a TCP port on localhost to which diagnostic messages
     *         about executed scripts should be forwarded
     */
    public int getDiagPort()
    {
        return portIn;
    }

    /**
     * @return a TCP port on localhost to which stderr of executed
     *         CGI scripts should be forwarded (in addition to delivering
     *         it to the requesting web client)
     */
    public int getOutPort()
    {
        return portOut;
    }
    
    /**
     * @return path to the Perl interpreter executable that should
     *         be used for running CGI scripts
     */
    public String getPerlExecutable()
    {
        return perlExecutable;
    }
    
    /**
     * @return prefix used by all EPIC-specific configuration properties
     */
    public String getPropsPrefix()
    {
        return propsPrefix;
    }
    
    /**
     * @param an additional prefix used by requested property names;
     *        this prefix should <b>not</b> include the one returned
     *        by {@link #getPropsPrefix} (which is always assumed)
     * @return a mapping of property names <b>with the prefix stripped</b>
     *         to their respective values
     */
    public Map getProperties(String prefix)
    {
        Map ret = new HashMap();
        int len = (propsPrefix + prefix).length();
        for (Iterator i = server.props.keySet().iterator(); i.hasNext();)
        {
            String key = (String) i.next(); 
            if (key.startsWith(propsPrefix + prefix))
                ret.put(key.substring(len), server.props.getProperty(key));
        }
        return ret;
    }
    
    /**
     * @param Request       a request received from a web client
     * @param name          (unprefixed) name of a configuration property
     * @param defaultValue  default value to be returned if the property
     *                      is not defined
     * @return value of the property extracted from the request
     */
    public String getRequestProperty(
        Request request,
        String name,
        String defaultValue)
    {
        return request.props.getProperty(propsPrefix + name, defaultValue);
    }
    
    /**
     * @return the protocol used by the web server ("http" or "https")
     */
    public String getProtocol()
    {
        return protocol;
    }
    
    /**
     * @return a list with command-line parameters representing
     *         the include path for the Perl interpreter 
     */
    public List getRunInclude()
    {
        // TODO why do we return a List here and a String in getDebugInclude?
        // more refactoring needed
        return Collections.unmodifiableList(runInc);
    }
    
    /**
     * @return the TCP port on which the web server awaits client requests
     */
    public int getServerPort()
    {
        return serverPort;
    }
    
    private int getIntProperty(String name)
    {
        return Integer.parseInt(getProperty(name));
    }
    
    private List getListProperty(String name)
    {
        List values = new ArrayList();

        for (int i = 0;; i++)
        {
            String value = getProperty(name + "[" + i + "]");
            if (value == null) break;
            else values.add(value);
        }
        return values;
    }
    
    private String getProperty(String name)
    {
        return server.props.getProperty(propsPrefix + name);
    }
}
