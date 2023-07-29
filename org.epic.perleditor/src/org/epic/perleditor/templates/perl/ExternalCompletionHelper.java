package org.epic.perleditor.templates.perl;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.epic.perleditor.editors.util.ExternalAutoComplete;

public class ExternalCompletionHelper  {
    
    private static ExternalCompletionHelper gInstance;
    
    public static ExternalCompletionHelper getInstance() {
        if (gInstance == null) {
            synchronized(ExternalCompletionHelper.class) {
                if (gInstance == null) {
                    gInstance = new ExternalCompletionHelper();
                }
            }
        }
        return gInstance;
    }
    
    private ExternalCompletionHelper() {
    }
    
    public ICompletionProposal[] getProposals(
            String documentText, int documentOffset, int selectionLength, IPath path,
            ITextViewer viewer
    ) {
        if (documentText.length() == 0) return new ICompletionProposal[0];

        String[] str = ExternalAutoComplete.getProposals(documentText, documentOffset, selectionLength, path);
        ICompletionProposal[] proposals = new ICompletionProposal[str.length];

        for (int i = 0; i < str.length; i++) {
            int firstCommaIndex = str[i].indexOf(',');
            int secondCommaIndex = str[i].indexOf(',', firstCommaIndex+1);
            int thirdCommaIndex = str[i].indexOf(',', secondCommaIndex+1);
            
            String replacementString = str[i];
            int replacementOffset = documentOffset,
                replacementLength = 0,
                cursorPosition = documentOffset + replacementString.length();
            
            try {

                replacementOffset = Integer.parseInt(str[i].substring(0, firstCommaIndex));
                replacementLength = Integer.parseInt(str[i].substring(firstCommaIndex+1, secondCommaIndex));
                cursorPosition = Integer.parseInt(str[i].substring(secondCommaIndex+1, thirdCommaIndex));
                replacementString = str[i].substring(thirdCommaIndex+1);
            } catch (NumberFormatException e) {}
            
            proposals[i] = new PositionBasedCompletionProposal(
                replacementString,
                new Position(replacementOffset, replacementLength),
                cursorPosition);
        }
        return proposals;
    }
    
    // Cf. org.eclipse.jface.text.templates.PositionBasedCompletionProposal
    class PositionBasedCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2 {

        /** The replacement string */
        private final String fReplacementString;
        /** The replacement position. */
        private final Position fReplacementPosition;
        /** The cursor position after this proposal has been applied, relative to fReplacementPosition */
        private final int fCursorPosition;

        public PositionBasedCompletionProposal(
            String replacementString,
            Position replacementPosition,
            int cursorPosition) {
            
            this.fReplacementString = replacementString;
            this.fReplacementPosition = replacementPosition;
            this.fCursorPosition = cursorPosition;
        }

        public void apply(IDocument document) {
        }

        public Point getSelection(IDocument document) {
            return new Point(fReplacementPosition.getOffset() + fCursorPosition, 0);
        }

        public String getAdditionalProposalInfo() {		
            return null;
        }

        public String getDisplayString() {
            return fReplacementString;
        }

        public Image getImage() {
            return null;
        }

        public IContextInformation getContextInformation() {
            return null;
        }

        @Override
        public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
            try {
                // We have to deal here with a (typical) situation in which the user has triggered
                // autocompletion and then continued to type in a prefix of the proposal (to narrow
                // down the proposals list) before accepting a proposal by hitting Enter.
                // In this case the replacement length returned by our helper script, which is only aware
                // of what was in the document at trigger time, is not valid. Rather, we must instead
                // replace the entire length of the proposal's prefix, including those characters
                // typed in between generating and accepting the autocompletion proposal.
                
                IDocument doc = viewer.getDocument();
                String prefix = doc.get(fReplacementPosition.getOffset(), offset - fReplacementPosition.getOffset());
                if (prefix.length() > 0 && fReplacementString.startsWith(prefix)) {
                    viewer.getDocument().replace(
                        fReplacementPosition.getOffset(),
                        prefix.length(),
                        fReplacementString);
                }
                else {
                    viewer.getDocument().replace(
                        fReplacementPosition.getOffset(),
                        fReplacementPosition.getLength(),
                        fReplacementString);
                }
            } catch (BadLocationException x) {
                // ignore
            }
        }

        @Override
        public void selected(ITextViewer viewer, boolean smartToggle) {            
        }

        @Override
        public void unselected(ITextViewer viewer) {
        }

        @Override
        public boolean validate(IDocument document, int offset, DocumentEvent event) {
            try {
                String content= document.get(fReplacementPosition.getOffset(), offset - fReplacementPosition.getOffset());
                if (fReplacementString.startsWith(content))
                    return true;
            } catch (BadLocationException e) {
                // ignore concurrently modified document
            }
            return false;
        }
    }
}
