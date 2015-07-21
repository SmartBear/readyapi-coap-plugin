package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;

import javax.swing.table.TableCellRenderer;

public class KnownOptions {

    public static TableCellRenderer getOptionRenderer(int optionNumber){
        return null;
    }

    public static int[] editableRequestOptions = {OptionNumberRegistry.ACCEPT, OptionNumberRegistry.CONTENT_TYPE};

    public static String[] getEditableRequestOptionsNames(){
        String[] result = new String[editableRequestOptions.length];
        for(int i = 0; i < result.length; ++i){
            result[i] = OptionNumberRegistry.toString(editableRequestOptions[i]);
        }
        return result;
    }

}
