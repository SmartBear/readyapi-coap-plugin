package com.smartbear.coapsupport;

import java.util.List;

public interface CoapOptionsDataSource {
    List<CoapOption> getOptions();
    void setOption(int optionNumber, List<String> optionValues);
    void addOption(int optionNumber, List<String> optionValues);
    void removeOption(int optionNumber);
    void addOptionsListener(CoapOptionsListener listener);
    void removeOptionsListener(CoapOptionsListener listener);
}
