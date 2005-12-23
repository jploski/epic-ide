package org.epic.perleditor.editors;

import java.util.HashMap;
import java.util.Map;

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
 * Reponsible for coloring individual partitions created by
 * {@link org.epic.perleditor.editors.PerlPartitioner}.
 * 
 * @author jploski
 */
public class PerlPresentationReconciler extends PresentationReconciler
{        
    public PerlPresentationReconciler(IPreferenceStore prefs)
    {
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
        private final Map textAttributes;
        private int offset;
        private int tokenI;
        
        // The colors mappings defined here are meant to be more or less
        // compatible with EPIC 0.3.12, they do not necessarily make sense...
        //
        private static String[] colors = new String[] {
            PartitionTypes.COMMENT,
            PreferenceConstants.EDITOR_COMMENT1_COLOR,
            PreferenceConstants.EDITOR_COMMENT1_COLOR_BOLD,
            PartitionTypes.DEFAULT,
            PreferenceConstants.EDITOR_FOREGROUND_COLOR,
            null,
            PartitionTypes.KEYWORD1,
            PreferenceConstants.EDITOR_KEYWORD1_COLOR,
            PreferenceConstants.EDITOR_KEYWORD1_COLOR_BOLD,
            PartitionTypes.KEYWORD2,
            PreferenceConstants.EDITOR_KEYWORD2_COLOR,
            PreferenceConstants.EDITOR_KEYWORD2_COLOR_BOLD,
            PartitionTypes.LITERAL1,
            PreferenceConstants.EDITOR_LITERAL1_COLOR,
            PreferenceConstants.EDITOR_LITERAL1_COLOR_BOLD,
            PartitionTypes.LITERAL2,
            PreferenceConstants.EDITOR_LITERAL2_COLOR,
            PreferenceConstants.EDITOR_LITERAL2_COLOR_BOLD,
            PartitionTypes.POD,
            PreferenceConstants.EDITOR_COMMENT2_COLOR,
            PreferenceConstants.EDITOR_COMMENT2_COLOR_BOLD,
            PartitionTypes.VARIABLE,
            PreferenceConstants.EDITOR_KEYWORD2_COLOR,
            PreferenceConstants.EDITOR_KEYWORD2_COLOR_BOLD,
            PartitionTypes.NUMBER,
            PreferenceConstants.EDITOR_NUMBER_COLOR,
            PreferenceConstants.EDITOR_NUMBER_COLOR_BOLD,
            PartitionTypes.OPERATOR,
            PreferenceConstants.EDITOR_OPERATOR_COLOR,
            PreferenceConstants.EDITOR_OPERATOR_COLOR_BOLD,
        };

        public ColoringScanner(IPreferenceStore prefs)
        {            
            this.prefs = prefs;

            tokens = new IToken[3];
            tokens[0] = Token.EOF;
            lengths = new int[3];
            
            textAttributes = new HashMap();
            for (int i = 0; i < colors.length; i += 3)
                textAttributes.put(colors[i], new Integer(i));
        }

        public void setRange(IDocument document, int offset, int length)
        {
//System.err.println("ColoringScanner.setRange " + offset + ":" + length);
            this.offset = offset;
            this.tokenI = -1;
            
            try
            {
                String type = document.getPartition(offset).getType();
                
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
            int index = ((Integer) textAttributes.get(type)).intValue();
            
            String colorPref = colors[index+1];
            String boldPref = colors[index+2];
            
            PerlEditorPlugin p = PerlEditorPlugin.getDefault();

            int style = SWT.NORMAL;
            if (boldPref != null && prefs.getBoolean(boldPref))
                style = SWT.BOLD;
            
            Color bgColor =
                prefs.getBoolean(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR)
                ? null
                : getColor(p, PreferenceConstants.EDITOR_BACKGROUND_COLOR);

            
            return new TextAttribute(
                getColor(p, colorPref),
                bgColor,
                style);
        }
        
        private Color getColor(PerlEditorPlugin p, String colorPref)
        {
            return p.getColor(PreferenceConverter.getColor(prefs, colorPref));
        }
    }
}
