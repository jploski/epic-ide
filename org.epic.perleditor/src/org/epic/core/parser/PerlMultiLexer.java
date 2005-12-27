package org.epic.core.parser;

import java.io.Reader;
import java.io.StringReader;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import antlr.*;

/**
 * Combines the various sublexers representing lexical states to provide a
 * single lexer capable of processing entire Perl source files.
 * 
 * @author jploski
 */
public class PerlMultiLexer extends TokenStreamSelector
{
    private final PerlLexer mainLexer;
    private final PODLexer lexExpectPODEnd;
    private final LexExpectString lexExpectString;
    private final LexExpectStringEnd lexExpectStringEnd;
    private final LexExpectStringSuffix lexExpectStringSuffix;
    private final LexExpectSubstExpr lexExpectSubstExpr;
    private final LexExpectHereDocEnd lexExpectHereDocEnd;
    private IDocument doc;
    
    public PerlMultiLexer()
    {
        this(null, null, null);
    }

    public PerlMultiLexer(Reader reader)
    {
        this(reader, null, null);
    }

    public PerlMultiLexer(IDocument doc, CurlyToken parseStartCurly)
    {
        this(null, doc, parseStartCurly);
    }

    private PerlMultiLexer(
        Reader reader,
        IDocument doc,
        CurlyToken parseStartCurly)
    {
        // TODO need a different tab size for debugging (such as 4) and for
        // token offset computations (1 - tab counts as a single character)
        final int TAB_SIZE = 1;

        this.doc = doc;

        StringReader dummyReader = new StringReader("");
        LexerSharedInputState inputState = new LexerSharedInputState(dummyReader);
        
        mainLexer = new PerlLexer(inputState);
        mainLexer.setTabSize(TAB_SIZE);
        mainLexer.setParent(this);

        lexExpectPODEnd = new PODLexer(inputState);
        lexExpectPODEnd.setTabSize(TAB_SIZE);
        lexExpectPODEnd.setParent(this);

        lexExpectStringEnd = new LexExpectStringEnd(inputState);
        lexExpectStringEnd.setTabSize(TAB_SIZE);
        lexExpectStringEnd.setParent(this);

        lexExpectString = new LexExpectString(inputState);
        lexExpectString.setTabSize(TAB_SIZE);
        lexExpectString.setParent(this);        

        lexExpectSubstExpr = new LexExpectSubstExpr(inputState);
        lexExpectSubstExpr.setTabSize(TAB_SIZE);
        lexExpectSubstExpr.setParent(this);        

        lexExpectStringSuffix = new LexExpectStringSuffix(inputState);
        lexExpectStringSuffix.setTabSize(TAB_SIZE);
        lexExpectStringSuffix.setParent(this);        

        lexExpectHereDocEnd = new LexExpectHereDocEnd(inputState);
        lexExpectHereDocEnd.setTabSize(TAB_SIZE);
        lexExpectHereDocEnd.setParent(this);
        
        if (reader != null || doc != null) reset(reader, doc, parseStartCurly);
    }

    public int getCurlyLevel()
    {
        return mainLexer.getCurlyLevel();
    }
    
    public void recover()
    {
        LexerSharedInputState inputState = mainLexer.getInputState();
        if (inputState instanceof PerlLexerSharedInputState)
            ((PerlLexerSharedInputState) inputState).recover();
    }
    
    public void reset(
        Reader reader,
        IDocument doc,
        CurlyToken parseStartCurly)
    {
        this.doc = doc;
        
        LexerSharedInputState inputState =
            createInputState(reader, doc, parseStartCurly);

        mainLexer.setInputState(inputState);
        lexExpectPODEnd.setInputState(inputState);
        lexExpectStringEnd.setInputState(inputState);
        lexExpectString.setInputState(inputState);
        lexExpectSubstExpr.setInputState(inputState);
        lexExpectStringSuffix.setInputState(inputState);
        lexExpectHereDocEnd.setInputState(inputState);
        if (parseStartCurly != null) mainLexer.setCurlyLevel(parseStartCurly.getLevel());
        select(mainLexer);
    }

    void expectHereDocEnd(String terminator)
    {
        lexExpectHereDocEnd.setTerminator(terminator.substring(2));
        lexExpectHereDocEnd.setStartLine(mainLexer.getLine());
        push(lexExpectHereDocEnd);
    }

    void expectPODEnd()
    {
        push(lexExpectPODEnd);
    }

    void expectString()
    {
        push(lexExpectString);
    }

    void expectStringEnd(char quoteBeginChar)
    {
        lexExpectStringEnd.setQuoteEndChar(getQuoteEndChar(quoteBeginChar));
        lexExpectStringEnd.setStartLine(mainLexer.getLine());
        push(lexExpectStringEnd);
    }

    void expectStringSuffix()
    {
        push(lexExpectStringSuffix);
    }

    void expectSubstExpr()
    {
        push(lexExpectSubstExpr);
    }

    void uponEOF()
        throws TokenStreamException, CharStreamException
    {
        while (getCurrentStream() != mainLexer)
            pop();
    }

    int computeTokenOffset(PerlToken t)
    {
        if (doc == null)
            return 0;

        try
        {
            return doc.getLineOffset(t.getLine() - 1) + t.getColumn() - 1;
        }
        catch (BadLocationException e)
        {
            // this can only happen if the document changes while being read
            // ...which should never occur if we have no bugs.
            throw new RuntimeException(e);
        }
    }

    private LexerSharedInputState createInputState(
        Reader reader,
        IDocument doc,
        CurlyToken parseStartCurly)
    {
        if (doc == null)
            return new LexerSharedInputState(reader);
        else
            return new PerlLexerSharedInputState(
                doc,
                getStartOffset(parseStartCurly));
    }
    
    private int getStartOffset(CurlyToken parseStartCurly)
    {
        return parseStartCurly != null ? parseStartCurly.getOffset() : 0;
    }

    private char getQuoteEndChar(char quoteBeginChar)
    {
        switch (quoteBeginChar)
        {
        case '{':
            return '}';
        case '[':
            return ']';
        case '(':
            return ')';
        case '<':
            return '>';
        default:
            return quoteBeginChar;
        }
    }

    private static Reader createStringReader(IDocument doc, int startOffset)
    {
        try
        {
            return new StringReader(
                doc.get(startOffset, doc.getLength() - startOffset));
        }
        catch (BadLocationException e)
        {
            // this can only happen if the document changes while being read
            // ...which should never occur if we have no bugs.
            throw new RuntimeException(e);
        }
    }

    private static class PerlLexerSharedInputState
        extends LexerSharedInputState
    {
        public PerlLexerSharedInputState(IDocument doc, int startOffset)
        {
            // we use DocumentInputBuffer for speed
            // super(createStringReader(doc, startOffset));
            super(new DocumentInputBuffer(doc, startOffset));

            try
            {
                line = doc.getLineOfOffset(startOffset) + 1;
                column = startOffset - doc.getLineOffset(line - 1) + 1;
                tokenStartColumn = column;
                tokenStartLine = line;
            }
            catch (BadLocationException e)
            {
                // this can only happen if the document changes while being read
                // ...which should never occur if we have no bugs.
                throw new RuntimeException(e);
            }
        }
        
        public void recover()
        {
            try
            {
                for (;;)
                {
                    char c = input.LA(1);
                    if (c == '\r' || c == '\n' || c == '\uFFFF') break;
                    else { input.consume(); column++; }
                }
            }
            catch (CharStreamException e) { /* can't happen */ } 
        }
    }
}