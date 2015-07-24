package com.smartbear.coapsupport;

import com.smartbear.ready.GhostText;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionNumberRegistry;

import com.eviware.soapui.support.StringUtils;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import static org.eclipse.californium.core.coap.OptionNumberRegistry.optionFormats.*;


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
                return null;
        }
    }

    public static Class<? extends TableCellEditor> getOptionEditor(int optionNumber){
        if(optionNumber == OptionNumberRegistry.ACCEPT || optionNumber == OptionNumberRegistry.CONTENT_FORMAT){
            return MediaTypeOptionEditor.class;
        }
        else if(OptionNumberRegistry.getFormatByNr(optionNumber) == INTEGER){
            return UIntOptionEditor.class;
        }
        else if(OptionNumberRegistry.getFormatByNr(optionNumber) == OPAQUE || OptionNumberRegistry.getFormatByNr(optionNumber) == UNKNOWN){
            return OpaqueOptionEditor.class;
        }
        return null;
    }



    public static class MediaTypeOptionRenderer extends DefaultTableCellRenderer{
        @Override
        protected void setValue(Object value) {
            String rawValue = (String) value;
            int number;
            if(rawValue == null || rawValue.length() == 0){
                number = 0;
            }
            else if(rawValue.startsWith("0x")) {
                try {
                    number = Integer.parseInt(rawValue.substring(2), 16);
                } catch (NumberFormatException ignored) {
                    setText(rawValue);
                    return;
                }
            }
            else{
                setText(rawValue);
                return;
            }
            if(MediaTypeRegistry.getAllMediaTypes().contains(number)) setText(MediaTypeRegistry.toString(number)); else setText(rawValue);
        }
    }

    public static class MediaTypeOptionEditor extends AbstractCellEditor implements TableCellEditor{
        private JComboBox<String> comboBox;
        private String initialValue;

        public MediaTypeOptionEditor(){
            String[] mediaTypeItems = new String[MediaTypeRegistry.getAllMediaTypes().size() - 1];
            int i = 0;
            for(int mediaType: MediaTypeRegistry.getAllMediaTypes()){
                if(mediaType == MediaTypeRegistry.UNDEFINED) continue;
                mediaTypeItems[i++] = MediaTypeRegistry.toString(mediaType);
            }
            comboBox = new JComboBox<String>(mediaTypeItems);
            comboBox.setEditable(true);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object valueObject, boolean isSelected, int row, int column) {
            String rawValue = (String) valueObject;
            initialValue = rawValue;
            if (rawValue == null || rawValue.length() == 0) {
                comboBox.setSelectedItem(MediaTypeRegistry.toString(0));
            } else {
                rawValue = rawValue.trim();
                if (rawValue.startsWith("0x")) {
                    int mediaType;
                    try {
                        mediaType = Integer.parseInt(rawValue.substring(2), 16);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException();
                    }
                    if (mediaType < 0 || mediaType >= 0x10000) throw new IllegalArgumentException();
                    if(MediaTypeRegistry.getAllMediaTypes().contains(mediaType)){
                        comboBox.setSelectedItem(MediaTypeRegistry.toString(mediaType));
                    }
                    else{
                        comboBox.setSelectedItem("0x" + Integer.toString(mediaType, 16));
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            }
            return comboBox;
        }

        @Override
        public Object getCellEditorValue(){
            String value = (String) comboBox.getEditor().getItem();
            if(StringUtils.isNullOrEmpty(value)) return initialValue;
            value = value.trim();
            if(comboBox.getSelectedIndex() >= 0) return "0x" + Integer.toString(MediaTypeRegistry.parse((String) comboBox.getSelectedItem()), 16);
            int radix = 10;
            if(value.startsWith("0x")){
                radix = 16;
                value = value.substring(2);
            }
            int mediaType;
            try {
                mediaType = Integer.parseInt(value,  radix);
            } catch (NumberFormatException e) {
                return initialValue;
            }
            if(mediaType < 0 || mediaType > 0xffff) return initialValue;
            return "0x" + Integer.toString(mediaType, 16);
        }
    }

    public static class UIntOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private JSpinner spinEdit = new JSpinner();

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            spinEdit.setValue(value);
            return spinEdit;
        }

        @Override
        public Object getCellEditorValue() {
            return spinEdit.getValue();
        }
    }

    public static class OpaqueOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private GhostText<JTextField> ghost;
        private JTextField editor;

        public OpaqueOptionEditor(){
            editor = new JTextField(10);
            ghost = new GhostText<JTextField>(editor, "Use hex (0x..) or string");
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
