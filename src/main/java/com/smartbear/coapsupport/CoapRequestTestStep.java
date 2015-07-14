package com.smartbear.coapsupport;

import com.eviware.soapui.config.AbstractRequestConfig;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.config.SettingConfig;
import com.eviware.soapui.config.SettingsConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.ModelItem;
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

@PluginTestStep(typeName = "CoapRequestTestStep", name = "CoAP Request", description = "Sends a request using the CoAP protocol.", iconPath = "com/smartbear/coapsupport/coap_request_step.png")
public class CoapRequestTestStep extends HttpTestRequestStep {

    public CoapRequestTestStep(WsdlTestCase testCase, TestStepConfig testStepData, boolean forLoadTest) {
        super(testCase, correctTestStepData(testStepData), forLoadTest);

        //deleteProperty("Endpoint", false);
        deleteProperty("Username", false);
        deleteProperty("Password", false);
        deleteProperty("Domain", false);
    }

    @Override
    protected HttpTestRequest buildTestRequest(boolean forLoadTest) {
        return new CoapRequest(this, (HttpRequestConfig)getConfig().getConfig(), forLoadTest);
    }

    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        return new WsdlTestStepResult(this);
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
}
