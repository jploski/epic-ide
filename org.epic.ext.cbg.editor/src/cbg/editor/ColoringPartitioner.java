package cbg.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

public class ColoringPartitioner extends DefaultPartitioner {

	public ColoringPartitioner(IPartitionTokenScanner scanner, String[] legalContentTypes) {
		super(scanner, legalContentTypes);
	}

	public ITypedRegion[] computePartitioning(int offset, int length) {
		if(fDocument == null) return new ITypedRegion[0];
		List list = new ArrayList();
		try {
			int endOffset = offset + length;
			
			//TODO Changed for EPIC (Not available in Eclipse 2.1)
			Position[] category = fDocument.getPositions(getManagingPositionCategories()[0]);
			//Position[] category = fDocument.getPositions(CONTENT_TYPES_CATEGORY);
			TypedPosition previous = null, current = null;
			int start, end, gapOffset;
			Position gap = null;
			for (int i = 0; i < category.length; i++) {
				current = (TypedPosition) category[i];
				gapOffset = (previous != null) ? previous.getOffset() + previous.getLength() : 0;
				if(gapOffset > current.getOffset()) {
					if(current != null) {
						//TODO Changed for EPIC (Not available in Eclipse 2.1)
						fDocument.removePosition(getManagingPositionCategories()[0], current);
						//fDocument.removePosition(CONTENT_TYPES_CATEGORY, current);
					}
					continue;
				} else {
					gap = new Position(gapOffset, current.getOffset() - gapOffset);
				}
				if (gap.getLength() > 0 && gap.overlapsWith(offset, length)) {
					start = Math.max(offset, gapOffset);
					end = Math.min(endOffset, gap.getOffset() + gap.getLength());
					list.add(new TypedRegion(start, end - start, IDocument.DEFAULT_CONTENT_TYPE));
				}
				if (current.overlapsWith(offset, length)) {
					start = Math.max(offset, current.getOffset());
					end = Math.min(endOffset, current.getOffset() + current.getLength());
					list.add(new TypedRegion(start, end - start, current.getType()));
				}
				previous = current;
			}
			if (previous != null) {
				gapOffset = previous.getOffset() + previous.getLength();
				gap = new Position(gapOffset, fDocument.getLength() - gapOffset);
				if (gap.getLength() > 0 && gap.overlapsWith(offset, length)) {
					start = Math.max(offset, gapOffset);
					end = Math.min(endOffset, fDocument.getLength());
					list.add(new TypedRegion(start, end - start, IDocument.DEFAULT_CONTENT_TYPE));
				}
			}
			if (list.isEmpty())
				list.add(new TypedRegion(offset, length, IDocument.DEFAULT_CONTENT_TYPE));
		} catch (BadPositionCategoryException x) {
			x.printStackTrace();
		}
		TypedRegion[] result = new TypedRegion[list.size()];
		list.toArray(result);
		return result;
	}

}
