package com.smartbear.coapsupport;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;


public class KnownOptions {

    public static int[] editableRequestOptions = {OptionNumberRegistry.ACCEPT, OptionNumberRegistry.CONTENT_FORMAT, OptionNumberRegistry.ETAG};

    public static String[] getEditableRequestOptionsNames(){
        String[] result = new String[editableRequestOptions.length];
        for(int i = 0; i < result.length; ++i){
            result[i] = OptionNumberRegistry.toString(editableRequestOptions[i]);
        }
        return result;
    }


    public static Class<? extends TableCellRenderer> getOptionRenderer(int optionNumber){
        switch (optionNumber){
            case OptionNumberRegistry.ACCEPT: case OptionNumberRegistry.CONTENT_FORMAT:
                return MediaTypeOptionRenderer.class;
            default:
                if(OptionNumberRegistry.getFormatByNr(optionNumber) == OptionNumberRegistry.optionFormats.INTEGER){
                    return UIntOptionRenderer.class;
                }
                else {
                    return null;
                }
        }
    }

    public static Class<? extends TableCellEditor> getOptionEditor(int optionNumber){
        if(optionNumber == OptionNumberRegistry.ACCEPT || optionNumber == OptionNumberRegistry.CONTENT_FORMAT){
            return MediaTypeOptionEditor.class;
        }
        else{
            switch (OptionNumberRegistry.getFormatByNr(optionNumber)){
                case STRING:
                    return StringOptionEditor.class;
                case INTEGER:
                    return UIntOptionEditor.class;
                case OPAQUE: case UNKNOWN:
                    return OpaqueOptionEditor.class;
            }
        }
        return null;
    }


    private static Long decodeIntOptionValue(String rawValue, int maxByteCount){
        if(rawValue == null) return null;
        if(rawValue.length() == 0) return 0L;
        if(rawValue.startsWith("0x0x")) return null;
        if(!rawValue.startsWith("0x") || rawValue.length() > maxByteCount * 2 + 2) return null;
        byte[] binValue;
        try {
            binValue = Utils.hexStringToBytes(rawValue.substring(2));
        } catch (IllegalArgumentException e) {
            return null;
        }
        Option tmpOption = new Option();
        tmpOption.setValue(binValue);
        long result = tmpOption.getLongValue();
        if(result < 0) return null;
        if(result >= (1L << (maxByteCount * 8))) return null;
        return result;
    }

    //returns null on error
    private static String encodeIntOptionValue(String text, int maxByteCount){
        if(text == null || text.length() == 0) return null;
        text = text.trim();
        Long actualValue = null;
        if(text.startsWith("0x") || text.startsWith("0X")){
            try {
                actualValue = Long.parseLong(text.substring(2), 16);
            }
            catch (NumberFormatException ignored) {
            }
        }
        else{
            try{
            actualValue = Long.parseLong(text);
            }
            catch (NumberFormatException ignored) {
            }
        }
        if(actualValue == null) return null;
        return encodeIntOptionValue(actualValue, maxByteCount);
    }

    //returns null on error
    private static String encodeIntOptionValue(long value, int maxByteCount){
        if(value < 0) return null;
        if(value >= (1L << (8 * maxByteCount))) return null;
        Option tmpOption = new Option();
        tmpOption.setLongValue(value);
        byte[] binValue = tmpOption.getValue();
        String result = Utils.bytesToHexString(binValue);
        if(result == null || result.length() == 0) return "";
        return "0x" + result;
    }

    public static class UIntOptionRenderer extends DefaultTableCellRenderer{
        @Override
        protected void setValue(Object value) {
            String rawValue = (String) value;
            Long intValue = decodeIntOptionValue(rawValue, 2);
            if(intValue == null){
                if(rawValue != null && rawValue.startsWith("0x")) setText(rawValue.substring(2)); else setText(rawValue);
                return;
            }
            setText("0x" + Long.toHexString(intValue));
        }

    }

    public static class MediaTypeOptionRenderer extends DefaultTableCellRenderer{
        @Override
        protected void setValue(Object value) {
            String rawValue = (String) value;
            Long intValue = decodeIntOptionValue(rawValue, 2);
            if(intValue == null){
                if(rawValue != null && rawValue.startsWith("0x")) setText(rawValue.substring(2)); else setText(rawValue);
                return;
            }
            if (MediaTypeRegistry.getAllMediaTypes().contains(intValue.intValue())) {
                setText(MediaTypeRegistry.toString(intValue.intValue()));
            } else {
                setText("0x" + Long.toHexString(intValue));
            }
        }
    }

    public static class MediaTypeComboBox extends JComboBox<String>{
        private String value = null;

        private static String[] getItems(){
            String[] mediaTypeItems = new String[MediaTypeRegistry.getAllMediaTypes().size() - 1];
            int i = 0;
            for(int mediaType: MediaTypeRegistry.getAllMediaTypes()){
                if(mediaType == MediaTypeRegistry.UNDEFINED) continue;
                mediaTypeItems[i++] = MediaTypeRegistry.toString(mediaType);
            }
            return mediaTypeItems;
        }

        public MediaTypeComboBox(){
            super(getItems());
            setEditable(true);
            setSelectedItem(null);

        }

        public String getEditedValue(){
            if(getSelectedIndex() >= 0){
                return encodeIntOptionValue(MediaTypeRegistry.parse((String) getSelectedItem()), 2);
            }
            return encodeIntOptionValue((String)getEditor().getItem(), 2);
        }

        public static final String VALUE_BEAN_PROP = "optionValue";
        public String getOptionValue(){return value;}
        public void setOptionValue(String newValue){
            String oldValue = getOptionValue();
            if(Utils.areStringsEqual(oldValue, newValue, false, false)) return;
            Long intValue = decodeIntOptionValue(newValue, 2);
            if(intValue == null){
                if(newValue != null && newValue.startsWith("0x")) setSelectedItem(newValue.substring(2)); else setSelectedItem(newValue);
            }
            else{
                int mediaType = intValue.intValue();
                if(MediaTypeRegistry.getAllMediaTypes().contains(mediaType)){
                    setSelectedItem(MediaTypeRegistry.toString(mediaType));
                }
                else{
                    setSelectedItem("0x" + Integer.toString(mediaType, 16));
                }
            }
//            value = getEditedValue();
//            if(!Utils.areStringsEqual(oldValue, value, false, false)) firePropertyChange(VALUE_BEAN_PROP, oldValue, value);
            updateValueProperty();
        }

        @Override
        protected void processFocusEvent(FocusEvent e) {
            super.processFocusEvent(e);
            if(e.getID() == FocusEvent.FOCUS_LOST){
                //updateValueProperty();
            }
        }

        @Override
        public void processKeyEvent(KeyEvent e) {
            super.processKeyEvent(e);
            //if(e.getKeyCode() == KeyEvent.VK_ENTER) updateValueProperty();
        }

        @Override
        protected void fireActionEvent() {
            final String prevValue = value;
            String curValue = getEditedValue();
            String curText = (String)getEditor().getItem();
            if(curValue == null && curText != null && curText.length() != 0){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setOptionValue(prevValue);
                    }
                });
            }
            else {
                updateValueProperty();
            }
            super.fireActionEvent();
        }

        private void updateValueProperty() {
            String oldValue = getOptionValue();
            String curValue = getEditedValue();
            if(!Utils.areStringsEqual(oldValue, curValue, false, false)){
                value = curValue;
                firePropertyChange(VALUE_BEAN_PROP, oldValue, value);
            }
        }
    }


    public static class MediaTypeOptionEditor extends AbstractCellEditor implements TableCellEditor{
        private MediaTypeComboBox comboBox;
        private String initialValue;

        public MediaTypeOptionEditor(ModelItem modelItem){
            comboBox = new MediaTypeComboBox();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object initialValue, boolean isSelected, int row, int column) {
            this.initialValue = (String)initialValue;
            comboBox.setOptionValue(this.initialValue);
            return comboBox;
        }

        @Override
        public Object getCellEditorValue(){
            String result = comboBox.getOptionValue();
            if(result == null) return initialValue; else return result;
        }
    }

    public static class UIntOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private JTextField component = new JTextField();
        private String initialValue;

        public UIntOptionEditor(ModelItem modelItem){}

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            initialValue = (String)value;
            Long longValue = decodeIntOptionValue(initialValue, 4);
            if(longValue == null){
                if(initialValue != null && initialValue.startsWith("0x")) component.setText(initialValue.substring(2)); else component.setText(initialValue);
            }
            else {
                component.setText("0x" + Long.toHexString(longValue));
            }
            return component;
        }

        @Override
        public Object getCellEditorValue() {
            String value = encodeIntOptionValue(component.getText(), 4);
            if(value == null) return initialValue; else return value;
        }
    }

    public static class OpaqueOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private final static String HINT_TEXT = "Use hex (0x..) or string as an option value";
        private JTextField editor = null;
        private ModelItem modelItem;

        public OpaqueOptionEditor(ModelItem modelItem){
            this.modelItem = modelItem;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if(editor == null){
                editor = new JTextField(20);
                editor.setToolTipText(HINT_TEXT);
                if(modelItem != null) PropertyExpansionPopupListener.enable(editor, modelItem);
            }
            editor.setText((String)value);
            return editor;
        }

        @Override
        public Object getCellEditorValue() {
            return editor.getText();
        }
    }

    private static class StringOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private JTextField editor;

        public StringOptionEditor(ModelItem modelItem){
            editor = new JTextField(20);
            PropertyExpansionPopupListener.enable(editor, modelItem);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editor.setText((String)value);
            return editor;
        }

        @Override
        public Object getCellEditorValue() {
            return editor.getText();
        }
    }
}
