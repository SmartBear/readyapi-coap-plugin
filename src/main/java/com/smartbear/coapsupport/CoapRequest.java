package com.smartbear.coapsupport;


import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.support.UISupport;
import org.apache.xmlbeans.XmlObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.swing.ImageIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class CoapRequest extends HttpTestRequest implements CoapOptionsDataSource {
    private final static String OUR_SCHEMA = null;// "http://smartbear.com/rapi/config";
    private static final String OPTION_SECTION = "Option";
    private static final String OPTION_VALUE_SECTION = "Value";
    private static final String OPTION_NUMBER_ATTR = "Number";

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
                listener.onWholeOptionListChanged(getOptions());
            }
        }
    }

    private static String getElementText(XmlObject obj){
        String result = "";
        Node child = (Element)obj.getDomNode().getFirstChild();
        do{
            if(child.getNodeType() == Node.TEXT_NODE){
                result += ((Text)child).getWholeText();
            }
            child = child.getNextSibling();
        }
        while (child != null);
        return result;
    }

    private ArrayList<String> readOptionValues(XmlObject optionSection){
        ArrayList<String> result = new ArrayList<String>();
        XmlObject[] valueSections = optionSection.selectPath("$this/" + OPTION_VALUE_SECTION);
        if(valueSections != null){
            for(XmlObject valueSection: valueSections){
                valueSection.getDomNode().getNodeValue();
                result.add(getElementText(valueSection));
            }
        }
        return result;

    }

    public List<CoapOption> getOptions(){
        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION);
        ArrayList<CoapOption> result = new ArrayList<>();
        if(optionSections == null) return result;
        for(XmlObject optionSection: optionSections){
            String optionNumberStr = ((Element) optionSection.getDomNode()).getAttribute(OPTION_NUMBER_ATTR);
            int optionNumber;
            try {
                optionNumber = Integer.parseInt(optionNumberStr);
            } catch (NumberFormatException e) {
                SoapUI.logError(e, String.format("Incorrect data (\"%s\") in the %s test step", optionNumberStr, getName()));
                continue;
            }
            CoapOption option = new CoapOption(optionNumber);
            option.values = readOptionValues(optionSection);
            result.add(option);
        }
        Collections.sort(result, new Comparator<CoapOption>() {
            @Override
            public int compare(CoapOption o1, CoapOption o2) {
                if(o1.number == o2.number) SoapUI.log(String.format("Incorrect data (%d option is duplicated) in the %s test step", o1.number, getName()));
                return o1.number - o2.number;
            }
        });
        return result;
    }

    public void setOption(int optionNumber, List<String> optionValues){
        CoapOptionsListener.ChangeKind change = null;
        XmlObject[] optionSections = getConfig().selectPath("$this/" + OPTION_SECTION + "[@" + OPTION_NUMBER_ATTR + "=\'" + Integer.toString(optionNumber) + "\']");
        ArrayList<String> oldValues = null;
        if(optionSections == null) {
            if(optionValues == null) return;
            change = CoapOptionsListener.ChangeKind.Added;
        }
        else{
            oldValues = readOptionValues(optionSections[0]);
            if(optionValues != null){
                if(oldValues.size() == optionValues.size()){
                    boolean noChanges = true;
                    for(int i = 0; i < oldValues.size(); ++i){
                        if(!Utils.areStringsEqual(oldValues.get(i), optionValues.get(i), false, true)){
                            noChanges = false;
                            break;
                        }
                    }
                    if(noChanges) return;
                }
            }
            getConfig().getDomNode().removeChild(optionSections[0].getDomNode());
        }
        if(optionValues == null) {
            change = CoapOptionsListener.ChangeKind.Removed;
        }
        else{
            Document document = getConfig().getDomNode().getOwnerDocument();
            Element newSection = document.createElement(OPTION_SECTION);
            newSection.setAttribute(OPTION_NUMBER_ATTR, Integer.toString(optionNumber));
            for(String value: optionValues){
                Element newValueSection = document.createElement(OPTION_VALUE_SECTION);
                Text valueNode = document.createTextNode(value);
                newValueSection.appendChild(valueNode);
                newSection.appendChild(newValueSection);
            }
            getConfig().getDomNode().appendChild(newSection);
            if(change != CoapOptionsListener.ChangeKind.Added) change = CoapOptionsListener.ChangeKind.ValueChanged;
        }
        if(optionsListeners != null){
            for(CoapOptionsListener listener: optionsListeners){
                listener.onOptionChanged(optionNumber, change, oldValues, optionValues);
            }
        }

    }

    public void addOption(int optionNumber, List<String> optionValues){
        setOption(optionNumber, optionValues);
    }

    public void removeOption(int optionNumber){
        setOption(optionNumber, null);
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
