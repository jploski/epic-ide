package org.epic.debug.ui.propertypages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;
import org.epic.core.util.StatusFactory;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLineBreakpoint;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlPartitioner;
import org.epic.perleditor.editors.PerlSourceViewerConfiguration;


/**
 *Implements the property page for a perl line breakpoint
 */
public class PerlLineBreakpointPage extends PropertyPage
{
    /*
     * TODO: a number of methods defined in this class dealing w/ creating widgets, error messages,
     * etc are ideal candiates for helper methods
     */

    //~ Instance fields

    private SourceViewer conditionText;

    private Button enableConditionButton;

    private Button enabledButton;

    private List<String> errors = new ArrayList<String>();

    //~ Methods

    /*
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    public boolean performOk()
    {
        IWorkspaceRunnable runnable =
            new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                getBreakpoint().setEnabled(enabledButton.getSelection());
                storeCondition();
            }
        };

        try
        {
            ResourcesPlugin.getWorkspace().run(runnable, null, 0, null);
        }
        catch (CoreException e)
        {
            logError(PropertyPageMessages.unableToStore, e);
        }

        return super.performOk();
    }

    /**
     * push an error message onto the stack
     */
    protected void addErrorMessage(String message)
    {
        /*
         * removing the message acts as if we are poping it off the stack, and re-adding it pushes
         * it back on in the last position.
         */
        errors.remove(message);
        errors.add(message);

        setErrorMessage(message);
        setValid(message == null);
    }

    protected Button createCheckButton(Composite parent, String text)
    {
        Button button = new Button(parent, SWT.CHECK | SWT.LEFT);
        button.setText(text);
        button.setFont(parent.getFont());
        button.setLayoutData(new GridData());
        return button;
    }

    protected Composite createComposite(Composite parent, int numColumns)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());

        GridLayout layout = new GridLayout();
        layout.numColumns = numColumns;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return composite;
    }

    /*
     * @see
     * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    protected Control createContents(Composite parent)
    {
        Composite composite = createComposite(parent, 1);
        noDefaultAndApplyButton();

        createLabels(composite);

        try
        {
            createEnabledButton(composite);
            createConditionEditor(composite);
        }
        catch (CoreException e)
        {
            logError(PropertyPageMessages.createContentsError, e);
        }

        setValid(true);

        // TODO change the dialog window title

        return composite;
    }

    protected Label createLabel(Composite parent, String text)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setFont(parent.getFont());
        label.setLayoutData(new GridData());

        return label;
    }

    protected Button createRadioButton(Composite parent, String text)
    {
        Button button = new Button(parent, SWT.RADIO | SWT.LEFT);
        button.setText(text);
        button.setFont(parent.getFont());
        button.setLayoutData(new GridData());

        return button;
    }

    protected Text createText(Composite parent, String initialValue)
    {
        Composite textComposite = createComposite(parent, 2);
        Text text = new Text(textComposite, SWT.SINGLE | SWT.BORDER);
        text.setText(initialValue);
        text.setFont(parent.getFont());
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return text;
    }

    protected PerlLineBreakpoint getBreakpoint()
    {
        return (PerlLineBreakpoint) getElement();
    }

    /**
     * pops an error message off the error stack
     */
    protected void removeErrorMessage(String message)
    {
        errors.remove(message);
        if (errors.isEmpty())
        {
            addErrorMessage(null);
        }
        else
        {
            addErrorMessage(errors.get(errors.size() - 1));
        }
    }

    /**
     * Creates the "suspend condition" editor
     */
    private void createCondition(Composite parent) throws CoreException
    {
        Composite composite = createComposite(parent, 1);
        PerlLineBreakpoint breakpoint = getBreakpoint();
        
        conditionText = new SourceViewer(
            composite, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        IDocument document = new Document(breakpoint.getCondition());        
        IDocumentPartitioner partitioner =
            new PerlPartitioner(PerlDebugPlugin.getDefault().getLog());
        
        document.setDocumentPartitioner(partitioner);
        partitioner.connect(document);
        conditionText.configure(new PerlSourceViewerConfiguration(
            PerlEditorPlugin.getDefault().getPreferenceStore(), null));
        conditionText.setEditable(true);
        conditionText.setDocument(document);

        Control control = conditionText.getControl();
        control.setFont(
            JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
        GridData data = new GridData(GridData.FILL_BOTH);
        data.widthHint = convertWidthInCharsToPixels(60);
        data.heightHint = convertHeightInCharsToPixels(5);
        control.setLayoutData(data);

        conditionText.addTextListener(new ITextListener() {
            public void textChanged(TextEvent event)
            {
                validateCondition();
            } });
    }

    /**
     * Creates the "suspend condition" group
     */
    private void createConditionEditor(Composite parent) throws CoreException
    {
        Composite groupComposite = createGroup(parent);
        PerlLineBreakpoint breakpoint = getBreakpoint();

        // TODO: content assist
        String caLabel = PropertyPageMessages.enableConditionWithoutCA;

        enableConditionButton = createCheckButton(groupComposite, caLabel);
        enableConditionButton.setSelection(breakpoint.isConditionEnabled());
        enableConditionButton.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    toggleConditionEnabled(enableConditionButton.getSelection());
                    validateCondition();
                }
            });

        // TODO: replace this with basic source editor
        createCondition(groupComposite);
        toggleConditionEnabled(enableConditionButton.getSelection());
    }

    /**
     * Creates the breakpoint "enabled" checkbox
     */
    private void createEnabledButton(Composite parent) throws CoreException
    {
        enabledButton = createCheckButton(parent, PropertyPageMessages.enabled);
        enabledButton.setSelection(getBreakpoint().isEnabled());
    }

    /**
     * Creates a group composite
     */
    private Composite createGroup(Composite parent)
    {
        Composite composite = new Group(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        composite.setLayout(new GridLayout());

        return composite;
    }

    /**
     * Creates the line number, etc label fields
     */
    private void createLabels(Composite parent)
    {
        Composite label = createComposite(parent, 2);
        PerlLineBreakpoint breakpoint = (PerlLineBreakpoint) getElement();

        String lineNumber = null;
        try
        {
            lineNumber = Integer.toString(breakpoint.getLineNumber());

            createLabel(label, PropertyPageMessages.line);
            createLabel(label, lineNumber);
        }
        catch (CoreException e)
        {
            logError(PropertyPageMessages.createContentsError, e);
        }
    }

    private void logError(String message, CoreException e)
    {
        String id = PerlDebugPlugin.getUniqueIdentifier();
        PerlDebugPlugin.getDefault().getLog().log(StatusFactory.createError(id, message, e));
    }

    /**
     * Stores the "condition" editor.
     */
    private void storeCondition() throws CoreException
    {
        PerlLineBreakpoint breakpoint = getBreakpoint();

        if (breakpoint.isConditionEnabled() != enableConditionButton.getSelection())
        {
            breakpoint.setConditionEnabled(enableConditionButton.getSelection());
        }

        if (!conditionText.getDocument().get().equals(breakpoint.getCondition()))
        {
            breakpoint.setCondition(conditionText.getDocument().get());
        }
    }

    /**
     * Toggles the "condition" editor as enabled/disabled
     */
    private void toggleConditionEnabled(boolean enabled)
    {
        conditionText.getTextWidget().setEnabled(enabled);
    }

    /**
     * Validates the "condition" editor
     */
    private void validateCondition()
    {
        if (! enableConditionButton.getSelection())
        {
            removeErrorMessage(PropertyPageMessages.conditionBlankErrorMessage);
            return;
        }

        if ("".equals(conditionText.getDocument().get().trim()))
        {
            addErrorMessage(PropertyPageMessages.conditionBlankErrorMessage);
        }
        else
        {
            removeErrorMessage(PropertyPageMessages.conditionBlankErrorMessage);
        }
    }
}
