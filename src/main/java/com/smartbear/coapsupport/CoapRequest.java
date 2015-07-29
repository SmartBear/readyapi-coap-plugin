package com.smartbear.coapsupport;


import com.eviware.fife.rsta.ac.java.Util;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import org.apache.xmlbeans.XmlObject;

import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.swing.ImageIcon;
import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class CoapRequest extends HttpTestRequest implements CoapOptionsDataSource {
    private static final String OPTION_SECTION = "Option";
    private static final String OPTION_NUMBER_ATTR = "Number";
    private static final String OPTION_VALUE_ATTR = "Value";
    public static final String CONTENT_FORMAT_OPTION_BEAN_PROP = "contentFormatOption";

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
        String rawValue = element.getAttributeNS(null, "confirmable");
        if(rawValue == null) return true;
        return Boolean.valueOf(rawValue);

    }

    public void setConfirmable(boolean newValue){
        boolean oldConfirmable = getConfirmable();
        if(newValue  !=  oldConfirmable) {
            Element element = (Element) getConfig().getDomNode();
            element.setAttributeNS(null, "confirmable", Boolean.toString(newValue));
            notifyPropertyChanged("confirmable", oldConfirmable, newValue);
        }
    }

    public ModelItem getParent() {
        return testStep;
    }

    public void updateConfig(HttpRequestConfig request){
        String oldContentFormat = getContentFormatOption();
        super.updateConfig(request);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onWholeOptionListChanged();
            }
        }
        String newContentFormat = getContentFormatOption();
        if(Utils.areStringsEqual(oldContentFormat, newContentFormat, true, false)){
            notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, oldContentFormat, newContentFormat);
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

    private int readOptionNumber(XmlObject optionSection) {
        return readOptionNumber((Element) optionSection.getDomNode());
    }

    private int readOptionNumber(Element optionSection) {
        String optionNumberStr = optionSection.getAttribute(OPTION_NUMBER_ATTR);
        int optionNumber;
        try {
            optionNumber = Integer.parseInt(optionNumberStr);
        } catch (NumberFormatException e) {
            SoapUI.logError(e, String.format("Incorrect data (\"%s\") in the %s test step", optionNumberStr, getName()));
            return -1;
        }
        return optionNumber;
    }

    private String readOptionValue(Element optionSection){
        return optionSection.getAttribute(OPTION_VALUE_ATTR);
    }

    private String readOptionValue(XmlObject optionSection) {
        return readOptionValue((Element) optionSection.getDomNode());
    }

//        byte[] value = null;
//        if(StringUtils.hasContent(optionValueStr)){
//            try {
//                value = Utils.hexStringToBytes(optionValueStr);
//            }
//            catch (IllegalArgumentException e){
//                SoapUI.logError(e, String.format("Incorrect data (\"%s\") in the %s test step", optionValueStr, getName()));
//                return new OptionEx();
//            }
//        }
//        return new OptionEx(optionNumber, value);
//    }

    private Element getOptionSection(int optionIndex){
        String path = String.format("$this/%s[%d]", OPTION_SECTION, optionIndex + 1);
        XmlObject[] pathResult = getConfig().selectPath(path);
        if(pathResult == null || pathResult.length == 0) return null;
        return (Element)pathResult[0].getDomNode();
    }

    @Override
    public int getOptionNumber(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        return readOptionNumber(optionSection);
    }

    @NotNull
    @Override
    public String getOptionValue(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        return readOptionValue(optionSection);
    }

    @Override
    public void setOption(int optionIndex, String optionValue) {
        Element optionSection = getOptionSection(optionIndex);

        String oldOptionValue = readOptionValue(optionSection);
        if(Utils.areStringsEqual(oldOptionValue, optionValue, false, true)) return;

        int optionNumber = readOptionNumber(optionSection);
        if(optionValue == null || optionValue.length() == 0){
            optionSection.removeAttribute(OPTION_VALUE_ATTR);
        }
        else{
            optionSection.setAttribute(OPTION_VALUE_ATTR, optionValue);
        }
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionChanged(optionIndex, optionNumber, optionNumber, oldOptionValue, optionValue);
            }
        }
        if(optionNumber == OptionNumberRegistry.CONTENT_FORMAT){
            notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, oldOptionValue, optionValue == null ? "" : optionValue);
        }
    }

    @Override
    public int addOption(int optionNumber, String optionValue) {
        Element newOptionSection = getConfig().getDomNode().getOwnerDocument().createElement(OPTION_SECTION);
        newOptionSection.setAttribute(OPTION_NUMBER_ATTR, Integer.toString(optionNumber));
        if(optionValue != null && optionValue.length() != 0) {
            newOptionSection.setAttribute(OPTION_VALUE_ATTR, optionValue);
        }

        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        int pos;
        Element optionSection = null;
        for(pos = 0; pos < optionSections.length; ++pos){
            optionSection = (Element) optionSections[pos].getDomNode();
            int curOptionNumber = readOptionNumber(optionSection);
            if(optionNumber > curOptionNumber) break;
        }
        if(pos == optionSections.length) getConfig().getDomNode().appendChild(newOptionSection); else getConfig().getDomNode().insertBefore(newOptionSection, optionSection);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionAdded(pos, optionNumber, optionValue);
            }
        }
        if(optionNumber == OptionNumberRegistry.CONTENT_FORMAT){
            notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, null, optionValue == null ? "" : optionValue);
        }
        return pos;
    }

    @Override
    public void removeOption(int optionIndex) {
        Element optionSection = getOptionSection(optionIndex);
        String oldOption = readOptionValue(optionSection);
        int oldOptionNumber = readOptionNumber(optionSection);
        getConfig().getDomNode().removeChild(optionSection);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionRemoved(optionIndex, oldOptionNumber, oldOption);
            }
        }
        if(oldOptionNumber == OptionNumberRegistry.CONTENT_FORMAT){
            notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, oldOption, null);
        }
    }


    @Override
    public void moveOption(int optionIndex, int delta) {
        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        int optionNumber = readOptionNumber(optionSections[optionIndex]);
        String optionValue = readOptionValue(optionSections[optionIndex]);
        Node movedSection = optionSections[optionIndex].getDomNode();
        getConfig().getDomNode().removeChild(movedSection);
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionRemoved(optionIndex, optionNumber, optionValue);
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
                listener.onOptionAdded(optionIndex, optionNumber, optionValue);
            }
        }
    }

    private String getMediaTypeString(String optionValue){
        Long mediaType = KnownOptions.decodeIntOptionValue(optionValue, 2);
        if(mediaType == null || mediaType == MediaTypeRegistry.UNDEFINED){
            return "";
        }
        for(int it: MediaTypeRegistry.getAllMediaTypes()){
            if(mediaType == it) return MediaTypeRegistry.toString(mediaType.intValue());
        }
        return "";

    }

    @Override
    public String getMediaType() {
        return getMediaTypeString(getContentFormatOption());
    }

    private void contenfFomatChanged(String oldValue, String newValue){
        notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, oldValue == null ? "" : oldValue, newValue == null ? "" : newValue);
        String oldMediaType = getMediaTypeString(oldValue);
        String newMediaType = getMediaTypeString(newValue);
        if(Utils.areStringsEqual(oldMediaType, newMediaType, true, true)) notifyPropertyChanged(MEDIA_TYPE, oldMediaType, newMediaType);
    }

    @Override
    public void setMediaType(String mediaType) {
        if(StringUtils.isNullOrEmpty(mediaType)) return;
        int number = MediaTypeRegistry.parse(mediaType);
        if(number == MediaTypeRegistry.UNDEFINED) return;
        setContentFormatOption(KnownOptions.encodeIntOptionValue(number, 2));
    }

    public String getContentFormatOption(){
        XmlObject[] mediaTypeSections = getConfig().selectPath(String.format("$this/%s[@%s=%d]", OPTION_SECTION, OPTION_NUMBER_ATTR, OptionNumberRegistry.CONTENT_FORMAT));
        if(mediaTypeSections == null || mediaTypeSections.length == 0) return null;
        if(mediaTypeSections.length != 1){
            SoapUI.log(String.format("Incorrect data (duplicated Content-Format option) in the %s test step", getName()));
        }
        return readOptionValue(mediaTypeSections[0]);
    }

    public void setContentFormatOption(String value){
        String oldValue = getContentFormatOption();
        if(Utils.areStringsEqual(oldValue, value, true, false)) return;
        int count = getOptionCount();
        for(int i = 0; i < count; ++i){
            if(getOptionNumber(i) == OptionNumberRegistry.CONTENT_FORMAT){
                if(value == null) removeOption(i); else setOption(i, value);
                notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, oldValue, value);
                return;
            }
        }
        addOption(OptionNumberRegistry.CONTENT_FORMAT, value);
        notifyPropertyChanged(CONTENT_FORMAT_OPTION_BEAN_PROP, oldValue, value);
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
