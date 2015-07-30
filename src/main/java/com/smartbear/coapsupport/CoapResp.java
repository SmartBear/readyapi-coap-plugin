package com.smartbear.coapsupport;

import org.eclipse.californium.core.coap.Response;
import com.eviware.soapui.impl.wsdl.submit.transports.http.HttpResponse;

public interface CoapResp extends HttpResponse {
    Response getResponseMessage();

}
