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
    public Option getOption(int optionIndex) {
        return options.get(optionIndex);
    }

    @Override
    public void setOption(int optionIndex, byte[] optionValue) {

    }

    @Override
    public int addOption(int optionNumber, byte[] optionValue) {
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
