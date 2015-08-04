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


public class OptionsSupport {

    public enum OptionType{Uint, String, Opaque, Empty, Unknown};

    public static int[] editableRequestOptions = {OptionNumberRegistry.ACCEPT, OptionNumberRegistry.CONTENT_FORMAT, OptionNumberRegistry.ETAG, OptionNumberRegistry.IF_MATCH, OptionNumberRegistry.IF_NONE_MATCH, OptionNumberRegistry.SIZE1, OptionNumberRegistry.SIZE2};

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
                if(OptionsSupport.getOptionType(optionNumber) == OptionType.Uint){
                    return UIntOptionRenderer.class;
                }
                else {
                    return null;
                }
        }
    }

    public static Class<? extends TableCellEditor> getOptionEditor(int optionNumber){
        switch (optionNumber){
            case OptionNumberRegistry.ACCEPT:case OptionNumberRegistry.CONTENT_FORMAT:
                return MediaTypeOptionEditor.class;
            case OptionNumberRegistry.SIZE2:
                return Size2Editor.class;
            default:
                switch (OptionsSupport.getOptionType(optionNumber)){
                    case Empty:
                        return EmptyOptionEditor.class;
                    case String:
                        return StringOptionEditor.class;
                    case Uint:
                        return UIntOptionEditor.class;
                    case Opaque: case Unknown:
                        return OpaqueOptionEditor.class;
                }
        }
        return null;
    }

    public static OptionType getOptionType(int optionNumber){
        if(optionNumber == OptionNumberRegistry.IF_NONE_MATCH) return OptionType.Empty;
        switch (OptionNumberRegistry.getFormatByNr(optionNumber)){
            case INTEGER: return OptionType.Uint;
            case OPAQUE: return OptionType.Opaque;
            case STRING: return OptionType.String;
            case UNKNOWN: return OptionType.Unknown;
        }
        return OptionType.Unknown;
    }


    public static Long decodeIntOptionValue(String rawValue, int maxByteCount){
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

    public static boolean isOptionRepeatable(int optionNumber) {
        if(optionNumber == OptionNumberRegistry.SIZE1 || optionNumber == OptionNumberRegistry.SIZE2) return false;
        return !OptionNumberRegistry.isSingleValue(optionNumber);
    }

    //returns null on error
    public static String encodeIntOptionValue(String text, int maxByteCount){
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
    public static String encodeIntOptionValue(long value, int maxByteCount){
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
            //setText("0x" + Long.toHexString(intValue));
            setText(Long.toString(intValue));
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

    public static class MediaTypeComboBox extends JComboBox<String> {
        private String value = null;
        private boolean applyingValue = false;

        private static String[] getItems() {
            String[] mediaTypeItems = new String[MediaTypeRegistry.getAllMediaTypes().size() - 1];
            int i = 0;
            for (int mediaType : MediaTypeRegistry.getAllMediaTypes()) {
                if (mediaType == MediaTypeRegistry.UNDEFINED) continue;
                mediaTypeItems[i++] = MediaTypeRegistry.toString(mediaType);
            }
            return mediaTypeItems;
        }

        public MediaTypeComboBox() {
            super(getItems());
            setEditable(true);
            applyValue(null);

        }

//        public String getEditedValue(){
//            if(getSelectedIndex() >= 0){
//                return encodeIntOptionValue(MediaTypeRegistry.parse((String) getSelectedItem()), 2);
//            }
//            return encodeIntOptionValue((String)getEditor().getItem(), 2);
//        }

        public static final String VALUE_BEAN_PROP = "value";

        public String getValue() {
            return value;
        }

        public void setValue(String newValue) {
            String oldValue = getValue();
            if (Utils.areStringsEqual(oldValue, newValue, false, false)) return;
            value = newValue;
            applyValue(newValue);
            firePropertyChange(VALUE_BEAN_PROP, oldValue, newValue);
        }

        private void applyValue(String newValue) {
            applyingValue = true;
            try {
                Long intValue = decodeIntOptionValue(newValue, 2);
                if (intValue == null) {
                    if (newValue != null && newValue.startsWith("0x")){
                        setSelectedItem(newValue.substring(2));
                    }
                    else {
                        setSelectedItem(newValue);
                    }
                }
                else {
                    int mediaType = intValue.intValue();
                    if (MediaTypeRegistry.getAllMediaTypes().contains(mediaType)) {
                        setSelectedItem(MediaTypeRegistry.toString(mediaType));
                    }
                    else {
                        setSelectedItem("0x" + Integer.toString(mediaType, 16));
                    }
                }
            }
            finally {
                applyingValue = false;
            }
        }

        public String getEditedValue() {
            if (getSelectedIndex() >= 0) {
                return encodeIntOptionValue(MediaTypeRegistry.parse((String) getSelectedItem()), 2);
            }
            String curText = (String) getEditor().getItem();
            String goodValue = encodeIntOptionValue(curText, 2);
            if (goodValue != null) return goodValue;
            if (curText != null && (curText.startsWith("0x") || curText.startsWith("0X"))) return "0x" + curText;
            else return curText;
        }

        private boolean isEditedValueValid() {
            return getSelectedIndex() >= 0 || encodeIntOptionValue((String) getEditor().getItem(), 2) != null;
        }

        @Override
        protected void processFocusEvent(FocusEvent e) {
            super.processFocusEvent(e);
            if (e.getID() == FocusEvent.FOCUS_LOST) {
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
            if(applyingValue) return;
            final String prevValue = value;
            boolean resetValue = !isEditedValueValid() && !Utils.areStringsEqual(prevValue, getEditedValue(), true, false);
            if (!resetValue) updateValueProperty();
            super.fireActionEvent();
            if (resetValue) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        applyValue(prevValue);
                    }
                });
            }
        }

        private void updateValueProperty() {
            String oldValue = getValue();
            String curValue = getEditedValue();
            if (!Utils.areStringsEqual(oldValue, curValue, false, false)) {
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
            comboBox.setValue(null);
            comboBox.setValue(this.initialValue);
            return comboBox;
        }

        @Override
        public Object getCellEditorValue(){
            String result = comboBox.getValue();
            if(result == null) return initialValue; else return result;
        }
    }

    public static class UIntOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private JTextField component = new JTextField(20);
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
                //component.setText("0x" + Long.toHexString(longValue));
                component.setText(Long.toString(longValue));
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

    public static class EmptyOptionEditor extends AbstractCellEditor implements TableCellEditor{
        private JTextField editor;
        public EmptyOptionEditor(ModelItem modelItem){
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if(editor == null){
                editor = new JTextField(20);
                editor.setEnabled(false);
                editor.setText("<Must have no value>");
                editor.setToolTipText("This option must have empty value.");
            }
            return editor;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    public static class Size2Editor extends AbstractCellEditor implements TableCellEditor{
        private JTextField editor;
        public Size2Editor(ModelItem modelItem){
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if(editor == null){
                editor = new JTextField(20);
                editor.setEnabled(false);
                editor.setText("0 <Only zero is allowed>");
                editor.setToolTipText("This option must have zero value for a request.");
            }
            return editor;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

}
