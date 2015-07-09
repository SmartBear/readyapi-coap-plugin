package com.smartbear.coapsupport;


import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.impl.support.http.HttpRequest;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestInterface;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.support.UISupport;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.ImageIcon;


public class CoapRequest extends HttpTestRequest {
    private final static String HTTP_SECTION = "HttpSimilarData";
    private final static String OUR_SCHEMA = null;// "http://smartbear.com/rapi/config";

    private CoapRequestTestStep testStep;
    private ImageIcon validRequestIcon;
    private ImageIcon failedRequestIcon;
    private ImageIcon unknownRequestIcon;
    private ImageIcon disabledRequestIcon;
    private boolean forLoadTest;

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
        XmlObject xmlObject = getConfig().selectAttribute(OUR_SCHEMA, "confirmable");
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


}
