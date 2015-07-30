package com.smartbear.coapsupport;

import com.eviware.soapui.model.ModelItem;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
import com.eviware.x.form.XFormFieldValidator;
import com.eviware.x.form.XFormOptionsField;
import com.eviware.x.form.support.ADialogBuilder;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashMap;

public class OptionsEditingPane extends JPanel {
    private final ImageIcon moveDownIcon;
    private final ImageIcon moveUpIcon;
    private OptionsTableModel tableModel;
    private ImageIcon addIcon;
    private ImageIcon deleteIcon;
    private JXToolBar toolBar;
    private boolean editable = false;
    private DeleteOptionAction deleteOptionAction;
    private AddOptionAction addOptionAction;
    private OptionsTable grid;
    private MoveOptionUpAction moveOptionUpAction;
    private MoveOptionDownAction moveOptionDownAction;
    private ModelItem owningModelItem;

    private final static int NAME_COLUMN = 0;
    private final static int VALUE_COLUMN = 1;

    public OptionsEditingPane(){this(null);}

    public OptionsEditingPane(ModelItem owningModelItem){
        super();
        this.owningModelItem = owningModelItem;
        addIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/add.png");
        deleteIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/delete.png");
        moveDownIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/down_arrow.gif");
        moveUpIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/up_arrow.gif");
        buildUI();
    }

    private void buildUI(){
        setLayout(new BorderLayout(0, 0));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
    }

    private JComponent buildToolbar(){
        toolBar = UISupport.createToolbar();
        addOptionAction = new AddOptionAction();
        toolBar.add(UISupport.createToolbarButton(addOptionAction, addOptionAction.isEnabled()));
        deleteOptionAction = new DeleteOptionAction();
        toolBar.add(UISupport.createToolbarButton(deleteOptionAction, deleteOptionAction.isEnabled()));
        moveOptionDownAction = new MoveOptionDownAction();
        toolBar.add(UISupport.createToolbarButton(moveOptionDownAction, moveOptionDownAction.isEnabled()));
        moveOptionUpAction = new MoveOptionUpAction();
        toolBar.add(UISupport.createToolbarButton(moveOptionUpAction, moveOptionUpAction.isEnabled()));
        toolBar.setVisible(isEditable());
        return toolBar;
    }

    private boolean isActionOnSelectedOptionAllowed(){
        return editable && getData() != null && grid.getSelectedRows() != null && grid.getSelectedRows().length != 0;
    }

    private JComponent buildGrid(){
        tableModel = new OptionsTableModel();
        grid = new OptionsTable(tableModel);
        grid.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        grid.setRowHeight(25);

        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(deleteOptionAction == null) return;
                deleteOptionAction.updateState();;
                moveOptionDownAction.updateState();
                moveOptionUpAction.updateState();
            }
        });

        return new JScrollPane(grid);
    }

    public CoapOptionsDataSource getData(){return tableModel.getDataSource();}

    public void setData(CoapOptionsDataSource data){
        tableModel.setDataSource(data);
        updateActions();
    }

    public boolean isEditable(){
        return editable;
    }
    public void setEditable(boolean newValue){
        if(newValue == isEditable()) return;
        editable = newValue;
        tableModel.setEditable(newValue);
        toolBar.setVisible(newValue);
        updateActions();
    }

    private void updateActions(){
        addOptionAction.updateState();
        deleteOptionAction.updateState();
        moveOptionDownAction.updateState();
        moveOptionUpAction.updateState();
    }

    private class OptionsTable extends JTable{
        private HashMap<Class<? extends TableCellRenderer>, TableCellRenderer> renderers = new HashMap<>();
        private HashMap<Class<? extends TableCellEditor>, TableCellEditor> editors = new HashMap<>();

        public OptionsTable(OptionsTableModel tableModel){super(tableModel);}

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if(column == VALUE_COLUMN) {
                Class<? extends TableCellRenderer> clazz = KnownOptions.getOptionRenderer(tableModel.getDataSource().getOptionNumber(row));
                if(clazz == null) return super.getCellRenderer(row, column);
                if(!renderers.containsKey(clazz)){
                    TableCellRenderer renderer = null;
                    try {
                        renderer = clazz.getConstructor().newInstance();
                    } catch (Throwable e) {
                        SoapUI.logError(e);
                    }
                    renderers.put(clazz, renderer);
                }
                return renderers.get(clazz);
            }
            else{
                return super.getCellRenderer(row, column);
            }
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            if(column == VALUE_COLUMN) {
                Class<? extends TableCellEditor> clazz = KnownOptions.getOptionEditor(tableModel.getDataSource().getOptionNumber(row));
                if(clazz == null) return super.getCellEditor(row, column);
                if(!editors.containsKey(clazz)){
                    TableCellEditor editor = null;
                    try {
                        editor = clazz.getConstructor(ModelItem.class).newInstance(owningModelItem);
                    } catch (Throwable e) {
                        SoapUI.logError(e);
                    }
                    editors.put(clazz, editor);
                }
                return editors.get(clazz);
            }
            else{
                return super.getCellEditor(row, column);
            }
        }

    }

    private class OptionsTableModel extends AbstractTableModel implements CoapOptionsListener{
        private CoapOptionsDataSource dataSource;
        private boolean editable;
        private boolean changingDataSource = false;

        public CoapOptionsDataSource getDataSource(){return dataSource;}

        public void setDataSource(CoapOptionsDataSource dataSource){
            if(dataSource == this.dataSource) return;
            if(this.dataSource != null){
                this.dataSource.removeOptionsListener(this);
            }
            this.dataSource = dataSource;
            if(this.dataSource != null) {
                this.dataSource.addOptionsListener(this);
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return dataSource == null ? 0 : dataSource.getOptionCount();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            int number = dataSource.getOptionNumber(rowIndex);
            if(columnIndex == NAME_COLUMN){
                return OptionNumberRegistry.toString(number);
            }
            else if(columnIndex == VALUE_COLUMN){
                return dataSource.getOptionValue(rowIndex);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if(columnIndex != VALUE_COLUMN) throw new IllegalArgumentException();
            changingDataSource = true;
            try{
                dataSource.setOption(rowIndex, (String)aValue);
            }
            finally {
                changingDataSource = false;
            }
        }

        @Override
        public String getColumnName(int column) {
            if(column == NAME_COLUMN) return "Option";
            if(column == VALUE_COLUMN) return "Value";
            throw new IllegalArgumentException();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == VALUE_COLUMN && editable;
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
        }

        @Override
        public void onOptionChanged(int optionIndex, int oldOptionNumber, int newOptionNumber, String oldOptionValue, String newOptionValue) {
            if(changingDataSource) return;
            fireTableRowsUpdated(optionIndex, optionIndex);
        }

        @Override
        public void onOptionAdded(int optionIndex, int optionNumber, String optionValue) {
            if(changingDataSource) return;
            fireTableRowsInserted(optionIndex, optionIndex);
        }

        @Override
        public void onOptionRemoved(int optionIndex, int oldOptionNumber, String oldOptionValue) {
            if(changingDataSource) return;
            fireTableRowsDeleted(optionIndex, optionIndex);
        }

        @Override
        public void onWholeOptionListChanged() {
            if(changingDataSource) return;
            fireTableDataChanged();
        }

        public boolean hasOption(int optionNumber) {
            if(dataSource == null) return false;
            int rowCount = dataSource.getOptionCount();
            for(int i = 0; i < rowCount; i++){
                if(dataSource.getOptionNumber(i) == optionNumber) return true;
            }
            return false;
        }
    }

    private class AddOptionAction extends AbstractAction implements Action {
        private Component valueEditingComponent;
        private TableCellEditor editor;

        public AddOptionAction(){
            super();
            putValue(Action.SHORT_DESCRIPTION, "Add Option");
            putValue(Action.SMALL_ICON, addIcon);
            updateState();
        }

        private int getOptionNumber(XFormOptionsField combo){
            if(combo.getSelectedIndexes().length == 0){
                String s = combo.getValue();
                if(StringUtils.isNullOrEmpty(s)) return -1;
                s = s.trim();
                int radix = 10;
                if(s.startsWith("0x") || s.startsWith("0X")) {
                    radix = 16;
                    s = s.substring(2);
                }
                try {
                    return Integer.parseInt(s, radix);
                } catch (NumberFormatException ignored ) {
                    return -1;
                }
            }
            else{
                return KnownOptions.editableRequestOptions[combo.getSelectedIndexes()[0]];
            }

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            valueEditingComponent = null;
            XFormDialog dialog = ADialogBuilder.buildDialog(ChooseOptionNumberForm.class);
            try{
                final XFormField valueField = dialog.getFormField(ChooseOptionNumberForm.VALUE);
                final JTextField dummyValueComponent = new JTextField(20);
                valueField.setProperty("component", dummyValueComponent);
                dummyValueComponent.setEnabled(false);
                valueField.setProperty("preferredSize", dummyValueComponent.getPreferredSize());

                final XFormOptionsField combo = (XFormOptionsField) dialog.getFormField(ChooseOptionNumberForm.OPTION);
                final String[] optionNames = KnownOptions.getEditableRequestOptionsNames();
                String[] comboItems = new String[KnownOptions.editableRequestOptions.length + 1];
                System.arraycopy(optionNames, 0, comboItems, 1, optionNames.length);
                combo.setOptions(comboItems);
                combo.addFormFieldValidator(new XFormFieldValidator() {
                    @Override
                    public ValidationMessage[] validateField(XFormField formField) {
                        int optionNumber = getOptionNumber(combo);
                        if(optionNumber <= 0) return new ValidationMessage[]{new ValidationMessage("Please type a valid positive decimal or hexadecimal number (starting from 0x) or choose an option from the list.", combo)};
                        if(OptionNumberRegistry.isSingleValue(optionNumber) && tableModel.hasOption(optionNumber)) return new ValidationMessage[]{new ValidationMessage("Unable to add this option because it is already specified and it does not allow multiple values", combo)};
                        if(editor != null){
                            String value = (String)editor.getCellEditorValue();
                            if(value == null) return new ValidationMessage[]{new ValidationMessage("Please input a valid option value", valueField)};
                        }
                        return new ValidationMessage[0];
                    }
                });
                combo.addFormFieldListener(new XFormFieldListener() {
                    @Override
                    public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                        int optionNumber = getOptionNumber(combo);
                        if(optionNumber <= 0){
                            dummyValueComponent.setEnabled(false);
                            dummyValueComponent.setText("");
                            valueEditingComponent = dummyValueComponent;
                            editor = null;
                        }
                        else{
                            Class<? extends TableCellEditor> clazz = KnownOptions.getOptionEditor(optionNumber);
                            if(clazz == null){
                                dummyValueComponent.setEnabled(true);
                                dummyValueComponent.setText("");
                                valueEditingComponent = dummyValueComponent;
                                editor = null;
                            }
                            else {
                                try {
                                    editor = clazz.getConstructor(ModelItem.class).newInstance(owningModelItem);
                                }
                                catch(Throwable e){
                                    return;
                                }
                                valueEditingComponent = editor.getTableCellEditorComponent(null, null, true, -1, -1);
                            }
                        }
                        valueField.setProperty("component", valueEditingComponent);
                        valueField.setProperty("preferredSize", valueEditingComponent.getPreferredSize());
                    }
                });
                if(dialog.show()){
                    int optionNumber = getOptionNumber(combo);
                    String value;
                    if(editor != null) {
                        value = (String) editor.getCellEditorValue();
                    }
                    else{
                        value = dummyValueComponent.getText();
                    }
                    int index = getData().addOption(optionNumber, value);
                    grid.getSelectionModel().clearSelection();
                    grid.getSelectionModel().addSelectionInterval(index, index);

                }
            }
            finally {
                dialog.release();
            }
        }

        public void updateState() {
            setEnabled(editable && getData() != null);
        }
    }

    private class DeleteOptionAction extends AbstractAction{
        public DeleteOptionAction(){
            putValue(Action.SHORT_DESCRIPTION, "Remove Option");
            putValue(Action.SMALL_ICON, deleteIcon);
            updateState();
        }

        private void updateState() {
            setEnabled(isActionOnSelectedOptionAllowed());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = grid.getSelectedRows();
            Arrays.sort(rows);
            for(int i = rows.length - 1; i >= 0; --i){
                getData().removeOption(rows[i]);
            }
            if(rows.length == 1){
                int newOptionCount = getData().getOptionCount();
                grid.getSelectionModel().clearSelection();
                if(rows[0] < newOptionCount) {
//                    grid.getSelectionModel().setLeadSelectionIndex(rows[0]);
                    grid.getSelectionModel().addSelectionInterval(rows[0], rows[0]);

                }
                else if(newOptionCount != 0){
//                    grid.getSelectionModel().setLeadSelectionIndex(newOptionCount - 1);
                    grid.getSelectionModel().addSelectionInterval(newOptionCount - 1, newOptionCount - 1);
                }
            }
        }
    }

    private class MoveOptionUpAction extends AbstractAction{
        public MoveOptionUpAction(){
            putValue(Action.SHORT_DESCRIPTION, "Move Option Up");
            putValue(Action.SMALL_ICON, moveUpIcon);
            updateState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int optionNo = grid.getSelectionModel().getLeadSelectionIndex();
            getData().moveOption(optionNo, -1);
//            grid.getSelectionModel().setLeadSelectionIndex(optionNo - 1);
            grid.getSelectionModel().clearSelection();
            grid.getSelectionModel().addSelectionInterval(optionNo - 1, optionNo - 1);
        }

        public void updateState() {
            setEnabled(isActionOnSelectedOptionAllowed() && grid.getSelectionModel().getLeadSelectionIndex() > 0);
        }
    }

    private class MoveOptionDownAction extends AbstractAction{
        public MoveOptionDownAction(){
            putValue(Action.SHORT_DESCRIPTION, "Move Option Down");
            putValue(Action.SMALL_ICON, moveDownIcon);
            updateState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int optionNo = grid.getSelectionModel().getLeadSelectionIndex();
            getData().moveOption(optionNo, +1);
            //grid.getSelectionModel().setLeadSelectionIndex(optionNo + 1);
            grid.getSelectionModel().clearSelection();
            grid.getSelectionModel().addSelectionInterval(optionNo + 1, optionNo + 1);
        }

        public void updateState() {
            setEnabled(isActionOnSelectedOptionAllowed() && grid.getSelectionModel().getLeadSelectionIndex() >= 0 && grid.getSelectionModel().getLeadSelectionIndex() != grid.getRowCount() - 1);
        }
    }



}
