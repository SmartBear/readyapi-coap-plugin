package com.smartbear.coapsupport;

import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.Response;

import java.util.ArrayList;
import java.util.List;

public class ResponseOptionsDataSource implements CoapOptionsDataSource {
    private List<Option> options = new ArrayList<>();

    public ResponseOptionsDataSource(Response message) {
        options = message.getOptions().asSortedList();
    }

    @Override
    public int getOptionCount() {
        return options.size();
    }

    @Override
    public int getOptionNumber(int optionIndex) {
        return options.get(optionIndex).getNumber();
    }

    @Override
    public String getOptionValue(int optionIndex) {
        Option option = options.get(optionIndex);
        switch (OptionsSupport.getOptionType(option.getNumber())) {
            case String:
                return option.getStringValue();
            case Opaque:
            case Unknown:
            case Uint:
            case Empty:
                if (option.getValue() == null || option.getValue().length == 0) {
                    return "";
                }
                return "0x" + Utils.bytesToHexString(option.getValue());
        }
        throw new IllegalArgumentException();
    }

    @Override
    public void setOption(int optionIndex, String optionValue) {

    }

    @Override
    public int addOption(int optionNumber, String optionValue) {
        return 0;
    }

    @Override
    public void removeOption(int optionIndex) {

    }

    @Override
    public void moveOption(int optionIndex, int delta) {

    }

    @Override
    public void addOptionsListener(CoapOptionsListener listener) {

    }

    @Override
    public void removeOptionsListener(CoapOptionsListener listener) {

    }
}
