package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
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


    public OptionsEditingPane(){
        super();
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
        toolBar.add(UISupport.createActionButton(addOptionAction, editable && getData() != null));
        deleteOptionAction = new DeleteOptionAction();
        toolBar.add(UISupport.createActionButton(deleteOptionAction, shouldDeleteOptionActionBeEnabled()));
        moveOptionUpAction = new MoveOptionUpAction();
        toolBar.add(UISupport.createActionButton(moveOptionUpAction, shouldDeleteOptionActionBeEnabled()));
        moveOptionDownAction = new MoveOptionDownAction();
        toolBar.add(UISupport.createActionButton(moveOptionDownAction, shouldDeleteOptionActionBeEnabled()));
        return toolBar;
    }

    private boolean shouldDeleteOptionActionBeEnabled(){
        return editable && getData() != null && grid.getSelectedRows() != null && grid.getSelectedRows().length != 0;
    }

    private JComponent buildGrid(){
        tableModel = new OptionsTableModel();
        grid = new OptionsTable(tableModel);
        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                deleteOptionAction.setEnabled(shouldDeleteOptionActionBeEnabled());
            }
        });
        grid.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        grid.setRowHeight(25);

        return new JScrollPane(grid);
    }

    public CoapOptionsDataSource getData(){return tableModel.getDataSource();}

    public void setData(CoapOptionsDataSource data){
        tableModel.setDataSource(data);
        addOptionAction.setEnabled(data != null && editable);
        deleteOptionAction.setEnabled(shouldDeleteOptionActionBeEnabled());
    }

    public boolean isEditable(){
        return editable;
    }
    public void setEditable(boolean newValue){
        if(newValue == isEditable()) return;
        editable = newValue;
        tableModel.setEditable(newValue);
        toolBar.setVisible(newValue);
        addOptionAction.setEnabled(getData() != null && editable);
        deleteOptionAction.setEnabled(shouldDeleteOptionActionBeEnabled());
    }

    private class OptionsTable extends JTable{
        private HashMap<Class<? extends TableCellRenderer>, TableCellRenderer> renderers = new HashMap<>();
        private HashMap<Class<? extends TableCellEditor>, TableCellEditor> editors = new HashMap<>();

        public OptionsTable(OptionsTableModel tableModel){super(tableModel);}

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if(column == 1) {
                Class<? extends TableCellRenderer> clazz = KnownOptions.getOptionRenderer(tableModel.getDataSource().getOption(row).number);
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
            if(column == 1) {
                Class<? extends TableCellEditor> clazz = KnownOptions.getOptionEditor(tableModel.getDataSource().getOption(row).number);
                if(clazz == null) return super.getCellEditor(row, column);
                if(!editors.containsKey(clazz)){
                    TableCellEditor editor = null;
                    try {
                        editor = clazz.getConstructor().newInstance();
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
            if(columnIndex == 0){
                return OptionNumberRegistry.toString(dataSource.getOption(rowIndex).number);
            }
            else if(columnIndex == 1){
                return dataSource.getOption(rowIndex).value;
            }
            throw new IllegalArgumentException();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if(columnIndex != 1) throw new IllegalArgumentException();
            changingDataSource = true;
            try {
                dataSource.setOption(rowIndex, (String)aValue);
            }
            finally {
                changingDataSource = false;
            }
        }

        @Override
        public String getColumnName(int column) {
            if(column == 0) return "Option";
            if(column == 1) return "Value";
            throw new IllegalArgumentException();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1 && editable;
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
    }

    private class AddOptionAction extends AbstractAction implements Action {
        public AddOptionAction(){
            super();
            putValue(Action.SHORT_DESCRIPTION, "Add Option");
            putValue(Action.SMALL_ICON, addIcon);
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
            XFormDialog dialog = ADialogBuilder.buildDialog(ChooseOptionNumberForm.class);
            try{
                final XFormOptionsField combo = (XFormOptionsField) dialog.getFormField(ChooseOptionNumberForm.OPTION);
                String[] optionNames = KnownOptions.getEditableRequestOptionsNames();
                String[] comboItems = new String[KnownOptions.editableRequestOptions.length + 1];
                System.arraycopy(optionNames, 0, comboItems, 1, optionNames.length);
                combo.setOptions(comboItems);
                combo.addFormFieldValidator(new XFormFieldValidator() {
                    private ValidationMessage[] getError(){
                        return new ValidationMessage[]{new ValidationMessage("Please type a valid positive decimal or hexadecimal number (starting from 0x) or choose an option from the list.", combo)};
                    }

                    @Override
                    public ValidationMessage[] validateField(XFormField formField) {
                        if(getOptionNumber(combo) <= 0) return getError();
                        return new ValidationMessage[0];
                    }
                });
                if(dialog.show()){
                    int optionNumber = getOptionNumber(combo);
                    getData().addOption(optionNumber, "");
                }
            }
            finally {
                dialog.release();
            }
        }
    }

    private class DeleteOptionAction extends AbstractAction{
        public DeleteOptionAction(){
            putValue(Action.SHORT_DESCRIPTION, "Remove Option");
            putValue(Action.SMALL_ICON, deleteIcon);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = grid.getSelectedRows();
            Arrays.sort(rows);
            for(int i = rows.length - 1; i >= 0; --i){
                getData().removeOption(rows[i]);
            }
        }
    }

    private class MoveOptionUpAction extends AbstractAction{
        public MoveOptionUpAction(){
            putValue(Action.SHORT_DESCRIPTION, "Move Option Up");
            putValue(Action.SMALL_ICON, moveUpIcon);
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

    private class MoveOptionDownAction extends AbstractAction{
        public MoveOptionDownAction(){
            putValue(Action.SHORT_DESCRIPTION, "Move Option Down");
            putValue(Action.SMALL_ICON, moveDownIcon);
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

//    private OptionSet coapOptions;
//
//    public OptionSet getCoapOptions() {
//        return coapOptions;
//    }
//
//    public void setCoapOptions(OptionSet coapOptions) {
//        this.coapOptions = coapOptions;
//    }


}
