package com.smartbear.coapsupport;

import org.eclipse.californium.core.coap.Option;

public class OptionEx extends Option {
    enum ValueFormat {Str, Binary};

    private ValueFormat valueFormat = ValueFormat.Binary;

    public OptionEx(){super();}
    public OptionEx(int number){super(number);}
    public OptionEx(int number, byte[] value){super(number,  value);}

    public ValueFormat getValueFormat(){return valueFormat;}
    public void setValueFormat(ValueFormat newValue){this.valueFormat = newValue;}
}
