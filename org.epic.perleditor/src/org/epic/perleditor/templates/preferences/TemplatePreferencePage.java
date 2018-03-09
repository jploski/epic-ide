package org.epic.perleditor.templates.preferences;

import java.io.File;
import java.util.*;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.*;
import org.epic.perleditor.templates.*;
import org.epic.perleditor.templates.ui.util.SWTUtil;
import org.osgi.service.prefs.BackingStoreException;

public class TemplatePreferencePage
  extends PreferencePage
  implements IWorkbenchPreferencePage {

  // preference store keys
  private static final String PREF_FORMAT_TEMPLATES = PerlEditorPlugin.getPluginId() + ".template.format";

  private Templates fTemplates;

  private CheckboxTableViewer fTableViewer;
  private Button fAddButton;
  private Button fEditButton;
  private Button fImportButton;
  private Button fExportButton;
  private Button fExportAllButton;
  private Button fRemoveButton;
  private Button fEnableAllButton;
  private Button fDisableAllButton;

  private SourceViewer fPatternViewer;
  //	private Button fFormatButton;

  public TemplatePreferencePage() {
    super();

    setPreferenceStore(PerlEditorPlugin.getDefault().getPreferenceStore());
    setDescription(TemplateMessages.getString("TemplatePreferencePage.message")); //$NON-NLS-1$

    fTemplates = Templates.getInstance();
  }

  /*
   * @see PreferencePage#createContents(Composite)
   */
  protected Control createContents(Composite ancestor) {
  	//TODO Check if necessary !!!!
    //PHPEditorEnvironment.connect(this);
    Composite parent = new Composite(ancestor, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    parent.setLayout(layout);

    Composite innerParent = new Composite(parent, SWT.NONE);
    GridLayout innerLayout = new GridLayout();
    innerLayout.numColumns = 2;
    innerLayout.marginHeight = 0;
    innerLayout.marginWidth = 0;
    innerParent.setLayout(innerLayout);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 2;
    innerParent.setLayoutData(gd);

    Table table =
      new Table(
        innerParent,
        SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

    GridData data = new GridData(GridData.FILL_BOTH);
    data.widthHint = convertWidthInCharsToPixels(3);
    data.heightHint = convertHeightInCharsToPixels(10);
    table.setLayoutData(data);

    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    TableLayout tableLayout = new TableLayout();
    table.setLayout(tableLayout);

    TableColumn column1 = new TableColumn(table, SWT.NONE);
    column1.setText(TemplateMessages.getString("TemplatePreferencePage.column.name")); //$NON-NLS-1$

    TableColumn column2 = new TableColumn(table, SWT.NONE);
    column2.setText(TemplateMessages.getString("TemplatePreferencePage.column.context")); //$NON-NLS-1$

    TableColumn column3 = new TableColumn(table, SWT.NONE);
    column3.setText(TemplateMessages.getString("TemplatePreferencePage.column.description")); //$NON-NLS-1$

    fTableViewer = new CheckboxTableViewer(table);
    fTableViewer.setLabelProvider(new TemplateLabelProvider());
    fTableViewer.setContentProvider(new TemplateContentProvider());

    fTableViewer.setComparator(new ViewerComparator() {
      public int compare(Viewer viewer, Object object1, Object object2) {
        if ((object1 instanceof Template) && (object2 instanceof Template)) {
          Template left = (Template) object1;
          Template right = (Template) object2;
          int result = left.getName().compareToIgnoreCase(right.getName());
          if (result != 0)
            return result;
          return left.getDescription().compareToIgnoreCase(
            right.getDescription());
        }
        return super.compare(viewer, object1, object2);
      }

      public boolean isSorterProperty(Object element, String property) {
        return true;
      }
    });

    fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent e) {
        edit();
      }
    });

    fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent e) {
        selectionChanged1();
      }
    });

    fTableViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        Template template = (Template) event.getElement();
        template.setEnabled(event.getChecked());
      }
    });

    Composite buttons = new Composite(innerParent, SWT.NONE);
    buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    buttons.setLayout(layout);

    fAddButton = new Button(buttons, SWT.PUSH);
    fAddButton.setText(TemplateMessages.getString("TemplatePreferencePage.new")); //$NON-NLS-1$
    fAddButton.setLayoutData(getButtonGridData(fAddButton));
    fAddButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        add();
      }
    });

    fEditButton = new Button(buttons, SWT.PUSH);
    fEditButton.setText(TemplateMessages.getString("TemplatePreferencePage.edit")); //$NON-NLS-1$
    fEditButton.setLayoutData(getButtonGridData(fEditButton));
    fEditButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        edit();
      }
    });

    fRemoveButton = new Button(buttons, SWT.PUSH);
    fRemoveButton.setText(TemplateMessages.getString("TemplatePreferencePage.remove")); //$NON-NLS-1$
    fRemoveButton.setLayoutData(getButtonGridData(fRemoveButton));
    fRemoveButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        remove();
      }
    });

    fImportButton = new Button(buttons, SWT.PUSH);
    fImportButton.setText(TemplateMessages.getString("TemplatePreferencePage.import")); //$NON-NLS-1$
    fImportButton.setLayoutData(getButtonGridData(fImportButton));
    fImportButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        import_();
      }
    });

    fExportButton = new Button(buttons, SWT.PUSH);
    fExportButton.setText(TemplateMessages.getString("TemplatePreferencePage.export")); //$NON-NLS-1$
    fExportButton.setLayoutData(getButtonGridData(fExportButton));
    fExportButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        export();
      }
    });

    fExportAllButton = new Button(buttons, SWT.PUSH);
    fExportAllButton.setText(TemplateMessages.getString("TemplatePreferencePage.export.all")); //$NON-NLS-1$
    fExportAllButton.setLayoutData(getButtonGridData(fExportAllButton));
    fExportAllButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        exportAll();
      }
    });

    fEnableAllButton = new Button(buttons, SWT.PUSH);
    fEnableAllButton.setText(TemplateMessages.getString("TemplatePreferencePage.enable.all")); //$NON-NLS-1$
    fEnableAllButton.setLayoutData(getButtonGridData(fEnableAllButton));
    fEnableAllButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        enableAll(true);
      }
    });

    fDisableAllButton = new Button(buttons, SWT.PUSH);
    fDisableAllButton.setText(TemplateMessages.getString("TemplatePreferencePage.disable.all")); //$NON-NLS-1$
    fDisableAllButton.setLayoutData(getButtonGridData(fDisableAllButton));
    fDisableAllButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        enableAll(false);
      }
    });

    fPatternViewer = createViewer(parent);

    //		fFormatButton= new Button(parent, SWT.CHECK);
    //		fFormatButton.setText(TemplateMessages.getString("TemplatePreferencePage.use.code.formatter")); //$NON-NLS-1$
    //        GridData gd1= new GridData();
    //        gd1.horizontalSpan= 2;
    //        fFormatButton.setLayoutData(gd1);

    fTableViewer.setInput(fTemplates);
    fTableViewer.setAllChecked(false);
    fTableViewer.setCheckedElements(getEnabledTemplates());

    updateButtons();
    configureTableResizing(
      innerParent,
      buttons,
      table,
      column1,
      column2,
      column3);

    // WorkbenchHelp.setHelp(parent, IJavaHelpContextIds.TEMPLATE_PREFERENCE_PAGE);

    return parent;
  }

  /**
  * Correctly resizes the table so no phantom columns appear
  */
  private static void configureTableResizing(
    final Composite parent,
    final Composite buttons,
    final Table table,
    final TableColumn column1,
    final TableColumn column2,
    final TableColumn column3) {
    parent.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        Rectangle area = parent.getClientArea();
        Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int width = area.width - 2 * table.getBorderWidth();
        if (preferredSize.y > area.height) {
          // Subtract the scrollbar width from the total column width
          // if a vertical scrollbar will be required
          Point vBarSize = table.getVerticalBar().getSize();
          width -= vBarSize.x;
        }
        width -= buttons.getSize().x;
        Point oldSize = table.getSize();
        if (oldSize.x > width) {
          // table is getting smaller so make the columns
          // smaller first and then resize the table to
          // match the client area width
          column1.setWidth(width / 4);
          column2.setWidth(width / 4);
          column3.setWidth(width - (column1.getWidth() + column2.getWidth()));
          table.setSize(width, area.height);
        } else {
          // table is getting bigger so make the table
          // bigger first and then make the columns wider
          // to match the client area width
          table.setSize(width, area.height);
          column1.setWidth(width / 4);
          column2.setWidth(width / 4);
          column3.setWidth(width - (column1.getWidth() + column2.getWidth()));
        }
      }
    });
  }

  private Template[] getEnabledTemplates() {
    Template[] templates = fTemplates.getTemplates();

    List<Template> list = new ArrayList<Template>(templates.length);

    for (int i = 0; i != templates.length; i++)
      if (templates[i].isEnabled())
        list.add(templates[i]);

    return list.toArray(new Template[list.size()]);
  }

  private SourceViewer createViewer(Composite parent) {
    Label label = new Label(parent, SWT.NONE);
    label.setText(TemplateMessages.getString("TemplatePreferencePage.preview")); //$NON-NLS-1$
    GridData data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    SourceViewer viewer =
      new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    //JavaTextTools tools = PerlEditorPlugin.getDefault().getJavaTextTools();
    IDocument document = new Document();
    new PerlPartitioner(PerlEditorPlugin.getDefault().getLog(), document);

    // TODO changed check
    PerlSourceViewerConfiguration sourceViewerConfiguration = new PerlSourceViewerConfiguration(PerlEditorPlugin.getDefault().getPreferenceStore(), null);
    viewer.configure(sourceViewerConfiguration);
    //viewer.configure(new PerlSourceViewerConfiguration(tools, null));
    // (tools, null));
    viewer.setEditable(false);
    viewer.setDocument(document);
    viewer.getTextWidget().setBackground(
      getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    Font font = JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT);
    viewer.getTextWidget().setFont(font);

    Control control = viewer.getControl();
    data = new GridData(GridData.FILL_BOTH);
    data.horizontalSpan = 2;
    data.heightHint = convertHeightInCharsToPixels(5);
    control.setLayoutData(data);

    return viewer;
  }

  private static GridData getButtonGridData(Button button) {
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = SWTUtil.getButtonWidthHint(button);
    data.heightHint = SWTUtil.getButtonHeigthHint(button);

    return data;
  }

  private void selectionChanged1() {
    IStructuredSelection selection =
      (IStructuredSelection) fTableViewer.getSelection();

    if (selection.size() == 1) {
      Template template = (Template) selection.getFirstElement();
      fPatternViewer.getTextWidget().setText(template.getPattern());
    } else {
      fPatternViewer.getTextWidget().setText(""); //$NON-NLS-1$
    }

    updateButtons();
  }

  private void updateButtons() {
    int selectionCount =
      ((IStructuredSelection) fTableViewer.getSelection()).size();
    int itemCount = fTableViewer.getTable().getItemCount();

    fEditButton.setEnabled(selectionCount == 1);
    fExportButton.setEnabled(selectionCount > 0);
    fRemoveButton.setEnabled(selectionCount > 0 && selectionCount <= itemCount);
    fEnableAllButton.setEnabled(itemCount > 0);
    fDisableAllButton.setEnabled(itemCount > 0);
  }

  private void add() {

    Template template = new Template();

    ContextTypeRegistry registry = ContextTypeRegistry.getInstance();
    ContextType type = registry.getContextType("perl"); //$NON-NLS-1$

    String contextTypeName;
    if (type != null)
      contextTypeName = type.getName();
    else {
      Iterator<String> iterator = registry.iterator();
      contextTypeName = iterator.next();
    }
    template.setContext(contextTypeName); //$NON-NLS-1$

    EditTemplateDialog dialog =
      new EditTemplateDialog(getShell(), template, false);
    if (dialog.open() == EditTemplateDialog.OK) {
      fTemplates.add(template);
      fTableViewer.refresh();
      fTableViewer.setChecked(template, template.isEnabled());
      fTableViewer.setSelection(new StructuredSelection(template));
    }
  }

  private void edit() {
    IStructuredSelection selection =
      (IStructuredSelection) fTableViewer.getSelection();

    Object[] objects = selection.toArray();
    if ((objects == null) || (objects.length != 1))
      return;

    Template template = (Template) selection.getFirstElement();
    edit(template);
  }

  private void edit(Template template) {
    Template newTemplate = new Template(template);
    EditTemplateDialog dialog =
      new EditTemplateDialog(getShell(), newTemplate, true);
    if (dialog.open() == EditTemplateDialog.OK) {

      if (!newTemplate.getName().equals(template.getName()) && MessageDialog.openQuestion(getShell(), TemplateMessages.getString("TemplatePreferencePage.question.create.new.title"), //$NON-NLS-1$
      TemplateMessages.getString("TemplatePreferencePage.question.create.new.message"))) //$NON-NLS-1$
        {
        template = newTemplate;
        fTemplates.add(template);
        fTableViewer.refresh();
      } else {
        template.setName(newTemplate.getName());
        template.setDescription(newTemplate.getDescription());
        template.setContext(newTemplate.getContextTypeName());
        template.setPattern(newTemplate.getPattern());
        fTableViewer.refresh(template);
      }
      fTableViewer.setChecked(template, template.isEnabled());
      fTableViewer.setSelection(new StructuredSelection(template));
    }
  }

  private void import_() {
    FileDialog dialog = new FileDialog(getShell());
    dialog.setText(TemplateMessages.getString("TemplatePreferencePage.import.title")); //$NON-NLS-1$
    dialog.setFilterExtensions(new String[] { TemplateMessages.getString("TemplatePreferencePage.import.extension")}); //$NON-NLS-1$
    String path = dialog.open();

    if (path == null)
      return;

    try {
      fTemplates.addFromFile(new File(path));

      fTableViewer.refresh();
      fTableViewer.setAllChecked(false);
      fTableViewer.setCheckedElements(getEnabledTemplates());

    } catch (CoreException e) {
      openReadErrorDialog(e);
    }
  }

  private void exportAll() {
    export(fTemplates);
  }

  private void export() {
    IStructuredSelection selection =
      (IStructuredSelection) fTableViewer.getSelection();
    Object[] templates = selection.toArray();

    TemplateSet templateSet = new TemplateSet();
    for (int i = 0; i != templates.length; i++)
      templateSet.add((Template) templates[i]);

    export(templateSet);
  }

  private void export(TemplateSet templateSet) {
    FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
    dialog.setText(TemplateMessages.getFormattedString("TemplatePreferencePage.export.title", new Integer(templateSet.getTemplates().length))); //$NON-NLS-1$
    dialog.setFilterExtensions(new String[] { TemplateMessages.getString("TemplatePreferencePage.export.extension")}); //$NON-NLS-1$
    dialog.setFileName(TemplateMessages.getString("TemplatePreferencePage.export.filename")); //$NON-NLS-1$
    String path = dialog.open();

    if (path == null)
      return;

    File file = new File(path);

    if (!file.exists() || confirmOverwrite(file)) {
      try {
        templateSet.saveToFile(file);
      } catch (CoreException e) {
        //PHPeclipsePlugin.log(e);
        e.printStackTrace();
        openWriteErrorDialog(e);
      }
    }
  }

  private boolean confirmOverwrite(File file) {
    return MessageDialog.openQuestion(getShell(), TemplateMessages.getString("TemplatePreferencePage.export.exists.title"), //$NON-NLS-1$
    TemplateMessages.getFormattedString("TemplatePreferencePage.export.exists.message", file.getAbsolutePath())); //$NON-NLS-1$
  }

  private void remove() {
    IStructuredSelection selection =
      (IStructuredSelection) fTableViewer.getSelection();

    @SuppressWarnings("unchecked")
    Iterator<Template> elements = selection.iterator();
    while (elements.hasNext()) {
      Template template = elements.next();
      fTemplates.remove(template);
    }

    fTableViewer.refresh();
  }

  private void enableAll(boolean enable) {
    Template[] templates = fTemplates.getTemplates();
    for (int i = 0; i != templates.length; i++)
      templates[i].setEnabled(enable);

    fTableViewer.setAllChecked(enable);
  }

  /*
   * @see IWorkbenchPreferencePage#init(IWorkbench)
   */
  public void init(IWorkbench workbench) {
  }

  /*
   * @see Control#setVisible(boolean)
   */
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible)
      setTitle(TemplateMessages.getString("TemplatePreferencePage.title")); //$NON-NLS-1$
  }

  /*
   * @see PreferencePage#performDefaults()
   */
  protected void performDefaults() {

    try {
      fTemplates.restoreDefaults();
    } catch (CoreException e) {
      e.printStackTrace();
      //PHPeclipsePlugin.log(e);
      openReadErrorDialog(e);
    }

    // refresh
    fTableViewer.refresh();
    fTableViewer.setAllChecked(false);
    fTableViewer.setCheckedElements(getEnabledTemplates());
  }

  /*
   * @see PreferencePage#performOk()
   */
  public boolean performOk() {
  //IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
    //	prefs.setValue(PREF_FORMAT_TEMPLATES, fFormatButton.getSelection());

    try {
      fTemplates.save();
    } catch (CoreException e) {
      e.printStackTrace();
      //PHPeclipsePlugin.log(e);
      openWriteErrorDialog(e);
    }

    // Deprecated? What is the new solution
    // PerlEditorPlugin.getDefault().savePluginPreferences();
    try
    {
        DefaultScope.INSTANCE.getNode(PerlEditorPlugin.getPluginId()).flush();
    }
    catch ( BackingStoreException e )
    {
        e.printStackTrace();
    }

    // TODO check if needed
    //PHPEditorEnvironment.disconnect(this);
    return super.performOk();
  }

  /*
   * @see PreferencePage#performCancel()
   */
  public boolean performCancel() {
    try {
      fTemplates.reset();
    } catch (CoreException e) {
      e.printStackTrace();
      //PHPeclipsePlugin.log(e);
      openReadErrorDialog(e);
    }

    // TODO Check if needed
    //PHPEditorEnvironment.disconnect(this);
    return super.performCancel();
  }

  /**
   * Initializes the default values of this page in the preference bundle.
   * Will be called on startup of the PHPeclipsePlugin
   */
  public static void initDefaults(IPreferenceStore prefs) {
    prefs.setDefault(PREF_FORMAT_TEMPLATES, true);
  }

  //	public static boolean useCodeFormatter() {
  //		IPreferenceStore prefs= PHPeclipsePlugin.getDefault().getPreferenceStore();
  //		return prefs.getBoolean(PREF_FORMAT_TEMPLATES);
  //	}

  private void openReadErrorDialog(CoreException e) {
    ErrorDialog.openError(getShell(), TemplateMessages.getString("TemplatePreferencePage.error.read.title"), //$NON-NLS-1$
    null, e.getStatus());
  }

  private void openWriteErrorDialog(CoreException e) {
    ErrorDialog.openError(getShell(), TemplateMessages.getString("TemplatePreferencePage.error.write.title"), //$NON-NLS-1$
    null, e.getStatus());
  }

}
