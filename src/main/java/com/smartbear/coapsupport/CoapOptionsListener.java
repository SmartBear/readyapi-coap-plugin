package com.smartbear.coapsupport;

public interface CoapOptionsListener {
    void onOptionChanged(int optionIndex, int oldOptionNumber, int newOptionNumber, String oldOptionValue, String newOptionValue);
    void onOptionAdded(int optionIndex, int optionNumber, String optionValue);
    void onOptionRemoved(int optionIndex, int oldOptionNumber, String oldOptionValue);
    void onWholeOptionListChanged();
}
