/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     jploski - adapted for EPIC from JavaPairMatcher
 *******************************************************************************/
package org.epic.perleditor.editors;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Helper class for matching pairs of characters.
 * 
 * @author jploski
 */
public class PerlPairMatcher implements ICharacterPairMatcher
{
    private static final char NO_PRETENDED_CHAR = '\u0000';
    
    private final ILog log;
    private ISourceViewer viewer;
	private IDocument fDocument;
	private int fOffset;
	private int fStartPos;
	private int fEndPos;
	private int fAnchor;

    private final static char[] BRACKETS =
        { '{', '}', '(', ')', '[', ']', '<', '>' };

	public PerlPairMatcher(ILog log)
    {
        this.log = log;
	}

	public IRegion match(IDocument document, int offset)
    {
		fOffset = offset;
		if (fOffset < 0) return null;

		fDocument = document;
		if (fDocument != null &&
            fDocument.getLength() > 0 &&
            matchPairsAt(NO_PRETENDED_CHAR) &&
            fStartPos != fEndPos)
        {
			return new Region(fStartPos, fEndPos - fStartPos + 1);
        }
		return null;
	}
    
    /**
     * Same as {@link #match}, but pretend that the given character
     * is in the document at offset-1. Useful for finding the matching
     * peer for a character which is about to be inserted.   
     */
    public IRegion match(IDocument document, int offset, char pretendPrevChar)
    {
        fOffset = offset;
        if (fOffset < 0) return null;

        fDocument = document;
        if (fDocument != null &&
            fDocument.getLength() > 0 &&
            matchPairsAt(pretendPrevChar) &&
            fStartPos != fEndPos)
        {
            return new Region(fStartPos, fEndPos - fStartPos + 1);
        }
        return null;
    }
    
    /**
     * Sets the viewer used to optimize searching: if viewer != null,
     * only the visible region of the viewer will be considered while
     * searching for matches.
     */
    public void setViewer(ISourceViewer viewer)
    {
        this.viewer = viewer;
    }

	public int getAnchor()
    {
		return fAnchor;
	}

    public int getEndPos()
    {
        return fEndPos;
    }
    
    public int getStartPos()
    {
        return fStartPos;
    }

	public void dispose()
    {
		clear();
		fDocument= null;
	}

	public void clear()
    {
	}

	private boolean matchPairsAt(char pretendPrevChar)
    {
		int i;
		int pairIndex1 = BRACKETS.length;
		int pairIndex2 = BRACKETS.length;

		fStartPos = -1;
		fEndPos = -1;

		// get the char preceding the start position
		try
        {
            boolean pretendPeer = pretendPrevChar != NO_PRETENDED_CHAR;
			char prevChar =
                pretendPeer
                ? pretendPrevChar
                : fDocument.getChar(Math.max(fOffset - 1, 0));

			// check if the character before the activation point is
            // an opening peer character (if so: we'll search forwards)
			for (i = 0; i < BRACKETS.length; i += 2)
            {
				if (prevChar == BRACKETS[i])
                {
					fStartPos = fOffset - 1;
					pairIndex1 = i;
				}
			}

			// check if the character before the activation point is
            // a closing peer character (if so: we'll search backwards)
			for (i = 1; i < BRACKETS.length; i += 2)
            {
				if (prevChar == BRACKETS[i])
                {
					fEndPos = fOffset - 1;
					pairIndex2 = i;
				}
			}

			if (fEndPos > -1)
            {
                //if (pretendPeer) fEndPos--;
				fAnchor = RIGHT;                
				fStartPos = searchForOpeningPeer(
                    fEndPos,
                    BRACKETS[pairIndex2 - 1],
                    BRACKETS[pairIndex2],
                    fDocument,
                    pretendPeer);
				if (fStartPos > -1) return true;
				else fEndPos= -1;
			}
            else if (fStartPos > -1)
            {
                //if (pretendPeer) fStartPos++;
				fAnchor = LEFT;
				fEndPos = searchForClosingPeer(
                    fStartPos,
                    BRACKETS[pairIndex1],
                    BRACKETS[pairIndex1 + 1],
                    fDocument,
                    pretendPeer);
				if (fEndPos > -1) return true;
				else fStartPos= -1;
			}
		}
        catch (BadLocationException e)
        {
            // this one should never occur
            log.log(
                new Status(Status.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Unexpected exception; report it as a bug " +
                    "in plug-in " + PerlEditorPlugin.getPluginId(),
                    e));
        }
		return false;
	}

	private int searchForClosingPeer(
        int offset,
        char openingPeer,
        char closingPeer,
        IDocument document,
        boolean pretendPeer) throws BadLocationException
    {
        int end =
            viewer != null
            ? Math.min(viewer.getBottomIndexEndOffset()+1, document.getLength())
            : document.getLength();
        
		return searchForPeer(
            offset,
            end,
            closingPeer,
            openingPeer,
            1,
            document,
            pretendPeer);
    }

	private int searchForOpeningPeer(
        int offset,
        char openingPeer,
        char closingPeer,
        IDocument document,
        boolean pretendPeer) throws BadLocationException
    {
        int end =
            viewer != null
            ? Math.max(viewer.getTopIndexStartOffset(), 0)
            : 0;
        
        return searchForPeer(
            offset,
            end,
            openingPeer,
            closingPeer,
            -1,
            document,
            pretendPeer);
	}
    
    /**
     * Finds a matching peer character, starting the (forward or backward)
     * search from the specified offset in a given document. If the starting
     * offset is contained within a literal or comment, only matches within
     * the same literal or comment are considered. Otherwise, only matches
     * outside of literals and comments are considered.  
     * 
     * @param start         start offset to initialize searching
     * @param end           end offset to terminate searching
     * @param searchFor     peer character which we are looking for
     * @param searchFrom    matching character for searchFor
     * @param direction     1 to search forwards, -1 to search backwards
     * @param document      document in which to search
     * @param pretendPeer   true if we are running in make-believe mode:
     *                      we just pretend that the searchFrom character
     *                      is in the document
     * @return position of the matching searchFor character
     *         or -1 if such character is not found in the searched region
     */
    private int searchForPeer(
        int start,
        int end,
        char searchFor,
        char searchFrom,
        int direction,
        IDocument document,
        boolean pretendPeer)
        throws BadLocationException
    {
        // n is the running count of 'searchFrom' chars not matched by
        // 'searchFor' chars:
        int n = 0;
        int i = start;
        int min = start < end ? start : end;
        int max = start > end ? start+1 : end;
        
        if (start > end && pretendPeer)
        {
            // Normally, when setting out to search for an opening peer
            // backwards, we go one step forward (see above) to make the
            // first iteration of the search loop find the closing peer
            // character. However, in the pretendPeer case, this closing
            // peer character is not present in the document.
            // We have to adjust accordingly to create the illusion
            // of its presence:
            n++; i--; max--;
        }
        
        String text = document.get(min, max - min);
        if (text.length() == 0) return -1;
        
        String partitionType = PartitionTypes.getPerlPartition(document, i).getType();
        boolean inLiteralOrComment = isLiteralOrComment(partitionType);
        
        while (direction == 1 && i < end ||
               direction == -1 && i >= end)
        {
            char c = text.charAt(i - min);
            if (c == searchFor || c == searchFrom)
            {
                int prevI = i;
                i = checkCurrentPartition(
                    document, inLiteralOrComment, i, direction, partitionType);
                if (i == -1) break;
                else if (i != prevI) continue;    
            }
            
            if (c == searchFor && --n == 0) return i;
            if (c == searchFrom) n++;
            i += direction;
        }
        return -1;
    }
    
    /**
     * @return true, if the given partitionType is a literal or comment,
     *         false otherwise
     */
    private boolean isLiteralOrComment(String partitionType)
    {
        return
            partitionType.indexOf("LITERAL1") != -1 ||
            partitionType.indexOf("LITERAL2") != -1 ||
            partitionType.indexOf("COMMENT") != -1;
    }
    
    /**
     * @return -1 if search should be terminated because of leaving
     *            the initial (literal or comment) partition;
     *         i if search should be continued normally;
     *         != i if search should be continued from the returned offset
     *         (skipping a literal or comment partition)
     */
    private int checkCurrentPartition(
        IDocument document,
        boolean inLiteralOrComment,
        int i,
        int direction,
        String partitionType) throws BadLocationException
    {
        if (inLiteralOrComment)
        {
            if (!PartitionTypes.getPerlPartition(document, i).getType().equals(partitionType))
            {
                // we started from within a literal or comment partition
                // and didn't found a matching peer so far - don't leave
                // that partition, give up
                return -1;
            }
        }
        else
        {
            ITypedRegion partition = PartitionTypes.getPerlPartition(document, i);
            if (isLiteralOrComment(partition.getType()))
            {
                // we started from non-literal/comment partition;
                // ignore any such partitions while looking for
                // the matching peer
                
                int startIndex = partition.getOffset();
                int endIndex = startIndex + partition.getLength();

                return (direction == 1) ? endIndex : startIndex - 1;
            }
        }
        return i;
    }
}