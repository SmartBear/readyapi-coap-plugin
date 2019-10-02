package com.smartbear.coapsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder;
import com.eviware.soapui.impl.wsdl.submit.RequestFilter;
import com.eviware.soapui.impl.wsdl.submit.RequestTransport;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.log.Log4JMonitor;
import com.eviware.soapui.support.types.StringList;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.EndpointManager;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.eviware.soapui.impl.support.HttpUtils.urlEncodeWithUtf8;

//@PluginRequestTransport(protocol = "coap")
public class CoapTransport implements RequestTransport {

    public final static String REQUEST_CONTEXT_PROP = "CoapRequest";
    public final static String RESPONSE_CONTEXT_PROP = "CoapResponse";
    private static final boolean logExchange = true;

    public class CoapSubmitFailureException extends RuntimeException{
        public CoapSubmitFailureException(String msg){
            super(msg);
        }

        public CoapSubmitFailureException(String msg, Throwable innerException){
            super(msg, innerException);
        }
    }

    private List<RequestFilter> filters = new ArrayList<RequestFilter>();
    private final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PluginConfig.COAP_LOG);

    private boolean isLoggerEstablished = false;
    private boolean isLogTabCreated = false;

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
                    if (query.length() > 0 || endpoint.contains("?")) {
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

        org.eclipse.californium.core.coap.Request message = new org.eclipse.californium.core.coap.Request(reqestType, request.getConfirmable() ? CoAP.Type.CON : CoAP.Type.NON);
        message.setURI(endpoint + query.toString());
        int optionCount = request.getOptionCount();
        for (int i = 0; i < optionCount; ++i) {
            Option option = new Option(request.getOptionNumber(i));
            String rawValue = request.getOptionValue(i);
            String expValue = null;
            if(rawValue != null) expValue = PropertyExpander.expandProperties(context, rawValue);
            if(expValue != null){
                if(expValue.startsWith("0x0x")){
                    option.setStringValue(expValue.substring(2));
                }
                else if(expValue.startsWith("0x")){
                    byte[] binValue;
                    try {
                        binValue = Utils.hexStringToBytes(expValue.substring(2));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format("\"%s\" string could not be interpreted as a binary value of the option %d", expValue, option.getNumber()));
                    }
                    option.setValue(binValue);
                }
                else {
                    option.setStringValue(expValue);
                }
            }
            message.getOptions().addOption(option);
        }

        byte[] actualRequestPayload = null;
        if(request.hasRequestBody()) {
            String requestContent = PropertyExpander.expandProperties(context, request.getRequestContent());
            if(requestContent != null && requestContent.length() != 0) {
                if (StringUtils.isNullOrEmpty(request.getMediaType())) {
                    if (requestContent.startsWith("0x0x")) {
                        actualRequestPayload = requestContent.substring(2).getBytes(Charset.forName("UTF-8"));
                    }
                    else if (requestContent.trim().startsWith("0x")) {
                        try{
                            actualRequestPayload = Utils.hexStringToBytes(requestContent.trim().substring(2));
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException(String.format("\"%s\" string could not be interpreted as a binary request content", Utils.limitStringLen(requestContent, 50)));
                        }
                    }
                    else {
                        actualRequestPayload = requestContent.getBytes(Charset.forName("UTF-8"));
                    }
                }
                else{
                    Integer mediaTypeNumber = request.getContentFormatAsNumber();
                    if(MediaTypeRegistry.isPrintable(mediaTypeNumber)){
                        actualRequestPayload = requestContent.getBytes(Charset.forName("UTF-8"));
                    }
                    else{
                        requestContent = requestContent.trim();
                        if(requestContent.startsWith("0x")) requestContent = requestContent.substring(2);
                        try{
                            actualRequestPayload = Utils.hexStringToBytes(requestContent);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException(String.format("\"%s\" string could not be interpreted as a binary request content", Utils.limitStringLen(requestContent, 50)));
                        }
                    }
                }
            }
            message.setPayload(actualRequestPayload);
        }

        context.setProperty(REQUEST_CONTEXT_PROP, message);
        filterRequest(context, request);
        if(request.getAutoSize1()){
            message.getOptions().setSize1(message.getPayloadSize());
        }

        long startTime = System.currentTimeMillis();
        if(logExchange){
            if(!isLogTabCreated){
                Log4JMonitor logMonitor = SoapUI.getLogMonitor();
                if(logMonitor != null) {
                    logMonitor.addLogArea("CoAP log", PluginConfig.COAP_LOG, false, true);
                    isLogTabCreated = true;
                }
            }

            Endpoint clientEndpoint = EndpointManager.getEndpointManager().getDefaultEndpoint();
            if(!isLoggerEstablished){

                clientEndpoint.addInterceptor(new CoapTracer(logger));
                isLoggerEstablished = true;
            }
            message.send(clientEndpoint);
        }
        else {
            message.send();
        }

        org.eclipse.californium.core.coap.Response responseMessage = message.waitForResponse(timeout);

        context.setProperty(RESPONSE_CONTEXT_PROP, responseMessage);
        filterResponse(context, request);

        if(message.isCanceled()) throw new CoapSubmitFailureException("Request was canceled.");
        long takenTime = System.currentTimeMillis() - startTime;
        if(responseMessage != null) return new CoapRespImpl(request, actualRequestPayload, responseMessage, takenTime);
        if(message.isRejected()) throw new CoapSubmitFailureException("Request was rejected by the recepient.");
        if(message.isTimedOut()) throw new CoapSubmitFailureException("Request was not received within the MAX_TRANSMIT_WAIT period.");
        throw new CoapSubmitFailureException("Response was not received within the specified timeout.");
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

    private void filterResponse(SubmitContext submitContext, CoapRequest coapRequest) {
        for (RequestFilter filter : filters) {
            try {
                filter.afterRequest(submitContext, coapRequest);
            } catch (Throwable e) {
                SoapUI.logError(e, "Error while filtering CoAP response " + coapRequest);
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

}
