package com.smartbear.coapsupport;

import javax.validation.constraints.NotNull;

public interface CoapOptionsDataSource {
    int getOptionCount();
    int getOptionNumber(int optionIndex);
    @NotNull
    String getOptionValue(int optionIndex);
    void setOption(int optionIndex, String newValue);
    int addOption(int optionNumber, String value);
    void removeOption(int optionIndex);
    void moveOption(int optionIndex, int delta);
    void addOptionsListener(CoapOptionsListener listener);
    void removeOptionsListener(CoapOptionsListener listener);
}
