package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.OptionSet;
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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class OptionsEditingPane extends JPanel {
    private OptionsTableModel tableModel;
    private ImageIcon addIcon;
    private ImageIcon deleteIcon;
    private JXToolBar toolBar;
    private boolean editable;


    public OptionsEditingPane(){
        super();
        addIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/add.png");
        deleteIcon = UISupport.createImageIcon("com/eviware/soapui/resources/images/delete.png");
        buildUI();
    }

    private void buildUI(){
        setLayout(new BorderLayout(0, 0));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
    }

    private JComponent buildToolbar(){
        toolBar = UISupport.createToolbar();
        toolBar.add(new AddOptionAction());
        toolBar.add(new DeleteOptionAction());
        return toolBar;
    }

    private JComponent buildGrid(){
        tableModel = new OptionsTableModel();
        JTable grid = new JTable(tableModel);

        return new JScrollPane(grid);
    }

    public CoapOptionsDataSource getData(){return tableModel.getDataSource();}

    public void setData(CoapOptionsDataSource data, boolean editable){
        tableModel.setDataSource(data);
        setEditable(editable);
    }

    public boolean isEditable(){
        return editable;
    }
    public void setEditable(boolean newValue){
        if(newValue == isEditable()) return;
        tableModel.setEditable(newValue);
        toolBar.setVisible(newValue);
    }


    private class OptionsTableModel extends AbstractTableModel implements CoapOptionsListener{
        private CoapOptionsDataSource dataSource;
        private ArrayList<Integer> numbers = new ArrayList<Integer>();
        private ArrayList<String> values = new ArrayList<>();
        private boolean editable;
        private boolean changingDataSource = false;

        public CoapOptionsDataSource getDataSource(){return dataSource;}

        public void setDataSource(CoapOptionsDataSource dataSource){
            if(dataSource != this.dataSource) return;
            if(this.dataSource != null){
                this.dataSource.removeOptionsListener(this);
            }
            this.dataSource = dataSource;
            if(this.dataSource == null) {
                updateData(null);
            }
            else{
                updateData(this.dataSource.getOptions());
                this.dataSource.addOptionsListener(this);
            }
        }

        @Override
        public int getRowCount() {
            return values.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(columnIndex == 0){
                return OptionNumberRegistry.toString(numbers.get(rowIndex));
            }
            else if(columnIndex == 1){
                return values.get(rowIndex);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if(columnIndex != 1) throw new IllegalArgumentException();
            changingDataSource = true;
            try {
                if(Utils.areStringsEqual((String)aValue, values.get(rowIndex), false, true)) return;
                values.set(rowIndex, (String)aValue);
                int startIndex = rowIndex;
                for(; startIndex > 0; --startIndex){
                    if(!numbers.get(startIndex - 1).equals(numbers.get(rowIndex))) break;
                }
                int endIndex = rowIndex;
                for(;endIndex < values.size() - 1; ++endIndex){
                    if(!numbers.get(endIndex + 1).equals(numbers.get(rowIndex))) break;
                }
                ArrayList<String> newOptionValues = new ArrayList<>();
                for(int i = startIndex; i <= endIndex; ++i){
                    newOptionValues.add(values.get(i));
                }
                dataSource.setOption(numbers.get(rowIndex), newOptionValues);
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

        @Override
        public void onOptionChanged(int optionNumber, ChangeKind changeKind, List<String> oldValues, List<String> newValues) {
            if(changingDataSource) return;
            int startIndex = 0;
            while (startIndex < numbers.size() && optionNumber < numbers.get(startIndex)) {
                ++startIndex;
            }
            int removeCounter = 0;
            for (int i = startIndex; i < numbers.size() && optionNumber == numbers.get(startIndex); ++i) {
                ++removeCounter;
            }
            if (removeCounter != 0) {
                for (int i = 0; i < removeCounter; ++i) {
                    numbers.remove(startIndex);
                    values.remove(startIndex);
                }
                fireTableRowsDeleted(startIndex, startIndex + removeCounter - 1);
            }
            if (newValues != null && newValues.size() != 0) {
                for (int i = 0; i < newValues.size(); ++i) {
                    numbers.add(startIndex + i, optionNumber);
                    values.add(startIndex + i, newValues.get(i));
                }
                fireTableRowsInserted(startIndex, startIndex + newValues.size() - 1);
            }
        }

        @Override
        public void onWholeOptionListChanged(List<CoapOption> newList) {
            if(changingDataSource) return;
            updateData(newList);
        }

        private void updateData(List<CoapOption> newList) {
            numbers.clear();
            values.clear();
            if(newList != null){
                for(CoapOption option: newList){
                    for(String value: option.values) {
                        numbers.add(option.number);
                        values.add(value);
                    }
                }
            }
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
        }

        public void addOption(int number, String value){
            changingDataSource = true;
            try {
                int pos = 0;
                while (pos < numbers.size() && numbers.get(pos) <= number) {
                    ++pos;
                }
                numbers.add(pos, number);
                values.add(pos, value);
                fireTableRowsInserted(pos, pos);
            }
            finally {
                changingDataSource = false;
            }
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
                    tableModel.addOption(optionNumber, "");

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
