/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.epic.debug.ui;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.epic.debug.PerlLaunchConfigurationConstants;

/**
 * An abstract field editor that manages a list of input values.
 * The editor displays a list containing the values, buttons for
 * adding and removing values, and Up and Down buttons to adjust
 * the order of elements in the list.
 * <p>
 * Subclasses must implement the <code>parseString</code>,
 * <code>createList</code>, and <code>getNewInputObject</code>
 * framework methods.
 * </p>
 */
public class ListEditor extends FieldEditor
{

	private IPropertyChangeListener mTab;

	//ILaunchConfigurationWorkingCopy mLaunch ;
	/**
	 * The list widget; <code>null</code> if none
	 * (before creation or after disposal).
	 */
	private List list;

	/**
	 * The button box containing the Add, Remove, Up, and Down buttons;
	 * <code>null</code> if none (before creation or after disposal).
	 */
	private Composite buttonBox;

	/**
	 * The Add button.
	 */
	private Button addButton;
	private Button editButton;
	/**
	 * The Remove button.
	 */
	private Button removeButton;

	/**
	 * The Up button.
	 */
	private Button upButton;

	/**
	 * The Down button.
	 */
	private Button downButton;

	/**
	 * The selection listener.
	 */
	private SelectionListener selectionListener;


	
	

	/**
	 * Creates a new list field editor
	 */
	protected ListEditor()
	{
	}
	/**
	 * Creates a list field editor.
	 *
	 * @param name the name of the preference this field editor works on
	 * @param labelText the label text of the field editor
	 * @param parent the parent of the field editor's control
	 */
	public ListEditor(
		String name,
		Composite parent,
		IPropertyChangeListener tab)
	{
		init(name, "CGI script environment variables:");
		createControl(parent);
		mTab = tab;
		
	
	}
	/**
	 * Notifies that the Add button has been pressed.
	 */
	private void addPressed()
	{
		setPresentsDefaultValue(false);
		String input = getNewInputObject();

		if (input != null)
		{

			int index = list.getSelectionIndex();
			if (index >= 0)
				index++;
			else
				index = 0;
				
			list.add(input, index);
			list.select(index);

			selectionChanged();
			listHasChanged();
			firePropertyChange();

		}
	}

	private void editPressed()
	{
		int index = list.getSelectionIndex();

		setPresentsDefaultValue(false);
		String input = list.getItem(index);
		if (index < 0)
			return;

		InputDialog dialog =
			new InputDialog(
				getShell(),
				"Edit Environment Variable",
				"Environment Variable",
				input,
				null);
		int returnCode = dialog.open();

		if (returnCode == Window.OK)
		{
			input = dialog.getValue();
		} else
			return;

		if (input != null)
		{
			list.setItem(index, input);
			selectionChanged();
			firePropertyChange();
			listHasChanged();
		}
	}
	/* (non-Javadoc)
	 * Method declared on FieldEditor.
	 */
	protected void adjustForNumColumns(int numColumns)
	{
		Control control = getLabelControl();
		((GridData) control.getLayoutData()).horizontalSpan = numColumns;
		((GridData) list.getLayoutData()).horizontalSpan = numColumns - 1;
	}
	/**
	 * Creates the Add, Remove, Up, and Down button in the given button box.
	 *
	 * @param buttonBox the box for the buttons
	 */
	private void createButtons(Composite buttonBox)
	{
		addButton = createPushButton(buttonBox, "ListEditor.add"); //$NON-NLS-1$
		removeButton = createPushButton(buttonBox, "ListEditor.remove");
		editButton = createPushButton(buttonBox, "Edit"); //$NON-NLS-1$
		upButton = createPushButton(buttonBox, "ListEditor.up"); //$NON-NLS-1$
		downButton = createPushButton(buttonBox, "ListEditor.down"); //$NON-NLS-1$
	}

	/**
	 * Helper method to create a push button.
	 *
	 * @param parent the parent control
	 * @param key the resource name used to supply the button's label text
	 */
	private Button createPushButton(Composite parent, String key)
	{
		Button button = new Button(parent, SWT.PUSH);
		button.setText(JFaceResources.getString(key));
		button.setFont(parent.getFont());
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint =
			convertVerticalDLUsToPixels(button, IDialogConstants.BUTTON_HEIGHT);
		int widthHint =
			convertHorizontalDLUsToPixels(
				button,
				IDialogConstants.BUTTON_WIDTH);
		data.widthHint =
			Math.max(
				widthHint,
				button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
		button.addSelectionListener(getSelectionListener());
		return button;
	}
	/**
	 * Creates a selection listener.
	 */
	public void createSelectionListener()
	{
		selectionListener = new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent event)
			{
				Widget widget = event.widget;
				if (widget == addButton)
				{
					addPressed();
				} else
					if (widget == removeButton)
					{
						removePressed();
					} else
						if (widget == upButton)
						{
							upPressed();
						} else
							if (widget == downButton)
							{
								downPressed();
							} else
								if (widget == editButton)
								{
									editPressed();
								} else
									if (widget == list)
									{
										selectionChanged();
									}
			}
		};
	}
	/* (non-Javadoc)
	 * Method declared on FieldEditor.
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns)
	{
		Control control = getLabelControl(parent);
		GridData gd = new GridData();
		gd.horizontalSpan = numColumns;
		control.setLayoutData(gd);

		list = getListControl(parent);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.verticalAlignment = GridData.FILL;
		gd.heightHint = 100;
		gd.horizontalSpan = numColumns - 1;
		gd.grabExcessHorizontalSpace = true;
		list.setLayoutData(gd);

		buttonBox = getButtonBoxControl(parent);

		gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		buttonBox.setLayoutData(gd);
	}
	/* (non-Javadoc)
	 * Method declared on FieldEditor.
	 */
	public void initilizeFrom(ILaunchConfiguration fLaunch)
	{
		if (list != null)
		{

			int count = 0;
			String res = null;
			list.removeAll();
			count = 0;

			ArrayList envs = null;

			try
			{
				envs =
					(ArrayList) fLaunch.getAttribute(
						PerlLaunchConfigurationConstants.ATTR_CGI_ENV,
						(ArrayList) null);
			} catch (CoreException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (envs != null)
				for (int x = 0; x < envs.size(); ++x)
					list.add((String) envs.get(x));
		}
	}

	/* (non-Javadoc)
	 * Method declared on FieldEditor.
	 */
	protected void doLoadDefault()
	{
		if (list != null)
		{
			list.removeAll();

		}

	}
	/* (non-Javadoc)
	 * Method declared on FieldEditor.
	 */
	public void doApply(ILaunchConfigurationWorkingCopy fLaunch)
	{

		if (list != null)
		{

			String[] vars = list.getItems();
			ArrayList al = new ArrayList();

			for (int i = 0; i < vars.length; i++)
			{
				al.add(vars[i]);
			}
			fLaunch.setAttribute(
				PerlLaunchConfigurationConstants.ATTR_CGI_ENV,
				al);
			//				try
			//				{
			//					//fLaunch.doSave();
			//				} catch (CoreException e)
			//				{
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}
		}
	}
	/**
	 * Notifies that the Down button has been pressed.
	 */
	private void downPressed()
	{
		swap(false);
		listHasChanged();
		firePropertyChange();
	}
	/**
	 * Returns this field editor's button box containing the Add, Remove,
	 * Up, and Down button.
	 *
	 * @param parent the parent control
	 * @return the button box
	 */
	public Composite getButtonBoxControl(Composite parent)
	{
		if (buttonBox == null)
		{
			buttonBox = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			buttonBox.setLayout(layout);
			createButtons(buttonBox);
			buttonBox.addDisposeListener(new DisposeListener()
			{
				public void widgetDisposed(DisposeEvent event)
				{
					addButton = null;
					removeButton = null;
					upButton = null;
					downButton = null;
					buttonBox = null;
				}
			});

		} else
		{
			checkParent(buttonBox, parent);
		}

		selectionChanged();
		return buttonBox;
	}
	/**
	 * Returns this field editor's list control.
	 *
	 * @param parent the parent control
	 * @return the list control
	 */
	public List getListControl(Composite parent)
	{
		if (list == null)
		{
			list =
				new List(
					parent,
					SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
			list.setFont(parent.getFont());
			list.addSelectionListener(getSelectionListener());
			list.addDisposeListener(new DisposeListener()
			{
				public void widgetDisposed(DisposeEvent event)
				{
					list = null;
				}
			});
		} else
		{
			checkParent(list, parent);
		}
		return list;
	}

	public String[] getItems()
	{
		return list.getItems();
	}

	/**
	 * Creates and returns a new item for the list.
	 * <p>
	 * Subclasses must implement this method.
	 * </p>
	 *
	 * @return a new item
	 */
	String getNewInputObject()
	{

		InputDialog dialog =
			new InputDialog(
				getShell(),
				"Add Environment Variable",
				"Environment Variable",
				null,
				null);
		int returnCode = dialog.open();

		if (returnCode == Window.OK)
		{

			return (dialog.getValue());

		}
		return (null);
	}

	void firePropertyChange()
	{
		mTab.propertyChange(new PropertyChangeEvent(this, " ", " ", " "));
	}
	/* (non-Javadoc)
	 * Method declared on FieldEditor.
	 */
	public int getNumberOfControls()
	{
		return 2;
	}
	/**
	 * Returns this field editor's selection listener.
	 * The listener is created if nessessary.
	 *
	 * @return the selection listener
	 */
	private SelectionListener getSelectionListener()
	{
		if (selectionListener == null)
			createSelectionListener();
		return selectionListener;
	}
	/**
	 * Returns this field editor's shell.
	 * <p>
	 * This method is internal to the framework; subclassers should not call
	 * this method.
	 * </p>
	 *
	 * @return the shell
	 */
	protected Shell getShell()
	{
		if (addButton == null)
			return null;
		return addButton.getShell();
	}
	/**
	 * Splits the given string into a list of strings.
	 * This method is the converse of <code>createList</code>.
	 * <p>
	 * Subclasses must implement this method.
	 * </p>
	 *
	 * @param stringList the string
	 * @return an array of <code>String</code>
	 * @see #createList
	 */
	String[] parseString(String stringList)
	{
		return null;
	}
	/**
	 * Notifies that the Remove button has been pressed.
	 */
	private void removePressed()
	{
		setPresentsDefaultValue(false);
		int index = list.getSelectionIndex();
		if (index >= 0)
		{
			list.remove(index);
			int max_index = list.getItems().length - 1;

			if (max_index >= 0)
			{

				if (index > max_index)
					index = max_index;

				list.setSelection(index);
			}

			selectionChanged();
			firePropertyChange();
		}
	}
	/**
	 * Notifies that the list selection has changed.
	 */
	private void selectionChanged()
	{

		int index = list.getSelectionIndex();
		int size = list.getItemCount();

		removeButton.setEnabled(index >= 0);
		editButton.setEnabled(index >= 0);
		upButton.setEnabled(size > 1 && index > 0);
		downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
		//is.fireValueChanged("nix","nix","nix");
	}
	/* (non-Javadoc)
	 * Method declared on FieldEditor.
	 */
	public void setFocus()
	{
		if (list != null)
		{
			list.setFocus();
		}
	}
	/**
	 * Moves the currently selected item up or down.
	 *
	 * @param up <code>true</code> if the item should move up,
	 *  and <code>false</code> if it should move down
	 */
	private void swap(boolean up)
	{
		setPresentsDefaultValue(false);
		int index = list.getSelectionIndex();
		int target = up ? index - 1 : index + 1;

		if (index >= 0)
		{
			String[] selection = list.getSelection();
			Assert.isTrue(selection.length == 1);
			list.remove(index);
			list.add(selection[0], target);
			list.setSelection(target);
		}
		selectionChanged();
	}
	/**
	 * Notifies that the Up button has been pressed.
	 */
	private void upPressed()
	{
		swap(true);
		firePropertyChange();
		listHasChanged();
	}

	/*
	 * @see FieldEditor.setEnabled(boolean,Composite).
	 */
	public void setEnabled(boolean enabled, Composite parent)
	{
		super.setEnabled(enabled, parent);
		getListControl(parent).setEnabled(enabled);
		addButton.setEnabled(enabled);
		removeButton.setEnabled(enabled);
		upButton.setEnabled(enabled);
		downButton.setEnabled(enabled);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doLoad()
	 */
	protected void doLoad()
	{
		// TODO Auto-generated method stub

	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	protected void doStore()
	{
		// TODO Auto-generated method stub

	}

	void listHasChanged()
	{

	}
	
	
	

}