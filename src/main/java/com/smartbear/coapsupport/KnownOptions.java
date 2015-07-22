package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import com.eviware.soapui.support.StringUtils;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.lang.reflect.Array;

import static ch.ethz.inf.vs.californium.coap.MediaTypeRegistry.*;

public class KnownOptions {

    public static int[] editableRequestOptions = {OptionNumberRegistry.ACCEPT, OptionNumberRegistry.CONTENT_TYPE};

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
            int number;
            try{
                number = Integer.parseInt(rawValue, 16);
            }
            catch (NumberFormatException ignored){
                setText(rawValue);
                return;
            }
        }
    }

    public static class MediaTypeOptionEditor extends AbstractCellEditor implements TableCellEditor{
        private JComboBox<String> comboBox;
        private String initialValue;

        public MediaTypeOptionEditor(){
            String[] mediaTypeItems = new String[knownMediaTypes.length];
            for(int i = 0; i < mediaTypeItems.length; ++i){
                mediaTypeItems[i] = MediaTypeRegistry.toString(knownMediaTypes[i]);
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
                    comboBox.setSelectedItem(rawValue);
                    for (int i = 0; i < knownMediaTypes.length; ++i) {
                        if (knownMediaTypes[i] == mediaType) {
                            comboBox.setSelectedIndex(i);
                            break;
                        }
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            }
            return comboBox;
        }

        @Override
        public Object getCellEditorValue(){
            String value = (String) comboBox.getSelectedItem();
            if(StringUtils.isNullOrEmpty(value)) return initialValue;
            value = value.trim();
            if(comboBox.getSelectedIndex() >= 0) return "0x" + Integer.toString(knownMediaTypes[comboBox.getSelectedIndex()]);
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

    public static final int[] knownMediaTypes = {
        TEXT_PLAIN,
        TEXT_XML,
        TEXT_CSV,
        TEXT_HTML,
        IMAGE_GIF,
        IMAGE_JPEG,
        IMAGE_PNG,
        IMAGE_TIFF,
        AUDIO_RAW,
        VIDEO_RAW,
        APPLICATION_LINK_FORMAT,
        APPLICATION_XML,
        APPLICATION_OCTET_STREAM,
        APPLICATION_RDF_XML,
        APPLICATION_SOAP_XML,
        APPLICATION_ATOM_XML,
        APPLICATION_XMPP_XML,
        APPLICATION_EXI,
        APPLICATION_FASTINFOSET,
        APPLICATION_SOAP_FASTINFOSET,
        APPLICATION_JSON,
        APPLICATION_X_OBIX_BINARY
    };

}
