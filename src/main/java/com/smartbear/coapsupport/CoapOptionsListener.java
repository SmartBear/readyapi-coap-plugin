package com.smartbear.coapsupport;

import java.util.List;

public interface CoapOptionsListener {
    void onOptionChanged(int optionIndex, int oldOptionNumber, int newOptionNumber, byte[] oldOptionValue, byte[] newOptionValue);
    void onOptionAdded(int optionIndex, int optionNumber, byte[] optionValue);
    void onOptionRemoved(int optionIndex, int oldOptionNumber, byte[] oldOptionValue);
    void onWholeOptionListChanged();
}
