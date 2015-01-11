package org.epic.core.parser;

import java.io.*;
import java.net.URL;
import java.util.*;

import antlr.Token;
import antlr.TokenStreamException;

import org.eclipse.jface.text.Document;
import org.epic.perl.editor.test.BaseTestCase;

public class TestPerlMultiLexer extends BaseTestCase
{
    private Map<Integer, String> tokenNames;
    private List<Integer> tokenCounts = new ArrayList<Integer>();
    
    public void testGlob() throws Exception
    {
        _testFile(
            "workspace/EPICTest/test_Glob.pl",
            new PrintWriter(new OutputStreamWriter(System.out), true));   
    }

    public void testHeredoc() throws Exception
    {
        _testFile(
            "workspace/EPICTest/test_Heredoc.pl",
            new PrintWriter(new OutputStreamWriter(System.out), true));   
    }
    
    /*public void testSelectedCase() throws Exception
    {
        _testFile(
            "workspace/EPICTest/format2.pl",
            new PrintWriter(new OutputStreamWriter(System.out), true));   
    }*/
    
    public void testAll() throws Exception
    {   
        if (!"true".equals(getProperty("TestPerlMultiLexer.enabled"))) return;

        BufferedReader r = null;
        PrintWriter w = null;
        String path = null;
        
        try
        {
            long t1 = System.currentTimeMillis();
            r = new BufferedReader(new FileReader("test.in/TestPerlMultiLexer-in.txt"));
            w = new PrintWriter(/*sw*/new OutputStreamWriter(System.out), true);
            
            int i = 0;
            tokenCounts = new ArrayList<Integer>();
            while ((path = r.readLine()) != null)
            {
                _testFile(path, w);
                i++;
            }
            long t2 = System.currentTimeMillis();
            Collections.sort(tokenCounts);
            
            double msPerFile = (double)(t2-t1) / i;
            System.out.println((t2-t1) + " ... " + msPerFile);
            System.out.println("tokens per file: " + tokenCounts.get((int)(tokenCounts.size()*0.75)));
          
            /*assertEquals(
                readFile("test.in/TestPerlMultiLexer-expected.txt"),
                sw.toString());*/
        }
        catch (Exception e)
        {
            System.err.println("failed in file: " + path);
            throw e;
        }
        finally
        {
            if (r != null) try { r.close(); } catch (Exception e) { }
            if (w != null) try { w.close(); } catch (Exception e) { }
        }
    }

    private void _testFile(String path, PrintWriter w)
        throws IOException, TokenStreamException
    {
        String source = readFile(path);
        PerlMultiLexer selector =
            new PerlMultiLexer(new Document(source), null);

        int i = 0;
        Token t;
        while ((t = nextToken(selector, path)).getType() != Token.EOF_TYPE)
        {
            //System.err.println(path + ": " + i + ":" + token2String(t) + " " + t.getClass());
            i++;
        }
        tokenCounts.add(new Integer(i));
    }
    
    protected void setUp()
        throws Exception
    {
        super.setUp();        
        setUpTokenNames();
    }
    
    private void setUpTokenNames() throws IOException
    {
        tokenNames = new HashMap<Integer, String>();
        URL tokensURL = PerlLexer.class.getResource("PerlTokenTypes.txt");
        assertNotNull(tokensURL);
        List<String> lines = readLines(tokensURL.getPath());

        for (int i = 2; i < lines.size(); i++)
        {
            String line = lines.get(i);
            StringTokenizer st = new StringTokenizer(line, "=");            
            String name = st.nextToken();
            Integer value = new Integer(st.nextToken());            
            tokenNames.put(value, name);
        }
    }
    
    private PerlToken nextToken(PerlMultiLexer lexer, String path) throws TokenStreamException
    {
        try
        {
            return (PerlToken) lexer.nextToken();
        }
        catch (TokenStreamException e)
        {
            lexer.recover();
            System.err.println(
                "WARNING: " + path + ": PerlMultiLexer recovery performed: " +
                e.getMessage()); // TODO log it when in development mode?
            return (PerlToken) lexer.nextToken();
        }
    }
    
    private String token2String(Token t)
    {
        StringBuffer buf = new StringBuffer();
        buf.append('<');
        buf.append(tokenNames.get(new Integer(t.getType())));
        buf.append(">,line=");
        buf.append(t.getLine());
        buf.append(",col=");
        buf.append(t.getColumn());
        buf.append(":\"");
        buf.append(t.getText());
        buf.append('\"');
        if (buf.length() > 200) { buf.setLength(200); buf.append("..."); }
        return buf.toString();
    }
}