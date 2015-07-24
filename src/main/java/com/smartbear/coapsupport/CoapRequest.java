package com.smartbear.coapsupport;


import org.eclipse.californium.core.coap.Option;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import org.apache.xmlbeans.XmlObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class CoapRequest extends HttpTestRequest implements CoapOptionsDataSource {
    private final static String OUR_SCHEMA = null;// "http://smartbear.com/rapi/config";
    private static final String OPTION_SECTION = "Option";
    private static final String OPTION_NUMBER_ATTR = "Number";
    private static final String OPTION_VALUE_ATTR = "Value";

    private CoapRequestTestStep testStep;
    private ImageIcon validRequestIcon;
    private ImageIcon failedRequestIcon;
    private ImageIcon unknownRequestIcon;
    private ImageIcon disabledRequestIcon;
    private boolean forLoadTest;
    private ArrayList<CoapOptionsListener> optionsListeners;

    public CoapRequest(CoapRequestTestStep testStep, HttpRequestConfig config, boolean forLoadTest) {
        super(config, testStep, forLoadTest);
        this.testStep = testStep;
        this.forLoadTest = forLoadTest;
    }

    @Override
    protected void initIcons() {
        validRequestIcon = UISupport.createImageIcon("com/smartbear/coapsupport/valid_coap_request_step.png");
        failedRequestIcon = UISupport.createImageIcon("com/smartbear/coapsupport/invalid_coap_request_step.png");
        unknownRequestIcon = UISupport.createImageIcon("com/smartbear/coapsupport/coap_request_step.png");
        disabledRequestIcon = UISupport.createImageIcon("com/smartbear/coapsupport/disabled_coap_request_step.png");

        setIconAnimator(new RequestIconAnimator<CoapRequest>(this, "com/smartbear/coapsupport/coap_request.png", "com/smartbear/coapsupport/coap_request.png", 5));
    }

    @Override
    public ImageIcon getIcon() {
        if (forLoadTest || getIconAnimator() == null) {
            return null;
        }

        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null
                && (testMonitor.hasRunningLoadTest(getTestStep().getTestCase()) || testMonitor
                .hasRunningSecurityTest(getTestStep().getTestCase()))) {
            return disabledRequestIcon;
        }

        ImageIcon icon = getIconAnimator().getIcon();
        if (icon == getIconAnimator().getBaseIcon()) {
            AssertionStatus status = getAssertionStatus();
            if (status == AssertionStatus.VALID) {
                return validRequestIcon;
            } else if (status == AssertionStatus.FAILED) {
                return failedRequestIcon;
            } else if (status == AssertionStatus.UNKNOWN) {
                return unknownRequestIcon;
            }
        }

        return icon;
    }

    public boolean getConfirmable(){
//        XmlObject xmlObject = getConfig().selectAttribute(OUR_SCHEMA, "confirmable");
//        if(xmlObject == null) return true;
//        String attrValue = xmlObject.xmlText();
//        return "true".equalsIgnoreCase(attrValue);
        Element element = (Element) getConfig().getDomNode();
        String rawValue = element.getAttributeNS(OUR_SCHEMA, "confirmable");
        if(rawValue == null) return true;
        return Boolean.valueOf(rawValue);

    }

    public void setConfirmable(boolean newValue){
        boolean oldConfirmable = getConfirmable();
        if(newValue  !=  oldConfirmable) {
            Element element = (Element) getConfig().getDomNode();
            element.setAttributeNS(OUR_SCHEMA, "confirmable", Boolean.toString(newValue));
            notifyPropertyChanged("confirmable", oldConfirmable, newValue);
        }
    }

    public ModelItem getParent() {
        return testStep;
    }

    public void updateConfig(HttpRequestConfig request){
        super.updateConfig(request);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onWholeOptionListChanged();
            }
        }
    }

    private static String getElementText(XmlObject obj){
        return getElementText((Element) obj.getDomNode());
    }

    private static String getElementText(Element element){
        String result = "";
        Node child = element.getFirstChild();
        while (child != null){
           if(child.getNodeType() == Node.TEXT_NODE){
                result += child.getNodeValue();
            }
            child = child.getNextSibling();
        };
        return result;
    }

    @Override
    public int getOptionCount() {
        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        return optionSections.length;
    }

    private OptionEx readOption(XmlObject optionSection) {
        return readOption((Element)optionSection.getDomNode());
    }

    private OptionEx readOption(Element optionSection){
        String optionNumberStr = optionSection.getAttribute(OPTION_NUMBER_ATTR);
        String optionValueStr = optionSection.getAttribute(OPTION_VALUE_ATTR);
        int optionNumber;
        try {
            optionNumber = Integer.parseInt(optionNumberStr);
        }
        catch (NumberFormatException e) {
            SoapUI.logError(e, String.format("Incorrect data (\"%s\") in the %s test step", optionNumberStr, getName()));
            return new OptionEx();
        }
        byte[] value = null;
        if(StringUtils.hasContent(optionValueStr)){
            try {
                value = Utils.hexStringToBytes(optionValueStr);
            }
            catch (IllegalArgumentException e){
                SoapUI.logError(e, String.format("Incorrect data (\"%s\") in the %s test step", optionValueStr, getName()));
                return new OptionEx();
            }
        }
        return new OptionEx(optionNumber, value);
    }

    private Element getOptionSection(int optionIndex){
        String path = String.format("$this/%s[%d]", OPTION_SECTION, optionIndex + 1);
        XmlObject[] pathResult = getConfig().selectPath(path);
        if(pathResult == null || pathResult.length == 0) return null;
        return (Element)pathResult[0].getDomNode();
    }

    @Override
    public Option getOption(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        return readOption(optionSection);
    }

    @Override
    public void setOption(int optionIndex, byte[] optionValue) {
        Element optionSection = getOptionSection(optionIndex);

        OptionEx oldOption = readOption(optionSection);
        if(Utils.areArraysEqual(oldOption.getValue(), optionValue, true)) return;

        if(optionValue == null || optionValue.length == 0){
            optionSection.removeAttribute(OPTION_VALUE_ATTR);
        }
        else{
            optionSection.setAttribute(OPTION_VALUE_ATTR, Utils.bytesToHexString(optionValue));
        }
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionChanged(optionIndex, oldOption.getNumber(), oldOption.getNumber(), oldOption.getValue(), optionValue);
            }
        }
    }

    @Override
    public int addOption(int optionNumber, byte[] optionValue) {
        Element newOptionSection = getConfig().getDomNode().getOwnerDocument().createElement(OPTION_SECTION);
        newOptionSection.setAttribute(OPTION_NUMBER_ATTR, Integer.toString(optionNumber));
        if(optionValue != null && optionValue.length != 0) {
            String valueStr = Utils.bytesToHexString(optionValue);
            newOptionSection.setAttribute(OPTION_VALUE_ATTR, valueStr);
        }

        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        int pos;
        Element optionSection = null;
        for(pos = 0; pos < optionSections.length; ++pos){
            optionSection = (Element) optionSections[pos].getDomNode();
            String optionNumberStr = optionSection.getAttribute(OPTION_NUMBER_ATTR);
            int curOptionNumber;
            try {
                curOptionNumber = Integer.parseInt(optionNumberStr);
            } catch (NumberFormatException e) {
                SoapUI.logError(e, String.format("Incorrect data (\"%s\") in the %s test step", optionNumberStr, getName()));
                return -1;
            }
            if(optionNumber > curOptionNumber) break;
        }
        if(pos == optionSections.length) getConfig().getDomNode().appendChild(newOptionSection); else getConfig().getDomNode().insertBefore(newOptionSection, optionSection);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionAdded(pos, optionNumber, optionValue);
            }
        }
        return pos;
    }

    @Override
    public void removeOption(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        Option oldOption = readOption(optionSection);
        getConfig().getDomNode().removeChild(optionSection);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionRemoved(optionIndex, oldOption.getNumber(), oldOption.getValue());
            }
        }
    }


    @Override
    public void moveOption(int optionIndex, int delta) {
        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        Option oldOption = readOption(optionSections[optionIndex]);
        Node movedSection = optionSections[optionIndex].getDomNode();
        getConfig().getDomNode().removeChild(movedSection);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionRemoved(optionIndex, oldOption.getNumber(), oldOption.getValue());
            }
        }
        int newIndex = optionIndex + delta;
        if(newIndex == optionSections.length - 1) {
            getConfig().getDomNode().appendChild(movedSection);
        }
        else {
            getConfig().getDomNode().insertBefore(movedSection, optionSections[newIndex + 1].getDomNode());
        }
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionAdded(optionIndex, oldOption.getNumber(), oldOption.getValue());
            }
        }
    }


    public void addOptionsListener(CoapOptionsListener listener){
        if(optionsListeners == null) optionsListeners = new ArrayList<>();
        optionsListeners.add(listener);
    }

    public void removeOptionsListener(CoapOptionsListener listener){
        if(optionsListeners == null) return;
        optionsListeners.remove(listener);
    }
}
