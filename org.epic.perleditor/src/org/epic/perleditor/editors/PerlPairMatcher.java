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
            matchPairsAt() &&
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

	private boolean matchPairsAt()
    {
		int i;
		int pairIndex1 = BRACKETS.length;
		int pairIndex2 = BRACKETS.length;

		fStartPos = -1;
		fEndPos = -1;

		// get the char preceding the start position
		try
        {
			char prevChar = fDocument.getChar(Math.max(fOffset - 1, 0));
			// search for opening peer character next to the activation point
			for (i = 0; i < BRACKETS.length; i += 2)
            {
				if (prevChar == BRACKETS[i])
                {
					fStartPos = fOffset - 1;
					pairIndex1 = i;
				}
			}

			// search for closing peer character next to the activation point
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
				fAnchor = RIGHT;
				fStartPos = searchForOpeningPeer(fEndPos, BRACKETS[pairIndex2 - 1], BRACKETS[pairIndex2], fDocument);
				if (fStartPos > -1) return true;
				else fEndPos= -1;
			}
            else if (fStartPos > -1)
            {
				fAnchor = LEFT;
				fEndPos = searchForClosingPeer(fStartPos, BRACKETS[pairIndex1], BRACKETS[pairIndex1 + 1], fDocument);
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
        IDocument document) throws BadLocationException
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
            document);
    }

	private int searchForOpeningPeer(
        int offset,
        char openingPeer,
        char closingPeer,
        IDocument document) throws BadLocationException
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
            document);
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
     * @return position of the matching searchFor character
     *         or -1 if such character is not found in the searched region
     */
    private int searchForPeer(
        int start,
        int end,
        char searchFor,
        char searchFrom,
        int direction,
        IDocument document)
        throws BadLocationException
    {
        int n = 0; // running count of 'searchFrom' chars not matched by 'searchFor' chars
        int i = start;
        int min = start < end ? start : end;
        int max = start > end ? start+1 : end;
        
        String text = document.get(min, max - min);
        if (text.length() == 0) return -1;
        
        String partitionType = document.getPartition(i).getType();
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
            if (!document.getPartition(i).getType().equals(partitionType))
            {
                // we started from within a literal or comment partition
                // and didn't found a matching peer so far - don't leave
                // that partition, give up
                return -1;
            }
        }
        else
        {
            ITypedRegion partition = document.getPartition(i);
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