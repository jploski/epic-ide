package org.epic.perleditor.editors;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
//import org.eclipse.jface.text.IDocumentPartitioner;
//import org.eclipse.jface.text.rules.DefaultPartitioner;
//import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.epic.perleditor.editors.perl.PerlPartitionScanner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IFileEditorInput;

import cbg.editor.ColoringDocumentProvider;

/** 
 * The JavaDocumentProvider provides the IDocuments used by java editors.
 */

//public class PerlDocumentProvider extends FileDocumentProvider {
public class PerlDocumentProvider extends ColoringDocumentProvider {

	//	private final static String[] TYPES= new String[] {PerlPartitionScanner.PERL_MULTI_LINE_COMMENT, PerlPartitionScanner.PERL_POD_COMMENT };

	private static PerlPartitionScanner fgScanner = null;

	public PerlDocumentProvider() {
		super();
	}

	/* (non-Javadoc)
	 * Method declared on AbstractDocumentProvider
	 */
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		//		if (document != null) {
		//			IDocumentPartitioner partitioner= createPerlPartitioner();
		//			document.setDocumentPartitioner(partitioner);
		//			partitioner.connect(document);
		//		}
		return document;
	}

	/**
	 * Return a partitioner for .pl files.
	 */
	//	 private IDocumentPartitioner createPerlPartitioner() {
	//		return new DefaultPartitioner(getPerlPartitionScanner(), TYPES);
	//	}

	/**
	 * Return a scanner for creating perl partitions.
	 */
	//	 private PerlPartitionScanner getPerlPartitionScanner() {
	//		if (fgScanner == null)
	//			fgScanner= new PerlPartitionScanner();
	//		return fgScanner;
	//	}

	/**
		 * Replaces createAnnotionModel of the super class.
		 */
	protected IAnnotationModel createAnnotationModel(Object element)
		throws CoreException {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput) element;
			return new PerlSourceAnnotationModel(input);
		} else {
			return super.createAnnotationModel(element);
		}
	}

}
