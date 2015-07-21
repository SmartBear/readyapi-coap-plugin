package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.Response;
import com.eviware.soapui.impl.support.panels.AbstractHttpXmlRequestDesktopPanel;
import com.eviware.soapui.impl.wsdl.submit.transports.http.HttpResponse;

class CoapResponseDocument extends AbstractHttpXmlRequestDesktopPanel.HttpResponseDocument {

    public CoapResponseDocument(CoapRequest request) {
        super(request);
    }

    public Response getResponseMessage(){
        CoapResp resp = getResponse();
        if(resp == null)
            return null;
        else {
            Response result = resp.getResponseMessage();
            return result;
        }
    }

    public CoapResp getResponse(){
        HttpResponse r = ((CoapRequest) getRequest()).getResponse();
        CoapResp result = (CoapResp)r;
        return result;
    }
}
