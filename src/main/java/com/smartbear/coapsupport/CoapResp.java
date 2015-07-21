package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.Response;
import com.eviware.soapui.impl.wsdl.submit.transports.http.HttpResponse;

public interface CoapResp extends HttpResponse {
    Response getResponseMessage();

}
