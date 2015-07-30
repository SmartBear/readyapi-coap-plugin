package com.smartbear.coapsupport;

import com.eviware.soapui.model.ModelItem;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import com.eviware.soapui.SoapUI;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.HashMap;

public class OptionsTable extends JTable {

    private ModelItem owningModelItem;
    private HashMap<Class<? extends TableCellRenderer>, TableCellRenderer> renderers = new HashMap<>();
    private HashMap<Class<? extends TableCellEditor>, TableCellEditor> editors = new HashMap<>();

    private final static int NAME_COLUMN = 0;
    private final static int VALUE_COLUMN = 1;

    public OptionsTable(){
        this(null);
    }

    public OptionsTable(ModelItem owningModelItem){
        super();
        this.owningModelItem = owningModelItem;
        setModel(new OptionsTableModel());
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        setRowHeight(25);
    }

    public OptionsTableModel getOptionsTableModel(){
        return (OptionsTableModel) super.getModel();
    }

    public boolean isEditable(){
        return getOptionsTableModel().isEditable();
    }

    public void setEditable(boolean newValue){
        getOptionsTableModel().setEditable(newValue);
    }

    public CoapOptionsDataSource getData(){return getOptionsTableModel().getDataSource();}
    public void setData(CoapOptionsDataSource newData){getOptionsTableModel().setDataSource(newData);}

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if(column == VALUE_COLUMN) {
            Class<? extends TableCellRenderer> clazz = OptionsSupport.getOptionRenderer(getOptionsTableModel().getDataSource().getOptionNumber(row));
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
            Class<? extends TableCellEditor> clazz = OptionsSupport.getOptionEditor(getOptionsTableModel().getDataSource().getOptionNumber(row));
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


    protected class OptionsTableModel extends AbstractTableModel implements CoapOptionsListener{
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

        public boolean isEditable(){return editable;}

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




}
