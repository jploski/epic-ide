/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.templates.perl.*;
import org.epic.perleditor.templates.ui.LinkedPositionManager;
import org.epic.perleditor.templates.ui.LinkedPositionUI;

/**
 * A template proposal.
 */
public class TemplateProposal implements IPerlCompletionProposal
{
	private final Template template;
	private final TemplateContext context;
	private final ITextViewer viewer;
	private final Image image;
	private final IRegion region;

	private TemplateBuffer templateBuffer;
	private IRegion selectedRegion; // initialized by apply()
		
	/**
	 * Creates a template proposal with a template and its context.
	 * @param template  the template
	 * @param context   the context in which the template was requested.
	 * @param image     the icon of the proposal.
	 */	
	public TemplateProposal(
        Template template,
        TemplateContext context,
        IRegion region,
        ITextViewer viewer,
        Image image)
    {
		this.template = template;
        this.context = context;
        this.viewer = viewer;
        this.image = image;
        this.region = region;
	}

	public void apply(IDocument document)
    {
	    try
        {
            if (templateBuffer == null)
                templateBuffer = context.evaluate(template);

			int start = region.getOffset();
			int end = region.getOffset() + region.getLength();
			
			// insert template string
            templateBuffer.indent(getLineIndent(document, region.getOffset())); 
			document.replace(start, end - start, templateBuffer.getString());	

			// translate positions
			LinkedPositionManager manager = new LinkedPositionManager(document);
			TemplatePosition[] variables = templateBuffer.getVariables();
			for (int i = 0; i < variables.length; i++)
            {
				TemplatePosition variable = variables[i];

				if (variable.isResolved()) continue;

				int[] offsets = variable.getOffsets();
				int length = variable.getLength();

				for (int j = 0; j < offsets.length; j++)
					manager.addPosition(offsets[j] + start, length);
			}

			LinkedPositionUI editor = new LinkedPositionUI(viewer, manager);
			editor.setFinalCaretOffset(getCaretOffset(templateBuffer) + start);
			editor.enter();

			selectedRegion = editor.getSelectedRegion();
		}
        catch (Exception e)
        {
            logException("Failed to apply template", e);
			openErrorDialog(e);
	    }
	}
	
	private static int getCaretOffset(TemplateBuffer buffer)
    {
	    TemplatePosition[] variables = buffer.getVariables();
		for (int i = 0; i != variables.length; i++)
        {
			TemplatePosition variable = variables[i];
			
			if (variable.getName().equals(PerlTemplateMessages.getString(
                "GlobalVariables.variable.name.cursor"))) //$NON-NLS-1$
            {
				return variable.getOffsets()[0];
            }
		}
		return buffer.getString().length();
	}

	public Point getSelection(IDocument document)
    {
		return new Point(
            selectedRegion.getOffset(), selectedRegion.getLength());
    }

	public String getAdditionalProposalInfo()
    {
	    try
        {
            if (templateBuffer == null)
                templateBuffer = context.evaluate(template);

			return templateBuffer.getString();
	    }
        catch (CoreException e)
        {
			logException("Failed to evaluate template", e);
			return null;
	    }
	}

	public String getDisplayString()
    {
		return
            template.getName() +
            TemplateMessages.getString("TemplateProposal.delimiter") + 
            template.getDescription(); //$NON-NLS-1$
	}

	public Image getImage()
    {
		return image;
	}

	public IContextInformation getContextInformation()
    {
		return null;
	}

	private void openErrorDialog(Exception e)
    {
		Shell shell = viewer.getTextWidget().getShell();
		MessageDialog.openError(
            shell,
            TemplateMessages.getString("TemplateEvaluator.error.title"),
            e.getMessage()); //$NON-NLS-1$
	}

	public int getRelevance()
    {
		if (context instanceof PerlUnitContext)
        {
			PerlUnitContext ctx = (PerlUnitContext) context;
			switch (ctx.getCharacterBeforeStart())
            {
			// high relevance after whitespace
			case ' ':
			case '\r':
			case '\n':
			case '\t':
				return 90;

			default:
				return 0;
			}
		}
        else return 90;
	}
    
    private String getLineIndent(IDocument doc, int offset)
    {
        try
        {
            int line = doc.getLineOfOffset(offset);
            int lineOffset = doc.getLineOffset(line);
            if (lineOffset == region.getOffset()) return "";
            
            String lineStart = doc.get(
                lineOffset, region.getOffset() - lineOffset);
            
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < lineStart.length(); i++)
            {
                char c = lineStart.charAt(i);
                if (Character.isWhitespace(c)) buf.append(c);
            }
            return buf.toString();
        }
        catch (BadLocationException e)
        {
            return "";
        }
    }
    
    private void logException(String msg, Throwable e)
    {
        Status status = new Status(
            IStatus.ERROR,
            PerlEditorPlugin.getPluginId(),
            IStatus.OK,
            msg,
            e);
        
        PerlEditorPlugin.getDefault().getLog().log(status);
    }
}
