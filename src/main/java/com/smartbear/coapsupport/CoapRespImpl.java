package com.smartbear.coapsupport;

import com.eviware.soapui.SoapUI;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import com.eviware.soapui.impl.rest.support.MediaTypeHandler;
import com.eviware.soapui.impl.rest.support.MediaTypeHandlerRegistry;
import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.support.AbstractHttpRequestInterface;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.submit.transports.http.SSLInfo;
import com.eviware.soapui.model.iface.Attachment;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.types.StringToStringsMap;
import com.google.common.base.Charsets;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class CoapRespImpl implements CoapResp {
    private StringToStringMap properties = new StringToStringMap();
    private CoapRequest request;
    private long takenTime;
    private int statusCode;
    //private byte[] responsePayload;
    private String responsePayloadString;
    private String responseContentType;
    private String xmlContent;
    private Response response;
    private static CoapUrlStreamHandler urlHandler;



    public CoapRespImpl(CoapRequest request, Response response, long takenTime) {
        if(urlHandler == null) urlHandler = new CoapUrlStreamHandler();
        this.request = request;
        this.takenTime = takenTime;
        this.response = response;
        if(response == null){
            statusCode = 0;
        }
        else {
            int rawStatus = response.getCode().value;
            statusCode = ((rawStatus & 0xe0) >> 5) * 100 + (rawStatus & 0x1f);
            if (response.getPayload() != null){
                //responsePayload = Arrays.copyOf(response.getPayload(), response.getPayloadSize());
                responsePayloadString = new String(response.getPayload(), Charsets.UTF_8);
            }
            responseContentType = MediaTypeRegistry.toString(response.getOptions().getContentFormat());
        }
//        this.responseContent = responseContent;
//        this.responseContentType = responseContentType;
    }


    @Override
    public AbstractHttpRequestInterface<?> getRequest() {
        return request;
    }

    @Override
    public void setResponseContent(String responseContent) {
        String oldContent = this.responsePayloadString;
        this.responsePayloadString = responseContent;

        ((AbstractHttpRequest<?>) getRequest()).notifyPropertyChanged(WsdlRequest.RESPONSE_CONTENT_PROPERTY,
                oldContent, responseContent);

    }

    @Override
    public SSLInfo getSSLInfo() {
        return null;
    }

    @Override
    public URL getURL() {
        try {
            return new URL(null, getRequest().getEndpoint(), urlHandler);
        } catch (MalformedURLException e) {
            SoapUI.logError(e);
            return null;
        }

    }

    @Override
    public String getMethod() {
        return request.getMethod().toString();
    }

    @Override
    public String getHttpVersion() {
        return "";
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getRequestContent() {
        return request.getRequestContent();
    }

    @Override
    public long getTimeTaken() {
        return takenTime;
    }

    @Override
    public Attachment[] getAttachments() {
        return new Attachment[0];
    }

    @Override
    public Attachment[] getAttachmentsForPart(String partName) {
        return new Attachment[0];
    }

    @Override
    public StringToStringsMap getRequestHeaders() {
        return new StringToStringsMap();
    }

    @Override
    public StringToStringsMap getResponseHeaders() {
        return new StringToStringsMap();
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public byte[] getRawRequestData() {
        return request.getRequestContent() == null ? null : request.getRequestContent().getBytes();
    }

    @Override
    public byte[] getRawResponseData() {
        //return responsePayload;
        return response.getPayload();
    }

    @Override
    public String getContentAsXml() {
        if (xmlContent == null) {
            MediaTypeHandler typeHandler = MediaTypeHandlerRegistry.getTypeHandler(getContentType());
            xmlContent = (typeHandler == null) ? "<xml/>" : typeHandler.createXmlRepresentation(this);
        }
        return xmlContent;
    }

    @Override
    public String getProperty(String name) {
        return properties.get( name );
    }

    @Override
    public void setProperty(String name, String value) {
        properties.put( name, value );
    }

    @Override
    public String[] getPropertyNames() {
        return properties.getKeys();
    }

    @Override
    public String getContentAsString() {
        return responsePayloadString;
    }

    @Override
    public String getContentType() {
        return responseContentType;
    }

    @Override
    public long getContentLength() {
        return responsePayloadString == null ? 0 : responsePayloadString.length();
    }


    public Response getResponseMessage(){
        return response;
    }

    public static class CoapUrlStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new CoapUrlConnection(u);
        }
    }
    public static class CoapUrlConnection extends URLConnection{

        protected CoapUrlConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {

        }
    }


}
