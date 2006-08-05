package org.epic.debug.ui.propertypages;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.dialogs.PropertyPage;

import org.epic.core.util.StatusFactory;

import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.PerlLineBreakpoint;

import gnu.regexp.RE;
import gnu.regexp.REException;

import java.util.ArrayList;
import java.util.List;


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

    private Button conditionHasChanged;

    private Button conditionIsTrue;

    private Button conditionMatchesRegExp;

    private Text conditionText;

    private Button enableConditionButton;

    private Button enabledButton;

    private List errors = new ArrayList();

    private Button hitCountButton;

    private Text hitCountText;

    private Text regExpText;

    private Label suspendWhenLabel;

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
                storeHitCount();
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

            // TODO: re-enable when implemented
            // createHitCount(composite);
            // createConditionEditor(composite);
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
            addErrorMessage((String) errors.get(errors.size() - 1));
        }
    }

    /**
     * Creates the "suspend condition" editor
     */
    private void createCondition(Composite parent) throws CoreException
    {
        Composite composite = createComposite(parent, 1);
        PerlLineBreakpoint breakpoint = getBreakpoint();

        String condition = breakpoint.getCondition();
        conditionText = createText(composite, (condition == null) ? "" : condition);

        conditionText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent event)
                {
                    validateCondition();
                }
            });
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

        createSuspendOptions(groupComposite);
        toggleSelectedSuspendOption(breakpoint);

        toggleConditionEnabled(enableConditionButton.getSelection());
        // XXX: remove when condition editor fully implemented
        enableConditionButton.setEnabled(false);
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
     * Creates the "hit count" checkbox and text field
     */
    private void createHitCount(Composite parent)
    {
        Composite composite = createComposite(parent, 2);
        PerlLineBreakpoint breakpoint = getBreakpoint();

        hitCountButton = createCheckButton(composite, PropertyPageMessages.hitCount);
        hitCountButton.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    hitCountText.setEnabled(hitCountButton.getSelection());
                    validateHitCount();
                }
            });

        int hits = breakpoint.getHitCount();
        hitCountButton.setSelection(false);

        String hitCount = "";
        if (hits > 0)
        {
            hitCount = Integer.toString(hits);
            hitCountButton.setSelection(true);
        }

        hitCountText = createText(composite, hitCount);
        if (hits <= 0)
        {
            hitCountText.setEnabled(false);
        }

        hitCountText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent event)
                {
                    validateHitCount();
                }
            });

        // XXX: remove when hit count is implemented!
        hitCountButton.setEnabled(false);
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

    /**
     * Creates the "regexp match" text field
     */
    private void createRegExpMatch(Composite parent)
    {
        Composite composite = createComposite(parent, 1);
        PerlLineBreakpoint breakpoint = getBreakpoint();

        // TODO: add ignore-case, etc checkbox options

        regExpText = createText(composite, breakpoint.getRegExp());
        regExpText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    validateRegExp();
                }
            });

        regExpText.setEnabled(conditionMatchesRegExp.getSelection());
    }

    /**
     * Creates the "suspend when" swt widgets
     */
    private void createSuspendOptions(Composite groupComposite)
    {
        suspendWhenLabel = createLabel(groupComposite, PropertyPageMessages.suspendWhen);

        conditionIsTrue = createRadioButton(groupComposite, PropertyPageMessages.conditionIsTrue);
        conditionHasChanged =
            createRadioButton(groupComposite, PropertyPageMessages.conditionHasChanged);
        conditionMatchesRegExp =
            createRadioButton(groupComposite, PropertyPageMessages.conditionMatchesRegExp);
        conditionMatchesRegExp.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    regExpText.setEnabled(conditionMatchesRegExp.getSelection());
                    validateRegExp();
                }
            });

        createRegExpMatch(groupComposite);
    }

    private void logError(String message, CoreException e)
    {
        String id = PerlDebugPlugin.getUniqueIdentifier();
        PerlDebugPlugin.getDefault().getLog().log(StatusFactory.createError(id, message, e));
    }

    /**
     * Stores the "condition" editor and the toggled "suspend when" condition
     */
    private void storeCondition() throws CoreException
    {
        PerlLineBreakpoint breakpoint = getBreakpoint();

        if (breakpoint.isConditionEnabled() != enableConditionButton.getSelection())
        {
            breakpoint.setConditionEnabled(enableConditionButton.getSelection());
        }

        if (! conditionText.getText().equals(breakpoint.getCondition()))
        {
            breakpoint.setCondition(conditionText.getText());
        }

        if (breakpoint.isConditionSuspendOnTrue() != conditionIsTrue.getSelection())
        {
            breakpoint.setConditionSuspendOnTrue(conditionIsTrue.getSelection());
        }

        if (breakpoint.isConditionSuspendOnChange() != conditionHasChanged.getSelection())
        {
            breakpoint.setConditionSuspendOnChange(conditionHasChanged.getSelection());
        }

        if (breakpoint.isConditionSuspendOnRegExp() != conditionMatchesRegExp.getSelection())
        {
            breakpoint.setConditionSuspendOnRegExp(conditionMatchesRegExp.getSelection());
        }

        if (! breakpoint.getRegExp().equals(conditionText.getText()))
        {
            breakpoint.setRegExp(conditionText.getText());
        }

    }

    /**
     * Stores the "hit count" text field
     */
    private void storeHitCount()
    {
        if (! hitCountButton.getSelection()) { return; }

        try
        {
            PerlLineBreakpoint breakpoint = getBreakpoint();
            breakpoint.setHitCount(Integer.parseInt(hitCountText.getText()));
        }
        catch (NumberFormatException e)
        {
            // will never happen due to validation routine
        }
    }

    /**
     * Toggles the "condition" editor and associated "suspend when" options as enabled/disabled
     */
    private void toggleConditionEnabled(boolean enabled)
    {
        conditionText.setEnabled(enabled);
        suspendWhenLabel.setEnabled(enabled);
        conditionIsTrue.setEnabled(enabled);
        conditionHasChanged.setEnabled(enabled);
        conditionMatchesRegExp.setEnabled(enabled);
    }

    /**
     * Toggles which "suspend when" condition is currently active on the page
     */
    private void toggleSelectedSuspendOption(PerlLineBreakpoint breakpoint) throws CoreException
    {
        if (breakpoint.isConditionSuspendOnTrue())
        {
            conditionIsTrue.setSelection(true);
        }
        else if (breakpoint.isConditionSuspendOnChange())
        {
            conditionHasChanged.setSelection(true);
        }
        else
        {
            conditionMatchesRegExp.setSelection(true);
            regExpText.setEnabled(true);
        }
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

        if ("".equals(conditionText.getText()))
        {
            addErrorMessage(PropertyPageMessages.conditionBlankErrorMessage);
        }
        else
        {
            removeErrorMessage(PropertyPageMessages.conditionBlankErrorMessage);
        }

    }

    /**
     * Validates the "hit count" text field
     */
    private void validateHitCount()
    {
        if (! hitCountButton.getSelection())
        {
            removeErrorMessage(PropertyPageMessages.hitCountErrorMessage);
            return;
        }

        try
        {
            int hitCount = Integer.parseInt(hitCountText.getText());
            if (hitCount < 1)
            {
                addErrorMessage(PropertyPageMessages.hitCountErrorMessage);
            }
            else
            {
                removeErrorMessage(PropertyPageMessages.hitCountErrorMessage);
            }
        }
        catch (NumberFormatException e)
        {
            addErrorMessage(PropertyPageMessages.hitCountErrorMessage);
        }
    }

    /**
     * Validates the "regexp match" text field
     */
    private void validateRegExp()
    {
        if (! conditionMatchesRegExp.getSelection())
        {
            removeErrorMessage(PropertyPageMessages.regExpBlankErrorMessage);
            return;
        }

        if ("".equals(regExpText.getText()))
        {
            addErrorMessage(PropertyPageMessages.regExpBlankErrorMessage);
            return;
        }

        removeErrorMessage(PropertyPageMessages.regExpBlankErrorMessage);

        try
        {
            // TODO: include regexp options when validating
            new RE(regExpText.getText());
            removeErrorMessage(PropertyPageMessages.regExpInvalidErrorMessage);
        }
        catch (REException e)
        {
            addErrorMessage(PropertyPageMessages.regExpInvalidErrorMessage);
            return;
        }
    }

}
