package com.smartbear.coapsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.submit.RequestTransportRegistry;
import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.support.UISupport;
import org.eclipse.californium.core.network.config.NetworkConfig;

@PluginConfiguration(groupId = "com.smartbear.plugins", name = "CoAP Support Plugin", version = "1.0.1",
        autoDetect = true, description = "Adds CoAP Request test step to SoapUI NG",
        infoUrl = "", minimumReadyApiVersion = "2.2.0")
public class PluginConfig extends PluginAdapter {

    final static String COAP_LOG = "coap.log";
    private boolean shouldLoad;


    public PluginConfig() {
        super();
        try {
            Class.forName("com.smartbear.ready.ui.ReadyApiMain");
            shouldLoad = true;
        } catch (Throwable e) {
            shouldLoad = false;
        }
        UISupport.addResourceClassLoader(getClass().getClassLoader());
    }


    @Override
    public boolean isActive() {
        return super.isActive() && shouldLoad;
    }

    @Override
    public void initialize() {
        super.initialize();
        SoapUI.getActionRegistry().addActionGroup(new CoapRequestTestStepActionGroup());
        NetworkConfig.createStandardWithoutFile();
        try {
            if (RequestTransportRegistry.getTransport("coap") == null) {
                RequestTransportRegistry.addTransport("coap", new CoapTransport());
            }
        } catch (RequestTransportRegistry.MissingTransportException e) {
            RequestTransportRegistry.addTransport("coap", new CoapTransport());
        }
    }

}
