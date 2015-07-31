package com.smartbear.coapsupport;

import com.eviware.soapui.config.AbstractRequestConfig;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.config.SettingConfig;
import com.eviware.soapui.config.SettingsConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.support.AbstractHttpRequestInterface;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestInterface;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestProperty;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepProperty;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import javax.xml.namespace.QName;
import java.beans.PropertyChangeEvent;

@PluginTestStep(typeName = "CoapRequestTestStep", name = "CoAP Request", description = "Sends a request using the CoAP protocol.", iconPath = "com/smartbear/coapsupport/coap_request_step.png")
public class CoapRequestTestStep extends HttpTestRequestStep {
    public final static String RESPONSE_ETAG_PROP = "ResponseETag";
    public static final String RESPONSE_LOCATION_PROP = "ResponseLocation";
    public static final String RESPONSE_PROP = "Response";


    public CoapRequestTestStep(WsdlTestCase testCase, TestStepConfig testStepData, boolean forLoadTest) {
        super(testCase, correctTestStepData(testStepData), forLoadTest);

        deleteProperty(RESPONSE, false);
        deleteProperty(RESPONSE_AS_XML, false);
        deleteProperty("Username", false);
        deleteProperty("Password", false);
        deleteProperty("Domain", false);
        addProperty(new TestStepBeanProperty(RESPONSE_ETAG_PROP, true, getRequest(), CoapRequest.RESPONSE_ETAG_BEAN_PROP, this));
        addProperty(new TestStepBeanProperty(RESPONSE_LOCATION_PROP, true, getRequest(), CoapRequest.RESPONSE_LOCATION_BEAN_PROP, this));
        addProperty(new DefaultTestStepProperty(RESPONSE, true, this){
            @Override
            public String getValue() {
                return getRequest().getResponse() == null ? null : getRequest().getResponse().getContentAsString();
            }
        });
        addProperty(new DefaultTestStepProperty(RESPONSE_AS_XML, true, this){
            @Override
            public String getValue() {
                return getRequest().getResponse() == null ? null : getRequest().getResponse().getContentAsXml();
            }
        });
    }

    @Override
    protected HttpTestRequest buildTestRequest(boolean forLoadTest) {
        return new CoapRequest(this, (HttpRequestConfig)getConfig().getConfig(), forLoadTest);
    }

    public CoapRequest getRequest(){return (CoapRequest)getTestRequest();}

    private static TestStepConfig correctTestStepData(TestStepConfig testStepData){
        if(testStepData.getConfig() == null){
            HttpRequestConfig httpConfig = (HttpRequestConfig) testStepData.addNewConfig().changeType(HttpRequestConfig.type);
            httpConfig.setMethod(RestRequestInterface.HttpMethod.GET.toString());
            httpConfig.setTimeout(Integer.toString(93000)); //MAX_TRANSMIT_WAIT
        }
        return testStepData;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if(event.getSource() == getRequest()) {
            if(CoapRequest.RESPONSE_ETAG_BEAN_PROP.equals(event.getPropertyName())){
                firePropertyValueChanged(RESPONSE_ETAG_PROP, (String)event.getOldValue(), (String) event.getNewValue());
            }
            else if(CoapRequest.RESPONSE_LOCATION_BEAN_PROP.equals(event.getPropertyName())){
                firePropertyValueChanged(RESPONSE_LOCATION_PROP, (String)event.getOldValue(), (String) event.getNewValue());
            }
            else if(AbstractHttpRequestInterface.RESPONSE_PROPERTY.equals(event.getPropertyName())){
                CoapResp oldResponseObj = (CoapResp) event.getOldValue();
                String oldResponse = null, oldResponseXml = null;
                if(oldResponseObj != null){
                    oldResponse = oldResponseObj.getContentAsString();
                    oldResponseXml = oldResponseObj.getContentAsXml();
                }
                CoapResp newResponseObj = (CoapResp) event.getNewValue();
                String newResponse = null, newResponseXml = null;
                if(newResponseObj != null){
                    newResponse = newResponseObj.getContentAsString();
                    newResponseXml = newResponseObj.getContentAsXml();
                }

                firePropertyValueChanged("Response", oldResponse, newResponse);
                firePropertyValueChanged("ResponseAsXml", oldResponseXml, newResponseXml);
            }
        }
        super.propertyChange(event);
    }
}
