package org.epic.perl6editor.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.epic.perl6editor.scanner.PartitionScanner;

public class PerlDocumentProvider extends FileDocumentProvider
{

	protected IDocument createDocument( Object element ) throws CoreException
	{
		IDocument document = super.createDocument( element );

		if ( document != null )
		{
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new PartitionScanner(),
					new String[] {
						PartitionScanner.P6_TAG,
						PartitionScanner.P6_COMMENT
					});

			partitioner.connect( document );
			document.setDocumentPartitioner( partitioner );

			try {
			System.out.println("# length: " + document.getLength());
			System.out.println("#  lines: " + document.getNumberOfLines());
			System.out.println("# line1L: " + document.getLineLength(1));
			System.out.println("#   Info: " + document.getLineInformation(9).getOffset());

			/*
			for ( int i = 1 ; i <= document.getNumberOfLines() ; i++ )
			{
				System.out.println("# ContTY: " + document.getContentType(i) + " ->> " + i );
				System.out.println("#   Info: " + document.getLineInformation(i));
			}
			*/
			}
			catch (Exception e) {}
		}

		return document;
	}
}

// END