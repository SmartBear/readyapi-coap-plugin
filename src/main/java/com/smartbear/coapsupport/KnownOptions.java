package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;

import com.eviware.soapui.support.StringUtils;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;


public class KnownOptions {

    public static int[] editableRequestOptions = {OptionNumberRegistry.ACCEPT, OptionNumberRegistry.CONTENT_TYPE, OptionNumberRegistry.ETAG};

    public static String[] getEditableRequestOptionsNames(){
        String[] result = new String[editableRequestOptions.length];
        for(int i = 0; i < result.length; ++i){
            result[i] = OptionNumberRegistry.toString(editableRequestOptions[i]);
        }
        return result;
    }


    public static Class<? extends TableCellRenderer> getOptionRenderer(int optionNumber){
        switch (optionNumber){
            case OptionNumberRegistry.ACCEPT: case OptionNumberRegistry.CONTENT_TYPE:
                return MediaTypeOptionRenderer.class;
            default:
                return null;
        }
    }

    public static Class<? extends TableCellEditor> getOptionEditor(int optionNumber){
        if(optionNumber == OptionNumberRegistry.ACCEPT || optionNumber == OptionNumberRegistry.CONTENT_TYPE){
            return MediaTypeOptionEditor.class;
        }
        return null;
    }



    public static class MediaTypeOptionRenderer extends DefaultTableCellRenderer{
        @Override
        protected void setValue(Object value) {
            String rawValue = (String) value;
            if(rawValue != null && rawValue.startsWith("0x")) {
                int number;
                try {
                    number = Integer.parseInt(rawValue.substring(2), 16);
                } catch (NumberFormatException ignored) {
                    setText(rawValue);
                    return;
                }
                if(MediaTypeRegistry.getAllMediaTypes().contains(number)) setText(MediaTypeRegistry.toString(number)); else setText(rawValue);
            }
            else{
                setText(rawValue);
            }
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
                comboBox.setSelectedItem(null);
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

}
