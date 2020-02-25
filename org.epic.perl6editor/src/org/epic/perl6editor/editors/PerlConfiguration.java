package org.epic.perl6editor.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.epic.perl6editor.scanner.TagScanner;
import org.epic.perl6editor.scanner.PartitionScanner;

public class PerlConfiguration extends SourceViewerConfiguration
{

	private PerlDoubleClickStrategy doubleClickStrategy;
	private TagScanner tagScanner;
	private RuleScanner scanner;
	private ColorManager colorManager;

	public PerlConfiguration( ColorManager colorManager )
	{
		this.colorManager = colorManager;
	}

	public String[] getConfiguredContentTypes( ISourceViewer sourceViewer )
	{
		return new String[]
		{
			IDocument.DEFAULT_CONTENT_TYPE,
			PartitionScanner.P6_COMMENT,
			PartitionScanner.P6_TAG
		};
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,	String contentType )
	{
		if (doubleClickStrategy == null)
			doubleClickStrategy = new PerlDoubleClickStrategy();

		return doubleClickStrategy;
	}

	protected RuleScanner getXMLScanner()
	{
		if ( scanner == null )
		{
			scanner = new RuleScanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IColorConstants.DEFAULT))));
		}

		return scanner;
	}

	protected TagScanner getXMLTagScanner()
	{
		if ( tagScanner == null )
		{
			tagScanner = new TagScanner( colorManager );
			tagScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor( IColorConstants.TAG ))));
		}

		return tagScanner;
	}

	public IPresentationReconciler getPresentationReconciler( ISourceViewer sourceViewer )
	{
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr =	new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, PartitionScanner.P6_TAG);
		reconciler.setRepairer(dr, PartitionScanner.P6_TAG);

		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IColorConstants.P6_COMMENT)));
		reconciler.setDamager(ndr, PartitionScanner.P6_COMMENT);
		reconciler.setRepairer(ndr, PartitionScanner.P6_COMMENT);

		return reconciler;
	}

}

// END