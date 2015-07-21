package com.smartbear.coapsupport;

import java.util.ArrayList;

public class CoapOption {

    public int number;
    public ArrayList<String> values = new ArrayList<>();

    public CoapOption(int number){
        this.number = number;
    }
}
