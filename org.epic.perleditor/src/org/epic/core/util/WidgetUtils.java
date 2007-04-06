package org.epic.core.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class WidgetUtils
{
    public static Composite createComposite(Composite parent, int numColumns)
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

    public static Button createButton(Composite parent, String text, int style)
    {
        Button button = new Button(parent, style);
        button.setText(text);
        button.setFont(parent.getFont());
        button.setLayoutData(new GridData());

        return button;
    }

    public static Text createText(Composite parent, String initialValue)
    {
        Composite textComposite = createComposite(parent, 2);
        Text text = new Text(textComposite, SWT.SINGLE | SWT.BORDER);
        text.setText(initialValue);
        text.setFont(parent.getFont());
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return text;
    }

    public static void addSpacer(Composite parent)
    {
        createLabel(parent, "");
    }

    public static Label createLabel(Composite parent, String text)
    {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setFont(parent.getFont());
        label.setLayoutData(new GridData());

        return label;
    }


    /**
     * Creates a group composite
     */
    public static Composite createGroup(Composite parent, int fill)
    {
        Composite composite = new Group(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        composite.setLayoutData(new GridData(fill));
        composite.setLayout(new GridLayout());

        return composite;
    }
}
