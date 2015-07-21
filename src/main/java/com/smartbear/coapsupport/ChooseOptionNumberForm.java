package com.smartbear.coapsupport;

import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;

@AForm(name = "Choose an option", description = "Choose an option name or type its number as a decimal")
public interface ChooseOptionNumberForm {
    @AField(name = "Option", description = "Choose an option name or type its number as a decimal", type = AField.AFieldType.COMBOBOX)
    public final static String OPTION = "Option";
}
