package com.smartbear.coapsupport;

import java.util.List;

public interface CoapOptionsDataSource {
    class CoapOption{
        public int number;
        public String value;
        CoapOption(int number){this.number = number;}
        CoapOption(int number, String value){this.number = number; this.value = value;}
    }
    //List<CoapOption> getOptions();
    int getOptionCount();
    CoapOption getOption(int optionIndex);
    void setOption(int optionIndex, String optionValue);
    int addOption(int optionNumber, String optionValue);
    void removeOption(int optionIndex);
    void moveOption(int optionIndex, int delta);
    void addOptionsListener(CoapOptionsListener listener);
    void removeOptionsListener(CoapOptionsListener listener);
}
