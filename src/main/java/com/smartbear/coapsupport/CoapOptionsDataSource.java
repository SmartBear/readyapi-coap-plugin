package com.smartbear.coapsupport;


import org.eclipse.californium.core.coap.Option;

public interface CoapOptionsDataSource {
    int getOptionCount();
    Option getOption(int optionIndex);
    void setOption(int optionIndex, Option option);
    int addOption(int optionNumber, Option option);
    void removeOption(int optionIndex);
    void moveOption(int optionIndex, int delta);
    void addOptionsListener(CoapOptionsListener listener);
    void removeOptionsListener(CoapOptionsListener listener);
}
