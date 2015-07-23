package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;

import java.util.ArrayList;
import java.util.List;

public class ResponseOptionsDataSource implements CoapOptionsDataSource {
    private List<Option> options = new ArrayList<>();

    public ResponseOptionsDataSource(Response message){

        options = message.getOptions().asSortedList();
    }

    @Override
    public int getOptionCount() {
        return options.size();
    }

    @Override
    public CoapOption getOption(int optionIndex) {
        Option raw = options.get(optionIndex);
        byte[] value = raw.getValue();
        if(value == null || value.length == 0) return new CoapOption(raw.getNumber());
        return new CoapOption(raw.getNumber(), "0x" + Utils.bytesToHexString(value));
    }

    @Override
    public void setOption(int optionIndex, String optionValue) {

    }

    @Override
    public int addOption(int optionNumber, String optionValue) {
        return -1;
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
