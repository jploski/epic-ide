package org.epic.perleditor.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Responsible for coloring individual partitions created by
 * {@link org.epic.perleditor.editors.PerlPartitioner}.
 * 
 * @author jploski
 */
public class PerlPresentationReconciler extends PresentationReconciler
{        
    public PerlPresentationReconciler(IPreferenceStore prefs)
    {
        setDocumentPartitioning(PartitionTypes.PERL_PARTITIONING);
        
        DefaultDamagerRepairer ddr =
            new DefaultDamagerRepairer(new ColoringScanner(prefs));
        
        String[] contentTypes = PartitionTypes.getTypes();
        for (int i = 0; i < contentTypes.length; i++)
        {
            setDamager(ddr, contentTypes[i]);
            setRepairer(ddr, contentTypes[i]);
        }
    }
    
    private static class ColoringScanner implements ITokenScanner
    {
        private final IPreferenceStore prefs;
        private final IToken[] tokens;
        private final int[] lengths;
        private final Map<String, Integer> textAttributes;
        private int offset;
        private int tokenI;
        
        // The colors mappings defined here are meant to be more or less
        // compatible with EPIC 0.3.12, they do not necessarily make sense...
        //
        private static String[] colors = new String[] {
            PartitionTypes.COMMENT,
            PreferenceConstants.EDITOR_COMMENT1_COLOR,
            PreferenceConstants.EDITOR_COMMENT1_COLOR_BOLD,
            PreferenceConstants.EDITOR_COMMENT1_COLOR_ITALIC,
            PartitionTypes.DEFAULT,
            PreferenceConstants.EDITOR_FOREGROUND_COLOR,
            null,
            null,
            PartitionTypes.KEYWORD1,
            PreferenceConstants.EDITOR_KEYWORD1_COLOR,
            PreferenceConstants.EDITOR_KEYWORD1_COLOR_BOLD,
            PreferenceConstants.EDITOR_KEYWORD1_COLOR_ITALIC,
            PartitionTypes.KEYWORD2,
            PreferenceConstants.EDITOR_KEYWORD2_COLOR,
            PreferenceConstants.EDITOR_KEYWORD2_COLOR_BOLD,
            PreferenceConstants.EDITOR_KEYWORD2_COLOR_ITALIC,
            PartitionTypes.LITERAL1,
            PreferenceConstants.EDITOR_LITERAL1_COLOR,
            PreferenceConstants.EDITOR_LITERAL1_COLOR_BOLD,
            PreferenceConstants.EDITOR_LITERAL1_COLOR_ITALIC,
            PartitionTypes.LITERAL2,
            PreferenceConstants.EDITOR_LITERAL2_COLOR,
            PreferenceConstants.EDITOR_LITERAL2_COLOR_BOLD,
            PreferenceConstants.EDITOR_LITERAL2_COLOR_ITALIC,
            PartitionTypes.POD,
            PreferenceConstants.EDITOR_COMMENT2_COLOR,
            PreferenceConstants.EDITOR_COMMENT2_COLOR_BOLD,
            PreferenceConstants.EDITOR_COMMENT2_COLOR_ITALIC,
            PartitionTypes.VARIABLE,
            PreferenceConstants.EDITOR_VARIABLE_COLOR,
            PreferenceConstants.EDITOR_VARIABLE_COLOR_BOLD,
            PreferenceConstants.EDITOR_VARIABLE_COLOR_ITALIC,
            PartitionTypes.NUMBER,
            PreferenceConstants.EDITOR_NUMBER_COLOR,
            PreferenceConstants.EDITOR_NUMBER_COLOR_BOLD,
            PreferenceConstants.EDITOR_NUMBER_COLOR_ITALIC,
            PartitionTypes.OPERATOR,
            PreferenceConstants.EDITOR_OPERATOR_COLOR,
            PreferenceConstants.EDITOR_OPERATOR_COLOR_BOLD,
            PreferenceConstants.EDITOR_OPERATOR_COLOR_ITALIC,
        };

        public ColoringScanner(IPreferenceStore prefs)
        {            
            this.prefs = prefs;

            tokens = new IToken[3];
            tokens[0] = Token.EOF;
            lengths = new int[3];
            
            textAttributes = new HashMap<String, Integer>();
            for (int i = 0; i < colors.length; i += 4)
                textAttributes.put(colors[i], new Integer(i));
        }

        public void setRange(IDocument document, int offset, int length)
        {
            this.offset = offset;
            this.tokenI = -1;
            
            try
            {
                String type = PartitionTypes.getPerlPartition(document, offset).getType();
                
                if (type.equals(PartitionTypes.VARIABLE))
                {
                    // render first character (such as $, @ or %) in default color,
                    // all other characters in specified color
                    tokens[0] = new Token(attr(PartitionTypes.DEFAULT));                    
                    tokens[1] = new Token(attr(type));                    
                    tokens[2] = Token.EOF;
                    lengths[0] = 1;
                    lengths[1] = length - 1;
                    lengths[2] = 0;
                }
                else
                {
                    // render all characters in specified color
                    tokens[0] = new Token(attr(type));
                    tokens[1] = Token.EOF;
                    lengths[0] = length;
                    lengths[1] = 0;
                }
            }
            catch (BadLocationException e) { }
        }

        public IToken nextToken()
        {
            if (tokenI >= 0) offset += lengths[tokenI];
            IToken ret = tokens[tokenI + 1];
            if (!ret.isEOF()) tokenI++;
            return ret;
        }

        public int getTokenOffset()
        {
            return offset;
        }

        public int getTokenLength()
        {
            return lengths[tokenI];
        }
        
        private TextAttribute attr(String type)
        {
            int index = textAttributes.get(type).intValue();
            
            String colorPref = colors[index+1];
            String boldPref = colors[index+2];
            String italicPref = colors[index+3];
            
            PerlEditorPlugin p = PerlEditorPlugin.getDefault();

            int style = SWT.NORMAL;
            if (boldPref != null && prefs.getBoolean(boldPref))
                style |= SWT.BOLD;
            if (italicPref != null && prefs.getBoolean(italicPref))
                style |= SWT.ITALIC;
            
            return new TextAttribute(getColor(p, colorPref), null, style);
        }
        
        private Color getColor(PerlEditorPlugin p, String colorPref)
        {
            return p.getColor(PreferenceConverter.getColor(prefs, colorPref));
        }
    }
}
