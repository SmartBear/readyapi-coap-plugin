package com.smartbear.coapsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.submit.RequestTransportRegistry;
import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.support.UISupport;
import com.smartbear.coapsupport.CoapRequestTestStepActionGroup;
import org.eclipse.californium.core.network.config.NetworkConfig;

@PluginConfiguration(groupId = "com.smartbear.plugins", name = "CoAP Protocol Support Plugin", version = "1.0.0-SNAPSHOT",
        autoDetect = true, description = "Adds CoAP TestStep to SoapUI NG",
        infoUrl = "")
public class PluginConfig extends PluginAdapter {

    final static String COAP_LOG = "coap.log";


    public PluginConfig(){
        super();
        UISupport.addResourceClassLoader(getClass().getClassLoader());
    }

    @Override
    public void initialize() {
        super.initialize();
        SoapUI.getActionRegistry().addActionGroup(new CoapRequestTestStepActionGroup());
        NetworkConfig.createStandardWithoutFile();
        RequestTransportRegistry.addTransport("coap", new CoapTransport());
    }

}
