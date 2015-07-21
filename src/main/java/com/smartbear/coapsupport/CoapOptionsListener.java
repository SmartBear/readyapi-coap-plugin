package com.smartbear.coapsupport;

import java.util.List;

public interface CoapOptionsListener {
    enum ChangeKind{Added, ValueChanged, Removed};
    void onOptionChanged(int optionNumber, ChangeKind changeKind, List<String> oldValues, List<String> newValues);
    void onWholeOptionListChanged(List<CoapOption> newList);
}
