package com.smartbear.coapsupport;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;

public class CoapTracer implements MessageInterceptor {
    private Logger logger;

    public CoapTracer(Logger logger){
        this.logger = logger;
    }

    @Override
    public void sendRequest(Request request) {
        handleCoapMessage(String.format("%s:%d <== REQ %s",
                request.getDestination(), request.getDestinationPort(), toString(request)));
    }

    @Override
    public void sendResponse(Response response) {
        handleCoapMessage(String.format(
                "%s:%d <== RES %s",
                response.getDestination(), response.getDestinationPort(), toString(response)));
    }

    @Override
    public void sendEmptyMessage(EmptyMessage message) {
        handleCoapMessage(String.format("%s:%d <== EMP %s", message.getDestination(), message.getDestinationPort(), message));
    }

    @Override
    public void receiveRequest(Request request) {
        handleCoapMessage(String.format("%s:%d ==> REQ %s", request.getSource(), request.getSourcePort(), toString(request)));
    }

    @Override
    public void receiveResponse(Response response) {
        handleCoapMessage(String.format("%s:%d ==> RES %s", response.getSource(), response.getSourcePort(), toString(response)));
    }

    @Override
    public void receiveEmptyMessage(EmptyMessage message) {
        handleCoapMessage(String.format("%s:%d ==> EMP %s", message.getSource(), message.getSourcePort(), message));
    }

    protected void handleCoapMessage(String msgInfo){
        if (logger == null) {
            System.out.println(msgInfo);
        }
        else{
            logger.info(msgInfo);
        }
    }

    protected String toString(Request msg){
        return String.format("%s-%-6s MID=%5d, Token=%s, Options:%s, %s", msg.getType(), msg.getCode(), msg.getMID(), msg.getTokenString(), msg.getOptions(), payloadToString(msg));

    }

    protected String toString(Response msg){
        return String.format("%s-%-6s MID=%5d, Token=%s, Options:%s, %s", msg.getType(), msg.getCode(), msg.getMID(), msg.getTokenString(), msg.getOptions(), payloadToString(msg));
    }

    protected String payloadToString(Message msg){
        if (msg.getPayloadSize() == 0) {
            return "no payload";
        }
        else {
            String payload = msg.getPayloadString();
            String text = Utils.limitStringLen(payload, 50);
            return String.format("Payload: %s %d bytes", text, msg.getPayloadSize());
        }
    }

}
