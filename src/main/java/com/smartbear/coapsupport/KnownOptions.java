package com.smartbear.coapsupport;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansion;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionUtils;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
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



    public static class MediaTypeOptionRenderer extends DefaultTableCellRenderer{
        @Override
        protected void setValue(Object value) {
            Number number = (Number) value;
            if(number == null){
                setText("");
                return;
            }
            if(MediaTypeRegistry.getAllMediaTypes().contains(number.intValue())){
                setText(MediaTypeRegistry.toString(number.intValue()));
            }
            else {
                setText("0x" + Integer.toHexString(number.intValue()));
            }
        }
    }

    public static class MediaTypeOptionEditor extends AbstractCellEditor implements TableCellEditor{
        private JComboBox<String> comboBox;
        private Number initialValue;

        public MediaTypeOptionEditor(ModelItem modelItem){
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
            initialValue = (Number)valueObject;
            if (initialValue == null) {
                comboBox.setSelectedItem("");
            }
            else {
                int mediaType = initialValue.intValue();
                //if (mediaType < 0 || mediaType >= 0x10000) throw new IllegalArgumentException();
                if(MediaTypeRegistry.getAllMediaTypes().contains(mediaType)){
                    comboBox.setSelectedItem(MediaTypeRegistry.toString(mediaType));
                }
                else{
                    comboBox.setSelectedItem("0x" + Integer.toString(mediaType, 16));
                }
            }
            return comboBox;
        }

        @Override
        public Object getCellEditorValue(){
            String value = (String) comboBox.getEditor().getItem();
            if(StringUtils.isNullOrEmpty(value)) return initialValue;
            value = value.trim();
            if(comboBox.getSelectedIndex() >= 0){
                return MediaTypeRegistry.parse((String) comboBox.getSelectedItem());
            }
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
            return mediaType;
        }
    }

    public static class UIntOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private JSpinner spinEdit = new JSpinner();

        public UIntOptionEditor(ModelItem modelItem){}

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

        public OpaqueOptionEditor(ModelItem modelItem){
            editor = new JTextField(10);
            PropertyExpansionPopupListener.enable(editor, modelItem);
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

    private static class StringOptionEditor extends AbstractCellEditor implements TableCellEditor {
        private JTextField editor;

        public StringOptionEditor(ModelItem modelItem){
            editor = new JTextField(10);
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
