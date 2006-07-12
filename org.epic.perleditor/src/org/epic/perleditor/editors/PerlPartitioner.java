package org.epic.perleditor.editors;

import java.util.*;

import antlr.Token;
import antlr.TokenStreamException;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.epic.core.parser.*;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Computes the partitioning of Perl source files by parsing them with
 * {@link org.epic.core.parser.PerlMultiLexer}.
 * 
 * @author jploski
 */
public class PerlPartitioner implements
    IDocumentPartitioner,
    IDocumentPartitionerExtension,
    IDocumentPartitionerExtension2,
    IDocumentPartitionerExtension3
{
    private final Object TOKENS_LOCK = new Object();
    private final PerlMultiLexer lexer = new PerlMultiLexer();
    private final ILog log;
    
    private IDocument doc;
    private boolean ignoreDocumentChangedEvent;
    private DocumentEvent ignoredEvent;
    private DocumentRewriteSession activeRewriteSession;
    private boolean initialized;
    private TokensList tokens;
    private int lastUnaffectedTokenI = -1;
    private int syncTokenI = -1;
    
    public PerlPartitioner(ILog log)
    {
        this.log = log;
        tokens = new TokensList();
    }    
    
    public ITypedRegion[] computePartitioning(int offset, int length)
    {
        return computePartitioning(offset, length, false);
    }
    
    public ITypedRegion[] computePartitioning(
        int offset, int length, boolean includeZeroLengthPartitions)
    {
        // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=49264
        // for explanation of includeZeroLengthPartitions
        
        List typedRegions = new ArrayList();
        if (!initialized) initialize();
        
        if (tokens.isEmpty())
        {
            return new ITypedRegion[] { new TypedRegion(
                0, doc.getLength(), PartitionTypes.DEFAULT) };
        }
                       
        int tokenCount = tokens.size();
        int prevRegionEnd = -1;
        int rOffset, rLength;
        
        for (int i = Math.max(tokens.getTokenIndexPreceding(offset), 0);
             i < tokenCount;
             i++)
        {
            PerlToken t = (PerlToken) tokens.get(i);
            if (t.getOffset() >= offset + length) break;
            
            if (prevRegionEnd == -1)
            {
                if (t.includes(offset))
                {
                    // add the right piece of the leftmost token
                    rOffset = offset;
                    rLength = t.getOffset() + t.getLength() - rOffset;
                    typedRegions.add(new TypedRegion(
                        rOffset, rLength, getTokenContentType(t, i)));
                    if (includeZeroLengthPartitions) typedRegions.add(new TypedRegion(
                        rOffset + rLength, 0, PartitionTypes.DEFAULT));
                }
                else
                {
                    rOffset = offset;
                    rLength = 0;
                }
            }
            else
            {
                if (t.getOffset() - prevRegionEnd > 0)
                {
                    // add gap before the current position
                    rOffset = prevRegionEnd;
                    rLength = t.getOffset() - rOffset;
                    typedRegions.add(new TypedRegion(
                        rOffset, rLength, PartitionTypes.DEFAULT));
                }
                rOffset = t.getOffset();
                rLength = Math.min(t.getLength(), offset+length-rOffset);
                typedRegions.add(new TypedRegion(
                    rOffset, rLength, getTokenContentType(t, i)));
                if (includeZeroLengthPartitions) typedRegions.add(new TypedRegion(
                    rOffset + rLength, 0, PartitionTypes.DEFAULT));
            }
            prevRegionEnd = rOffset + rLength;
        }
        
        if (prevRegionEnd < offset + length)
        {
            // add gap after the rightmost position
            typedRegions.add(new TypedRegion(
                prevRegionEnd,
                offset + length - prevRegionEnd,
                PartitionTypes.DEFAULT));
        }
        
        ITypedRegion[] ret = (ITypedRegion[]) typedRegions.toArray(
            new ITypedRegion[typedRegions.size()]);
        
        //dumpPartitioning(offset, length, includeZeroLengthPartitions, ret);        
        return ret;
    }

    public void connect(IDocument document)
    {
        connect(document, false);
    }
    
    public void connect(IDocument document, boolean delayInitialization)
    {
        doc = document;
        initialized = false;
        
        if (!delayInitialization) initialize();
    }

    public void disconnect()
    {
        doc = null;
    }

    public void documentAboutToBeChanged(DocumentEvent event)
    {
        // note: this is not ALWAYS called before documentChanged.. just sometimes
        computeLastUnaffectedTokenI(event);
        computeSyncTokenI(event);
    }

    public boolean documentChanged(DocumentEvent event)
    {
        return documentChanged2(event) != null;
    }
    
    public IRegion documentChanged2(DocumentEvent event)
    {
        try
        {
            if (ignoreDocumentChangedEvent)
            {
                ignoreDocumentChangedEvent = false;
                ignoredEvent = event;
                return null;
            }
    
            synchronized (TOKENS_LOCK)
            {
                IRegion ret = documentChanged2Impl(event);
                //tokens.dump();
                return ret;
            }
        }
        finally
        {
            syncTokenI = lastUnaffectedTokenI = -1;
        }
    }
    
    public DocumentRewriteSession getActiveRewriteSession()
    {
        return activeRewriteSession;
    }
    
    public String getContentType(int offset)
    {
        return getContentType(offset, false);
    }
    
    public String getContentType(int offset, boolean preferOpenPartitions)
    {
        return getPartition(offset, preferOpenPartitions).getType();
    }
    
    public String[] getLegalContentTypes()
    {
        return PartitionTypes.getTypes();
    }
    
    public String[] getManagingPositionCategories()
    {
        return null;
    }

    public ITypedRegion getPartition(int offset)
    {
        return getPartition(offset, false);
    }

    public ITypedRegion getPartition(int offset, boolean preferOpenPartitions)
    {
        ITypedRegion ret = getPartitionImpl(offset, preferOpenPartitions);
        assert ret.getOffset() + ret.getLength() <= doc.getLength() :
            "assertion failed: getPartition returned invalid result for offset " +
            offset + ": " + ret.getOffset() + ":" + ret.getLength() + ":" + ret.getType();
        return ret;
    }
    
    public List getTokens()
    {
        return Collections.unmodifiableList(tokens);
    }
    
    public Object getTokensLock()
    {
        return TOKENS_LOCK;
    }
    
    /**
     * Marks the next documentChanged event as coming from PerlBracketInserter
     * and therefore safe to ignore (because it is will be followed by another
     * event). This helps the smart typing feature improve performance.
     */
    public void ignoreSmartTypingEvent()
    {
        ignoreDocumentChangedEvent = true;
    }

    public void startRewriteSession(DocumentRewriteSession session)
        throws IllegalStateException
    {
        if (activeRewriteSession != null) throw new IllegalStateException(
            "another rewrite session is already active");
        else
            activeRewriteSession = session;
    }

    public void stopRewriteSession(DocumentRewriteSession session)
    {
        activeRewriteSession = null;
        initialize(); // reset state, e.g. after a series of find-replace operations
    }
    
    private void computeLastUnaffectedTokenI(DocumentEvent event)
    {
        try
        {
            lastUnaffectedTokenI =
                tokens.getTokenIndexPreceding(event.getOffset());
            
            if (lastUnaffectedTokenI >= 0)
            {
                PerlToken t = (PerlToken) tokens.get(lastUnaffectedTokenI);
                if (t.includes(event.getOffset())) lastUnaffectedTokenI--;                
            }
            else lastUnaffectedTokenI = -1;
            
            // scan back for a curly brace
            while (lastUnaffectedTokenI >= 0)
            {
                PerlToken t = (PerlToken) tokens.get(lastUnaffectedTokenI);
                PerlToken prevT = lastUnaffectedTokenI > 0
                    ? (PerlToken) tokens.get(lastUnaffectedTokenI-1)
                    : null;
    
                if (t.getType() == PerlTokenTypes.OPEN_CURLY &&
                    (prevT == null || prevT.getType() != PerlTokenTypes.OPER_ARROW))
                {
                    break;
                }
                else lastUnaffectedTokenI--;
            }
        }
        finally
        {
            assert lastUnaffectedTokenI == -1 ||
                   tokens.get(lastUnaffectedTokenI) instanceof CurlyToken;
        }
    }
    
    private void computeSyncTokenI(DocumentEvent event)
    {
        int offset = event.getOffset() + event.getLength();
        syncTokenI = tokens.getTokenIndexPreceding(offset);
        
        if (syncTokenI < 0) syncTokenI = -1;
        else
        {
            syncTokenI++;
            if (syncTokenI >= tokens.size()) syncTokenI = -1;
        }
    }
    
    private IRegion documentChanged2Impl(DocumentEvent event)
    {
        assert event.getDocument() == doc;

        String repl = event.getText();
        if (repl == null) repl = "";
        int shiftDelta = repl.length() - event.getLength();
        
        if (ignoredEvent != null) // compensate for ignoredEvent
        {
            repl = ignoredEvent.getText();
            if (repl == null) repl = "";
            shiftDelta += repl.length() - ignoredEvent.getLength();
            ignoredEvent = null;
        }
        
        PerlToken sync;
        if (syncTokenI >= 0 && syncTokenI < tokens.size())
        {          
            sync = (PerlToken) tokens.get(syncTokenI);
            sync.shift(shiftDelta, 0);
            tokens.markSync(syncTokenI+1);
        }
        else
        {
            sync = null;
        }

        CurlyToken parseStartCurly;
        int parseStartOffset;
        if (lastUnaffectedTokenI >= 0)
        {
            parseStartCurly = (CurlyToken) tokens.get(lastUnaffectedTokenI);
            parseStartOffset = parseStartCurly.getOffset();
            tokens.truncate(lastUnaffectedTokenI);
        }
        else
        {
            parseStartOffset = 0;
            parseStartCurly = null;
            tokens.truncate(0);
        }
        
        lexer.reset(null, doc, parseStartCurly);
        try
        {
            PerlToken t;
            while ((t = nextToken(lexer)).getType() != Token.EOF_TYPE)
            {
                if (t.equals(sync))
                {
                    int lineShiftDelta = t.getLine() - sync.getLine();
                    tokens.add(t);

                    // add remaining tokens unchanged, except for shifted
                    // token offsets and possibly adjusted bracket
                    // nesting levels
                    
                    int start = tokens.size();
                    tokens.addSync();
                    int pc = lexer.getCurlyLevel();
                    int tokenCount = tokens.size();

                    for (int i = start; i < tokenCount; i++)
                    {
                        PerlToken pt = (PerlToken) tokens.get(i);
                        pt.shift(shiftDelta, lineShiftDelta);
                        if (pt instanceof CurlyToken)
                        {
                            if (pt.getType() == PerlTokenTypes.OPEN_CURLY)
                            {
                                ((CurlyToken) pt).setLevel(pc);
                                pc++;
                            }
                            else
                            {
                                pc--;
                                ((CurlyToken) pt).setLevel(pc);
                            }
                        }
                    }
                    assert tokens.noOverlaps();
                    return new Region(parseStartOffset, sync.getOffset() - parseStartOffset);
                }
                else tokens.add(t);
            }
        }
        catch (TokenStreamException e)
        {
            if (e.getMessage().indexOf("unrecognized character at document offset") != -1)
            {
                log.log(new Status(
                    IStatus.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Could not parse source file due to an unrecognized character. " +
                    "Check if the text file encoding is set correctly in Preferences/Editors.",
                    e
                    ));
            }
            else
            {
                log.log(new Status(
                    IStatus.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Could not parse source file. Report this exception as " +
                    "a bug, including the text fragment which triggers it, " +
                    "if possible.",
                    e
                    ));
            }
        }
        
        initialized = true;
        return new Region(parseStartOffset, doc.getLength() - parseStartOffset);    
    }
    
    private ITypedRegion getPartitionImpl(int offset, boolean preferOpenPartitions)
    {
        if (!initialized) initialize();
        if (offset == doc.getLength()) return new TypedRegion(offset, 0, PartitionTypes.DEFAULT);

        int i = tokens.getTokenIndexPreceding(offset);
        
        if (i == -1) // offset lies before the first token
        {
            if (!tokens.isEmpty())
                return new TypedRegion(
                    0,
                    ((PerlToken) tokens.get(0)).getOffset(),
                    PartitionTypes.DEFAULT);
            else
                return new TypedRegion(0, 0, PartitionTypes.DEFAULT);
        }
        else
        {
            PerlToken t = (PerlToken) tokens.get(i);
            if (t.includes(offset))
            {
                if (preferOpenPartitions && t.getOffset() == offset)
                {
                    // imaginary zero-length open partition before each token
                    return new TypedRegion(offset, 0, PartitionTypes.DEFAULT);
                }   
                else
                {
                    if (i > 0 &&
                        t.getOffset() == offset &&
                        t.getType() == PerlTokenTypes.WS)
                    {
                        // if we are asked for partition at the beginning of
                        // a whitespace token following a non-whitespace token
                        // and preferOpenPartitions is false, we return the
                        // previous non-whitespace partition instead
                        
                        PerlToken t2 = (PerlToken) tokens.get(i-1);
                        if (t2.getOffset() + t2.getLength() == offset)
                        {
                            return token2Region(t2, i-1);
                        }
                        else return token2Region(t, i);
                    }
                    else return token2Region(t, i);
                }
            }

            if (i < tokens.size() - 1)
            {
                // offset lies in a gap between tokens
                PerlToken t2 = (PerlToken) tokens.get(i+1);
                return new TypedRegion(
                    t.getOffset() + t.getLength(),
                    t2.getOffset() - (t.getOffset() + t.getLength()),
                    PartitionTypes.DEFAULT);
            }
            else                
            {
                // offset lies after the last token
                return new TypedRegion(
                    t.getOffset() + t.getLength(),
                    doc.getLength() - (t.getOffset() + t.getLength()),
                    PartitionTypes.DEFAULT);
            }
        }
    }
    
    private String getTokenContentType(PerlToken t, int i)
    {   
        switch (t.getType())
        {
        case PerlTokenTypes.COMMENT:
            return PartitionTypes.COMMENT;
        case PerlTokenTypes.OPEN_HEREDOC:
        case PerlTokenTypes.HEREDOC_LINE:
        case PerlTokenTypes.CLOSE_HEREDOC:
        case PerlTokenTypes.OPEN_POD:
        case PerlTokenTypes.POD_BODY:
        case PerlTokenTypes.CLOSE_POD:
            return PartitionTypes.POD;
        case PerlTokenTypes.KEYWORD1:
        case PerlTokenTypes.KEYWORD_USE:
        case PerlTokenTypes.KEYWORD_SUB:
        case PerlTokenTypes.KEYWORD_PACKAGE:
            return PartitionTypes.KEYWORD1;
        case PerlTokenTypes.KEYWORD2:
            return PartitionTypes.KEYWORD2;
        case PerlTokenTypes.VAR:
        case PerlTokenTypes.SPECIAL_VAR:
            return PartitionTypes.VARIABLE;
        case PerlTokenTypes.OPEN_SQUOTE:
        case PerlTokenTypes.OPEN_DQUOTE:
        case PerlTokenTypes.OPEN_BQUOTE:
            return PartitionTypes.LITERAL1;                    
        case PerlTokenTypes.MATCH_EXPR:
        case PerlTokenTypes.SUBST_EXPR:
        case PerlTokenTypes.OPEN_QUOTE: 
        case PerlTokenTypes.OPEN_SLASH:
        case PerlTokenTypes.OPEN_QMARK:
            return PartitionTypes.LITERAL2;
        case PerlTokenTypes.CLOSE_QUOTE:
        case PerlTokenTypes.STRING_BODY:
        case PerlTokenTypes.STRING_SUFFIX:
            return i > 0
                ? getTokenContentType((PerlToken) tokens.get(i-1), i-1)
                : PartitionTypes.DEFAULT;
        case PerlTokenTypes.NUMBER:
            return PartitionTypes.NUMBER;
        default:
            if (t instanceof OperatorToken ||
                t instanceof CurlyToken) return PartitionTypes.OPERATOR;
            else return PartitionTypes.DEFAULT;
        }
    }
    
    private void initialize()
    {
        documentChanged2(new DocumentEvent(doc, 0, doc.getLength(), doc.get()));
    }
    
    private void dumpPartitioning(
        int offset,
        int length,
        boolean includeZeroLengthPartitions,
        ITypedRegion[] p)
    {
        System.err.println(
            "computePartitioning " + includeZeroLengthPartitions +
            " " + offset + ":" + length + " start...");

        for (int i = 0; i < p.length; i++)
        {
            System.err.println(
                i + ": " + p[i].getOffset() + ":" +
                p[i].getLength() + ":" + p[i].getType());
        }
        System.err.println("---- end of computePartitioning");

    }
    
    private PerlToken nextToken(PerlMultiLexer lexer) throws TokenStreamException
    {
        try
        {
            return (PerlToken) lexer.nextToken();
        }
        catch (TokenStreamException e)
        {
            lexer.recover();
            System.err.println(
                "WARNING: PerlMultiLexer recovery performed at " +
                e.getMessage()); // TODO log it when in development mode?
            return (PerlToken) lexer.nextToken();
        }
    }
    
    private TypedRegion token2Region(PerlToken t, int i)
    {
        return new TypedRegion(
            t.getOffset(),
            t.getLength(),
            getTokenContentType(t, i));
    }
}