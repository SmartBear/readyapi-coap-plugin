package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.CoapResponse;
import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.MessageObserver;
import ch.ethz.inf.vs.californium.coap.Request;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder;
import com.eviware.soapui.impl.wsdl.submit.RequestFilter;
import com.eviware.soapui.impl.wsdl.submit.RequestTransport;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.plugins.auto.PluginRequestTransport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.types.StringList;

import java.util.ArrayList;
import java.util.List;

import static com.eviware.soapui.impl.support.HttpUtils.urlEncodeWithUtf8;

@PluginRequestTransport(protocol = "coap")
public class CoapTransport implements RequestTransport {

    public final static String REQUEST_CONTEXT_PROP = "CoapRequest";
    public final static String ACKNOWLEDGED_CONTEXT_PROP = "IsCoapRequestAcknowledged";
    public final static String STATUS_CONTEXT_PROP = "CoapSubmitStatus";

    public enum SubmitStatus{InProgress, GotResponse, Rejected, Timeout, Canceled};
    public class CoapSubmitFailureException extends RuntimeException{
        public CoapSubmitFailureException(String msg){
            super(msg);
        }

        public CoapSubmitFailureException(String msg, Throwable innerException){
            super(msg, innerException);
        }
    }

    private List<RequestFilter> filters = new ArrayList<RequestFilter>();

    @Override
    public void abortRequest(SubmitContext submitContext) {
        Object requestPropValue = submitContext.getProperty(REQUEST_CONTEXT_PROP);
        if(requestPropValue instanceof Request){
            Request request = (Request)requestPropValue;
            request.cancel();
        }

    }


    private static List<String> splitMultipleParameters(String paramStr, String delimiter, boolean allowEmptyParam) {
        StringList result = new StringList();

        if (StringUtils.hasContent(paramStr) || allowEmptyParam) {
            if (!StringUtils.hasContent(delimiter)) {
                result.add(paramStr);
            } else {
                result.addAll(paramStr.split(delimiter));
            }
        }

        return result;

    }

    @Override
    public Response sendRequest(SubmitContext context, com.eviware.soapui.model.iface.Request req) throws Exception {
        CoapRequest request = (CoapRequest)req;

        filterRequest(context, request);

        String endpoint = request.getEndpoint();
        endpoint = PropertyExpander.expandProperties(context, endpoint);

        StringBuilder query = new StringBuilder();
        RestParamsPropertyHolder params = request.getParams();

        for (int c = 0; c < params.getPropertyCount(); c++) {
            RestParamProperty param = params.getPropertyAt(c);

            String value = PropertyExpander.expandProperties(context, param.getValue());
            List<String> valueParts = splitMultipleParameters(value, request.getMultiValueDelimiter(), request.isSendEmptyParameters() || param.getRequired());

            if(param.getStyle() == RestParamsPropertyHolder.ParameterStyle.QUERY){
                for (String valuePart : valueParts) {
                    if (query.length() > 0) {
                        query.append('&');
                    }
                    else{
                        query.append('?');
                    }

                    query.append(urlEncodeWithUtf8(param.getName()));
                    query.append('=');
                    if (StringUtils.hasContent(valuePart)) {
                        query.append(valuePart);
                    }
                }
            }
        }
        CoAP.Code reqestType = CoAP.Code.GET;
        switch (request.getMethod()){
            case POST:
                reqestType = CoAP.Code.POST;
                break;
            case PUT:
                reqestType = CoAP.Code.PUT;
                break;
            case DELETE:
                reqestType = CoAP.Code.DELETE;
                break;
        }

        String timeoutStr = context.expand(request.getTimeout());
        long timeout = 93000; //MAX_TRANSMIT_WAIT
        try {
            timeout = Long.parseLong(timeoutStr);
        }
        catch (NumberFormatException ignored){
        }

        ch.ethz.inf.vs.californium.coap.Request message = new ch.ethz.inf.vs.californium.coap.Request(reqestType, request.getConfirmable() ? CoAP.Type.CON : CoAP.Type.NON);
        message.setURI(endpoint + query.toString());
        context.setProperty(REQUEST_CONTEXT_PROP, message);
        context.setProperty(ACKNOWLEDGED_CONTEXT_PROP, false);
        context.setProperty(STATUS_CONTEXT_PROP, SubmitStatus.InProgress);
        CoapMessageObserver observer = new CoapMessageObserver(message, context);
        message.addMessageObserver(observer);

        long startTime = System.currentTimeMillis();
        message.send();

        ch.ethz.inf.vs.californium.coap.Response responseMessage = message.waitForResponse(timeout);
        long takenTime = System.currentTimeMillis() - startTime;
        switch((SubmitStatus)context.getProperty(STATUS_CONTEXT_PROP)){
            case Rejected: throw new CoapSubmitFailureException("Request was rejected by the recepient.");
            case Timeout: throw new CoapSubmitFailureException("Response was not received within the timeout.");
            case Canceled: throw new CoapSubmitFailureException("Request was canceled.");
        }

        return new CoapResp(request, responseMessage, takenTime);
    }

    private void filterRequest(SubmitContext submitContext, CoapRequest coapRequest) {
        for (RequestFilter filter : filters) {
            try {
                filter.filterRequest(submitContext, coapRequest);
            } catch (Throwable e) {
                SoapUI.logError(e, "Error while filtering CoAP request " + coapRequest);
            }
        }
    }

    @Override
    public void addRequestFilter(RequestFilter filter) {
        filters.add(filter);
    }

    @Override
    public void removeRequestFilter(RequestFilter filterToRemove) {
        if (!filters.remove(filterToRemove)) {
            for (RequestFilter requestFilter : filters) {
                if (requestFilter.getClass().equals(filterToRemove.getClass())) {
                    filters.remove(requestFilter);
                    break;
                }
            }
        }

    }

    @Override
    public void insertRequestFilter(RequestFilter filter, RequestFilter refFilter) {
        int ix = filters.indexOf( refFilter );
        if( ix == -1 )
            filters.add( filter );
        else
            filters.add( ix, filter );
    }


    private class CoapMessageObserver implements MessageObserver {
        private SubmitContext context;
        private Request request;

        public CoapMessageObserver(Request request, SubmitContext context){
            this.context = context;
            this.request = request;
        }

        @Override
        public void onRetransmission() {

        }

        @Override
        public void onResponse(ch.ethz.inf.vs.californium.coap.Response response) {
            context.setProperty(STATUS_CONTEXT_PROP, SubmitStatus.GotResponse);
        }

        @Override
        public void onAcknowledgement() {
            context.setProperty(ACKNOWLEDGED_CONTEXT_PROP, true);
        }

        @Override
        public void onReject() {
            context.setProperty(STATUS_CONTEXT_PROP, SubmitStatus.Rejected);
        }

        @Override
        public void onTimeout() {
            context.setProperty(STATUS_CONTEXT_PROP, SubmitStatus.Timeout);
        }

        @Override
        public void onCancel() {
            context.setProperty(STATUS_CONTEXT_PROP, SubmitStatus.Canceled);
        }
    }
}
