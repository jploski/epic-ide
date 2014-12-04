package org.epic.perl.editor.test;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.ILog;

import junit.framework.TestCase;

public class BaseTestCase extends TestCase
{
    private static final String PROPERTY_PREFIX = "org.epic.perleditor-test.";

    private ILog log = new Log();

    public void testDummy() { }

    /**
     * @param name a short name of the property
     * @return value of the given property from test.properties
     */
    public static String getProperty(String name)
    {
        return System.getProperty(PROPERTY_PREFIX + name);
    }

    /**
     * @param path relative to directory containing test.properties
     * @return corresponding File
     */
    protected File getFile(String path)
    {
        return new File(
            new File(getPropertiesPath()).getParentFile(),
            path);
    }

    protected ILog getLoggerForTests()
    {
        return this.log;
    }

    /**
     * @param path relative to directory containing test.properties
     * @return contents of the specified file as string
     */
    protected String readFile(String path) throws IOException
    {
        StringWriter sw = new StringWriter();
        BufferedReader r = null;

        try
        {
            r = new BufferedReader(new InputStreamReader(
                new FileInputStream(getFile(path)), "ISO-8859-1"));

            char[] buf = new char[4096];
            int bread;
            while ((bread = r.read(buf)) > 0) sw.write(buf, 0, bread);
            return sw.toString();
        }
        finally
        {
            if (r != null) try { r.close(); } catch (IOException e) { }
        }
    }

    /**
     * @param path relative to directory containing test.properties
     * @return a list of strings represented lines from the specified file
     */
    protected List<String> readLines(String path) throws IOException
    {
        BufferedReader r = null;

        try
        {
            r = new BufferedReader(new FileReader(getFile(path)));

            List<String> lines = new ArrayList<String>();
            String l;
            while ((l = r.readLine()) != null) lines.add(l);
            return lines;
        }
        finally
        {
            if (r != null) try { r.close(); } catch (IOException e) { }
        }
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        setUpTestProperties();
    }

    private String getPropertiesPath()
    {
        String propertiesPath = getProperty("properties");
        return propertiesPath != null ? propertiesPath : "test.properties";
    }

    private void setUpTestProperties() throws IOException
    {
        BufferedInputStream in = null;

        try
        {
            in = new BufferedInputStream(new FileInputStream(getPropertiesPath()));
            Properties testProperties = new Properties();
            testProperties.load(in);

            for (Enumeration e = testProperties.keys(); e.hasMoreElements();)
            {
                String key = e.nextElement().toString();
                String value = testProperties.getProperty(key);
                System.setProperty(key, value);
            }
        }
        finally
        {
            if (in != null) try { in.close(); } catch (IOException e) { }
        }
    }
}
