package org.epic.core.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

public class XMLUtilities
{
    private static final String INCLUDE_FILE_NAME = ".includepath";
    private static final String IGNORE_FILE_NAME = ".ignorepath";
    private static final String CHARSET = "UTF-8";

    public XMLUtilities()
    {
    }
    
    public String[] getIgnoredEntries(IProject project)
    {
        List<String> ignores = new ArrayList<String>();
        try
        {
            String fileName = project.getLocation().toString() + File.separator + IGNORE_FILE_NAME;
            File file = new File(fileName);

            if (file.exists())
            {
                SAXBuilder builder = new SAXBuilder(false);
                Document doc = builder.build(file);

                Element root = doc.getRootElement();
                List entries = root.getChildren("ignoredpathentry");
                Iterator iter = entries.iterator();

                while (iter.hasNext())
                {
                    Element element = (Element) iter.next();
                    ignores.add(element.getAttributeValue("path"));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return ignores.toArray(new String[ignores.size()]);
    }

    public String[] getIncludeEntries(IProject project)
    {
        return getIncludeEntries(project, false);
    }

    public String[] getIncludeEntries(IProject project, boolean replaceVariables)
    {
        List<String> includes = new ArrayList<String>();
        try
        {
            String fileName = project.getLocation().toString() + File.separator
                + INCLUDE_FILE_NAME;

            File file = new File(fileName);

            if (file.exists())
            {

                // No validation
                SAXBuilder builder = new SAXBuilder(false);
                Document doc = builder.build(file);

                // Get the variable manager for substitution
                IStringVariableManager varMgr = VariablesPlugin.getDefault()
                    .getStringVariableManager();

                // Get root element
                Element root = doc.getRootElement();

                List entries = root.getChildren("includepathentry");

                Iterator iter = entries.iterator();

                while (iter.hasNext())
                {
                    Element element = (Element) iter.next();
                    String path = element.getAttributeValue("path");

                    if (replaceVariables)
                    {
                        try
                        {
                            // TODO: variable substitution is buggy/unsuitable
                            // for our purposes, as it only works in context of
                            // a selected resource (see ResourceResolver.java:40);
                            // however, we don't guarantee that there is any resource
                            // selected, leading to exceptions (or the selected resource
                            // may be something accidental, leading to erratic behavior)
                            String expandedPath = varMgr
                                .performStringSubstitution(path);
                            path = expandedPath;
                        }
                        catch (CoreException e)
                        {
                            path = null;
                            e.printStackTrace();
                        }
                    }

                    if (path != null)
                    {
                        includes.add(path);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return includes.toArray(new String[includes.size()]);
    }

    public void writeIgnoredEntries(IProject project, String[] items)
        throws IOException
    {
        String file = project.getLocation().toString() + File.separator + IGNORE_FILE_NAME;

        if (items.length == 0)
        {
            File ignoredPathsFile = new File(file);
            if (ignoredPathsFile.exists()) ignoredPathsFile.delete();
            return;
        }

        Element root = new Element("ignoredpath");

        for (int i = 0; i < items.length; i++)
        {
            Element entry = new Element("ignoredpathentry");
            entry.setAttribute(new Attribute("path", items[i]));
            root.addContent(entry);
        }

        Document doc = new Document(root);

        OutputStream out = null;
        try
        {
            out = new FileOutputStream(file);
            writeOutput(doc, out);
        }
        finally
        {
            SafeClose.close(out);
        }
    }

    public void writeIncludeEntries(IProject project, String[] items)
        throws IOException
    {
        // Build XML document
        Element root = new Element("includepath");

        for (int i = 0; i < items.length; i++)
        {
            Element entry = new Element("includepathentry");
            entry.setAttribute(new Attribute("path", items[i]));
            root.addContent(entry);
        }

        // Prepare output
        Document doc = new Document(root);
        String file = project.getLocation().toString() + File.separator
            + INCLUDE_FILE_NAME;

        OutputStream out = null;
        try
        {
            out = new FileOutputStream(file);
            this.writeOutput(doc, out);
        }
        finally
        {
            SafeClose.close(out);
        }
    }

    private void writeOutput(Document doc, OutputStream out) throws IOException
    {
        Format xmlFormat = Format.getPrettyFormat();
        xmlFormat.setLineSeparator(System.getProperty("line.separator"));
        // xmlFormat.setTextMode(Format.TextMode.NORMALIZE);
        xmlFormat.setEncoding(CHARSET);

        XMLOutputter xmlout = new XMLOutputter(xmlFormat);
        xmlout.output(doc, out);
    }

}
