package com.smartbear.coapsupport;

import com.eviware.soapui.impl.support.panels.AbstractHttpXmlRequestDesktopPanel;
import com.eviware.soapui.support.editor.views.xml.raw.RawXmlEditor;
import com.eviware.soapui.support.editor.xml.XmlDocument;
import org.eclipse.californium.core.coap.Response;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.settings.UISettings;
import com.eviware.soapui.support.editor.Editor;
import com.eviware.soapui.support.editor.views.AbstractXmlEditorView;
import com.eviware.soapui.support.editor.xml.XmlEditor;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

//@PluginResponseEditorView(viewId = "CoAP Message", targetClass = CoapRequest.class)
public class CoapResponseEditorView extends AbstractXmlEditorView<AbstractHttpXmlRequestDesktopPanel.HttpResponseDocument> {


    private JTextArea memo;
    private JComponent component;
    private JLabel responseCodeLabel;
    private JLabel messageTypeLabel;
    private OptionsTable optionsView;
    //private JTextArea payloadMemo;
    private RawXmlEditor<XmlDocument> innerRawView;


    public CoapResponseEditorView(Editor<?> editor, CoapRequest request, String viewId, RawXmlEditor<XmlDocument> innerRawView) {
        super("Response", (XmlEditor<AbstractHttpXmlRequestDesktopPanel.HttpResponseDocument>)editor, viewId);
        this.innerRawView = innerRawView;
    }

    @Override
    public boolean saveDocument(boolean validate) {
        return false;
    }

    @Override
    public void release() {
        innerRawView.release();
        super.release();
    }

    @Override
    public void setDocument(AbstractHttpXmlRequestDesktopPanel.HttpResponseDocument xmlDocument) {
        super.setDocument(xmlDocument);
        innerRawView.setDocument(xmlDocument);
    }

    @Override
    public JComponent getComponent() {
        if(component == null) {
            JPanel mainPanel = new JPanel(new GridBagLayout());
            JLabel label = new JLabel("Response Code:");
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            mainPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, getDefInsets(), 0, 0));
            responseCodeLabel = new JLabel();
            mainPanel.add(responseCodeLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, getDefInsets(), 0, 0));

            label = new JLabel("Message Type:");
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            mainPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, getDefInsets(), 0, 0));
            messageTypeLabel = new JLabel();
            mainPanel.add(messageTypeLabel, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, getDefInsets(), 0, 0));

            optionsView = new OptionsTable();
            optionsView.setEditable(false);
            JPanel pnl1 = new JPanel(new BorderLayout(0, 0));
            pnl1.add(new JScrollPane(optionsView), BorderLayout.CENTER); //workaround on lazy JScrollPane repainting
            Expander optionsExpander = new Expander("Response Options", pnl1, false, 200, 220);
            mainPanel.add(optionsExpander, new GridBagConstraints(0, 2, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, getDefInsets(), 0, 0));

            JPanel pnl2 = new JPanel(new BorderLayout(0, 0));
            pnl2.add(innerRawView.getComponent(), BorderLayout.CENTER); //workaround on lazy JScrollPane repainting
            Expander payloadExpander = new Expander("Response Payload", pnl2, true, 200, 400);
            mainPanel.add(payloadExpander, new GridBagConstraints(0, 3, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, getDefInsets(), 0, 0));

            JPanel dummyPanel = new JPanel();
            dummyPanel.setPreferredSize(new Dimension(0, 0));
            mainPanel.add(dummyPanel, new GridBagConstraints(1, 4, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

            component = new JScrollPane(mainPanel);
            updateView();

        }
        return component;
    }

    private Insets getDefInsets() {
        return new Insets(4, 4, 4, 4);
    }

    @Override
    public void setEditable(boolean enabled) {

    }

    @Override
    public void documentUpdated(){
        if(component != null){
            updateView();
            innerRawView.documentUpdated();
        }
    }

    private void updateView() {
        Response message = null;
        CoapResp resp = null;
        if(getDocument() != null){
            if(getDocument().getRequest() != null){
                if(getDocument().getRequest().getResponse() != null){
                    resp = (CoapResp)getDocument().getRequest().getResponse();
                    if(resp != null) message = resp.getResponseMessage();
                }

            }
//            message = getDocument().getResponseMessage();
//            resp = getDocument().getResponse();
        }
        if(message == null || resp == null) {
            responseCodeLabel.setText("");
            messageTypeLabel.setText("");
            optionsView.setData(null);
        }
        else{
            responseCodeLabel.setText(message.getCode().toString() + " " + Utils.responseCodeToText(resp.getStatusCode()));
            messageTypeLabel.setText(message.getType().toString());
            optionsView.setData(new ResponseOptionsDataSource(message));
        }
        //payloadMemo.setText(getRawPayloadAsString(resp));
    }

    public String getRawPayloadAsString(CoapResp resp) {
        if (resp == null || resp.getRawResponseData() == null
                || resp.getRawResponseData().length == 0) {
            return "<missing raw response data>";
        }

        byte[] rawResponseData = resp.getRawResponseData();
        int maxSize = (int) SoapUI.getSettings().getLong(UISettings.RAW_RESPONSE_MESSAGE_SIZE, 10000);

        if (maxSize < rawResponseData.length) {
            return new String(Arrays.copyOf(rawResponseData, maxSize));
        } else {
            return new String(rawResponseData);
        }
    }


}
