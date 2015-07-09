package com.smartbear.coapsupport;

import com.eviware.soapui.config.AbstractRequestConfig;
import com.eviware.soapui.config.HttpRequestConfig;
import com.eviware.soapui.config.SettingConfig;
import com.eviware.soapui.config.SettingsConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.ModelItem;
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
public class CoapRequestTestStep extends WsdlTestStepWithProperties {


    private CoapRequest request;
    private HttpRequestConfig httpRequestConfig;

    public CoapRequestTestStep(WsdlTestCase testCase, TestStepConfig testStepData, boolean forLoadTest) {
        super(testCase, testStepData, true, forLoadTest);
        XmlObject config = testStepData.getConfig();
        if(config == null){
            testStepData.addNewConfig().changeType(HttpRequestConfig.type);

        }
        httpRequestConfig = (HttpRequestConfig)testStepData.getConfig();
        request = buildRequest(forLoadTest);

        for (TestProperty property : request.getProperties().values()) {
            addProperty(new RestTestStepProperty((RestParamProperty) property));
        }
    }


    private CoapRequest buildRequest(boolean forLoadTest) {
        return new CoapRequest(this, httpRequestConfig, forLoadTest);

    }

    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        return new WsdlTestStepResult(this);
    }

    public CoapRequest getRequest(){return request;}

    private class RestTestStepProperty implements TestStepProperty {
        private RestParamProperty property;

        public RestTestStepProperty(RestParamProperty property) {
            this.property = property;
        }

        public TestStep getTestStep() {
            return CoapRequestTestStep.this;
        }

        public String getName() {
            return property.getName();
        }

        public String getDescription() {
            return property.getDescription();
        }

        public String getValue() {
            return property.getValue();
        }

        public String getDefaultValue() {
            return property.getDefaultValue();
        }

        public void setValue(String value) {
            property.setValue(value);
        }

        public boolean isReadOnly() {
            return false;
        }

        public QName getType() {
            return property.getType();
        }

        public ModelItem getModelItem() {
            return getRequest();
        }

        @Override
        public boolean isRequestPart() {
            return true;
        }

        @Override
        public SchemaType getSchemaType() {
            return property.getSchemaType();
        }
    }

}
