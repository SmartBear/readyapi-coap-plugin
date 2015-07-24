package com.smartbear.coapsupport;


import org.eclipse.californium.core.coap.Option;

public interface CoapOptionsDataSource {
    int getOptionCount();
    int getOptionNumber(int optionIndex);
    String getOptionValue(int optionIndex);
    void setOption(int optionIndex, String newValue);
    int addOption(int optionNumber, String value);
    void removeOption(int optionIndex);
    void moveOption(int optionIndex, int delta);
    void addOptionsListener(CoapOptionsListener listener);
    void removeOptionsListener(CoapOptionsListener listener);
}
