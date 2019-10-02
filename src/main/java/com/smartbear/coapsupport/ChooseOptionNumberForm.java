package com.smartbear.coapsupport;

import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;

@AForm(name = "Add an option", description = "Please choose an option and input its value")
public interface ChooseOptionNumberForm {
    @AField(name = "Option", description = "Choose an option name or type its number as a decimal or hexadecimal (starting from 0x) number", type = AField.AFieldType.COMBOBOX)
    String OPTION = "Option";

    @AField(name = "Value", description = "Value of the option", type = AField.AFieldType.COMPONENT)
    String VALUE = "Value";
}
